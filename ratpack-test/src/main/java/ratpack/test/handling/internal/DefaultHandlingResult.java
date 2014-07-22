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
import io.netty.util.CharsetUtil;
import ratpack.api.Nullable;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.event.internal.DefaultEventController;
import ratpack.event.internal.EventController;
import ratpack.exec.ExecControl;
import ratpack.exec.Execution;
import ratpack.file.internal.FileHttpTransmitter;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.handling.internal.DelegatingHeaders;
import ratpack.http.*;
import ratpack.http.internal.*;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.render.internal.RenderController;
import ratpack.server.BindAddress;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultHandlingResult implements HandlingResult {

  private DefaultContext.RequestConstants requestConstants;
  private Exception exception;
  private Headers headers;
  private byte[] body = new byte[0];
  private Status status;
  private boolean calledNext;
  private boolean sentResponse;
  private Path sentFile;
  private Object rendered;
  private Integer clientError;

  public DefaultHandlingResult(final Request request, final MutableStatus status, final MutableHeaders responseHeaders, Registry registry, final int timeout, LaunchConfigBuilder launchConfigBuilder, final Handler handler) {

    // There are definitely concurrency bugs in here around timing out
    // ideally we should prevent the stat from changing after a timeout occurs

    this.headers = new DelegatingHeaders(responseHeaders);
    this.status = status;

    final CountDownLatch latch = new CountDownLatch(1);

    final DefaultChunkedResponseTransmitter chunkedResponseTransmitter = new DefaultChunkedResponseTransmitter(null, null, null); //TODO: what test support is required here?
    final ServerSentEventTransmitter serverSentEventTransmitter = new DefaultServerSentEventTransmitter(null, null, null); //TODO: what test support is required here?

    final EventController<RequestOutcome> eventController = new DefaultEventController<>();

    final FileHttpTransmitter fileHttpTransmitter = new FileHttpTransmitter() {
      @Override
      public void transmit(ExecControl execContext, BasicFileAttributes basicFileAttributes, Path file) {
        sentFile = file;
        eventController.fire(new DefaultRequestOutcome(request, new DefaultSentResponse(headers, status), System.currentTimeMillis()));
        latch.countDown();
      }
    };

    final Action<ByteBuf> committer = new Action<ByteBuf>() {
      public void execute(ByteBuf byteBuf) throws Exception {
        sentResponse = true;
        body = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(body);
        byteBuf.release();
        eventController.fire(new DefaultRequestOutcome(request, new DefaultSentResponse(headers, status), System.currentTimeMillis()));
        latch.countDown();
      }
    };

    final Handler next = new Handler() {
      public void handle(Context context) {
        calledNext = true;
        latch.countDown();
      }
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

    ClientErrorHandler clientErrorHandler = new ClientErrorHandler() {
      @Override
      public void error(Context context, int statusCode) throws Exception {
        DefaultHandlingResult.this.clientError = statusCode;
        latch.countDown();
      }
    };

    ServerErrorHandler serverErrorHandler = new ServerErrorHandler() {
      @Override
      public void error(Context context, Exception exception) throws Exception {
        DefaultHandlingResult.this.exception = exception;
        latch.countDown();
      }
    };


    final Registry effectiveRegistry = Registries.join(
      Registries.registry().
        add(ClientErrorHandler.class, clientErrorHandler).
        add(ServerErrorHandler.class, serverErrorHandler).
        build(),
      registry
    );

    final RenderController renderController = new RenderController() {
      @Override
      public void render(Object object, Context context) {
        rendered = object;
        latch.countDown();
      }
    };

    final LaunchConfig launchConfig = launchConfigBuilder.build();

    launchConfig.getExecController().start(new Action<Execution>() {
      @Override
      public void execute(Execution execution) throws Exception {
        Response response = new DefaultResponse(status, responseHeaders, fileHttpTransmitter, chunkedResponseTransmitter, serverSentEventTransmitter, launchConfig.getBufferAllocator(), committer);
        DefaultContext.ApplicationConstants applicationConstants = new DefaultContext.ApplicationConstants(launchConfig, renderController);
        requestConstants = new DefaultContext.RequestConstants(
          applicationConstants, bindAddress, request, response, null, eventController.getRegistry(), execution
        );

        Context context = new DefaultContext(requestConstants, effectiveRegistry, new Handler[]{handler}, 0, next);
        context.next();
      }
    });


    try {
      if (!latch.await(timeout, TimeUnit.SECONDS)) {
        throw new HandlerTimeoutException(this, timeout);
      }
    } catch (InterruptedException e) {
      throw uncheck(e); // what to do here?
    } finally {
      launchConfig.getExecController().close();
    }
  }

  @Override
  public byte[] getBodyBytes() {
    if (sentResponse) {
      return body;
    } else {
      return null;
    }
  }

  @Override
  public String getBodyText() {
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
  public Exception getException() {
    return exception;
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
    return calledNext;
  }

  @Override
  public boolean isSentResponse() {
    return sentResponse;
  }

  @Override
  public <T> T rendered(Class<T> type) {
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
