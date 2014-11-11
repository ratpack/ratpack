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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.reactivestreams.Subscriber;
import ratpack.api.Nullable;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.event.internal.DefaultEventController;
import ratpack.event.internal.EventController;
import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.ChainHandler;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.handling.internal.DelegatingHeaders;
import ratpack.http.*;
import ratpack.http.internal.DefaultResponse;
import ratpack.http.internal.DefaultSentResponse;
import ratpack.http.internal.DefaultStatus;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.render.internal.RenderController;
import ratpack.server.BindAddress;
import ratpack.server.Stopper;
import ratpack.server.internal.NettyHandlerAdapter;
import ratpack.test.handling.HandlerExceptionNotThrownException;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.UnexpectedHandlerException;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultHandlingResult implements HandlingResult {

  private DefaultContext.RequestConstants requestConstants;
  private Throwable throwable;
  private Headers headers;
  private byte[] body = new byte[0];
  private Status status;
  private boolean calledNext;
  private boolean sentResponse;
  private Path sentFile;
  private Object rendered;
  private Integer clientError;

  public DefaultHandlingResult(final Request request, final MutableHeaders responseHeaders, Registry registry, final int timeout, LaunchConfig launchConfig, final Handler handler) {

    // There are definitely concurrency bugs in here around timing out
    // ideally we should prevent the stat from changing after a timeout occurs

    this.headers = new DelegatingHeaders(responseHeaders);

    final CountDownLatch latch = new CountDownLatch(1);

    final EventController<RequestOutcome> eventController = new DefaultEventController<>();

    final Handler next = context -> {
      calledNext = true;
      latch.countDown();
    };

    final BindAddress bindAddress = new BindAddress() {
      @Override
      public String getHost() {
        return "localhost";
      }

      @Override
      public int getPort() {
        return 5050;
      }
    };

    ClientErrorHandler clientErrorHandler = (context, statusCode) -> {
      DefaultHandlingResult.this.clientError = statusCode;
      latch.countDown();
    };

    ServerErrorHandler serverErrorHandler = (context, throwable1) -> {
      DefaultHandlingResult.this.throwable = throwable1;
      latch.countDown();
    };

    final Registry userRegistry = Registries.registry().
      add(ClientErrorHandler.class, clientErrorHandler).
      add(ServerErrorHandler.class, serverErrorHandler).
      build()
      .join(registry);

    final RenderController renderController = (object, context) -> {
      rendered = object;
      latch.countDown();
    };

    Stopper stopper = () -> {
      throw new UnsupportedOperationException("stopping not supported while unit testing");
    };

    ResponseTransmitter responseTransmitter = new ResponseTransmitter() {
      @Override
      public void transmit(HttpResponseStatus status, ByteBuf byteBuf) {
        sentResponse = true;
        body = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(body);
        byteBuf.release();
        eventController.fire(new DefaultRequestOutcome(request, new DefaultSentResponse(headers, new DefaultStatus(status)), System.currentTimeMillis()));
        latch.countDown();
      }

      @Override
      public void transmit(HttpResponseStatus responseStatus, BasicFileAttributes basicFileAttributes, Path file) {
        sentFile = file;
        eventController.fire(new DefaultRequestOutcome(request, new DefaultSentResponse(headers, status), System.currentTimeMillis()));
        latch.countDown();
      }

      @Override
      public Subscriber<ByteBuf> transmitter(HttpResponseStatus status) {
        throw new UnsupportedOperationException("streaming not supported while unit testing");
      }
    };

    ExecController execController = launchConfig.getExecController();
    ExecControl execControl = execController.getControl();
    Registry baseRegistry = NettyHandlerAdapter.buildBaseRegistry(stopper, launchConfig);
    Registry effectiveRegistry = baseRegistry.join(userRegistry);
    Response response = new DefaultResponse(execControl, responseHeaders, launchConfig.getBufferAllocator(), responseTransmitter);
    DefaultContext.ApplicationConstants applicationConstants = new DefaultContext.ApplicationConstants(launchConfig, renderController, next);
    requestConstants = new DefaultContext.RequestConstants(
      applicationConstants, bindAddress, request, response, null, eventController.getRegistry()
    );

    DefaultContext.start(execController.getEventLoopGroup().next(), execControl, requestConstants, effectiveRegistry, ChainHandler.unpack(handler), Action.noop());

    try {
      if (!latch.await(timeout, TimeUnit.SECONDS)) {
        throw new HandlerTimeoutException(this, timeout);
      }
    } catch (InterruptedException e) {
      throw uncheck(e); // what to do here?
    } finally {
      status = response.getStatus();
    }
  }

  @Override
  public byte[] getBodyBytes() {
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
    if (throwable != null) {
      throw new UnexpectedHandlerException(throwable);
    }
    if (sentResponse) {
      return new String(body, CharsetUtil.UTF_8);
    } else {
      return null;
    }
  }

  @Nullable
  @Override
  public Integer getClientError() {
    return clientError;
  }

  private Context getContext() {
    return requestConstants.context;
  }

  @Override
  public <T extends Throwable> T exception(Class<T> clazz) {
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
}
