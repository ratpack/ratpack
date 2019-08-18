/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.test.handling.internal;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.CharsetUtil;
import org.reactivestreams.Subscriber;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.exec.ExecController;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.ChainHandler;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.http.internal.*;
import ratpack.registry.Registry;
import ratpack.render.internal.RenderController;
import ratpack.server.Stopper;
import ratpack.test.handling.HandlerExceptionNotThrownException;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.UnexpectedHandlerException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ratpack.util.Exceptions.uncheck;

public class DefaultHandlingResult implements HandlingResult {

  private DefaultContext.RequestConstants requestConstants;
  private Headers headers;
  private Set<Cookie> cookies;
  private byte[] body = new byte[0];
  private Status status;
  private boolean calledNext;
  private boolean sentResponse;
  private Path sentFile;
  private Object rendered;
  private ResultsHolder results;

  public DefaultHandlingResult(final DefaultRequest request, final ResultsHolder results, final MutableHeaders responseHeaders, Registry registry, final int timeout, final Handler handler) throws Exception {

    // There are definitely concurrency bugs in here around timing out
    // ideally we should prevent the stat from changing after a timeout occurs

    this.headers = new DelegatingHeaders(responseHeaders);

    this.results = results;
    final CountDownLatch latch = results.getLatch();

    final Handler next = context -> {
      calledNext = true;
      results.getLatch().countDown();
    };

    final RenderController renderController = (object, context) -> {
      rendered = object;
      latch.countDown();
    };

    Stopper stopper = () -> {
      throw new UnsupportedOperationException("stopping not supported while unit testing");
    };

    ResponseTransmitter responseTransmitter = new ResponseTransmitter() {

      private List<Action<? super RequestOutcome>> outcomeListeners = Lists.newArrayList();

      @Override
      public void transmit(HttpResponseStatus status, ByteBuf byteBuf) {
        sentResponse = true;
        body = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(body);
        byteBuf.release();
        fire(new DefaultRequestOutcome(request, new DefaultSentResponse(headers, new DefaultStatus(status)), Instant.now()));
        latch.countDown();
      }

      @Override
      public void transmit(HttpResponseStatus status, Path file) {
        sentFile = file;
        fire(new DefaultRequestOutcome(request, new DefaultSentResponse(headers, DefaultHandlingResult.this.status), Instant.now()));
        latch.countDown();
      }

      @Override
      public Subscriber<ByteBuf> transmitter(HttpResponseStatus status) {
        throw new UnsupportedOperationException("streaming not supported while unit testing");
      }

      private void fire(RequestOutcome requestOutcome) {
        for (Action<? super RequestOutcome> outcomeListener : outcomeListeners) {
          try {
            outcomeListener.execute(requestOutcome);
          } catch (Exception e) {
            LoggerFactory.getLogger(DefaultHandlingResult.class).warn("request outcome listener " + outcomeListener + " threw exception", e);
          }
        }
      }

      @Override
      public void addOutcomeListener(Action<? super RequestOutcome> action) {
        outcomeListeners.add(action);
      }

      @Override
      public void forceCloseConnection() {

      }
    };

    ExecController execController = registry.get(ExecController.class);
    Registry effectiveRegistry = Registry.single(Stopper.class, stopper).join(registry);
    DefaultContext.ApplicationConstants applicationConstants = new DefaultContext.ApplicationConstants(effectiveRegistry, renderController, execController, next);
    requestConstants = new DefaultContext.RequestConstants(
      applicationConstants, request, null, responseTransmitter, null);
    Response response = new DefaultResponse(responseHeaders, registry.get(ByteBufAllocator.class), responseTransmitter);
    requestConstants.response = response;
    DefaultContext.start(execController.getEventLoopGroup().next(), requestConstants, effectiveRegistry, ChainHandler.unpack(handler), Action.noop());

    try {
      if (!latch.await(timeout, TimeUnit.SECONDS)) {
        throw new HandlerTimeoutException(this, timeout);
      }
    } catch (InterruptedException e) {
      throw uncheck(e); // what to do here?
    } finally {
      status = response.getStatus();
      cookies = Collections.unmodifiableSet(response.getCookies());
    }
  }

  @Override
  public byte[] getBodyBytes() {
    Throwable throwable = results.getThrowable();
    if (throwable != null) {
      throw new UnexpectedHandlerException(throwable);
    }
    if (sentResponse) {
      return body;
    } else {
      return null;
    }
  }

  @Override
  public String getBodyText() {
    Throwable throwable = results.getThrowable();
    if (throwable != null) {
      throw new UnexpectedHandlerException(throwable);
    }
    if (sentResponse) {
      return new String(body, CharsetUtil.UTF_8);
    } else {
      return null;
    }
  }

  @Override
  public Set<Cookie> getCookies() {
    return cookies;
  }

  @Nullable
  @Override
  public Integer getClientError() {
    return results.getClientError();
  }

  private Context getContext() {
    return requestConstants.context;
  }

  @Override
  public <T extends Throwable> T exception(Class<T> clazz) {
    Throwable throwable = results.getThrowable();
    if (throwable == null) {
      throw new HandlerExceptionNotThrownException();
    } else {
      if (clazz.isAssignableFrom(throwable.getClass())) {
        return clazz.cast(throwable);
      } else {
        throw new UnexpectedHandlerException(throwable);
      }
    }
  }

  @Override
  public Headers getHeaders() {
    return headers;
  }

  @Override
  public Registry getRegistry() {
    return getContext();
  }

  @Override
  public Registry getRequestRegistry() {
    return getContext().getRequest();
  }

  @Override
  public Path getSentFile() {
    return sentFile;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  @Override
  public boolean isCalledNext() {
    Throwable throwable = results.getThrowable();
    if (throwable != null) {
      throw new UnexpectedHandlerException(throwable);
    }
    return calledNext;
  }

  @Override
  public boolean isSentResponse() {
    return sentResponse;
  }

  @Override
  public <T> T rendered(Class<T> type) {
    Throwable throwable = results.getThrowable();
    if (throwable != null) {
      throw new UnexpectedHandlerException(throwable);
    }
    if (rendered == null) {
      return null;
    }

    if (type.isAssignableFrom(rendered.getClass())) {
      return type.cast(rendered);
    } else {
      throw new AssertionError(String.format("Wrong type of object rendered. Was expecting %s but got %s", type, rendered.getClass()));
    }
  }

  public static class ResultsHolder {
    private Integer clientError;
    private Throwable throwable;
    private final CountDownLatch latch = new CountDownLatch(1);

    public Integer getClientError() {
      return clientError;
    }

    public void setClientError(Integer clientError) {
      this.clientError = clientError;
    }

    public Throwable getThrowable() {
      return throwable;
    }

    public void setThrowable(Throwable throwable) {
      this.throwable = throwable;
    }

    public CountDownLatch getLatch() {
      return latch;
    }
  }
}
