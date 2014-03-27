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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;
import ratpack.api.Nullable;
import ratpack.background.internal.DefaultBackground;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.event.internal.DefaultEventController;
import ratpack.event.internal.EventController;
import ratpack.file.internal.FileHttpTransmitter;
import ratpack.func.Action;
import ratpack.handling.*;
import ratpack.handling.internal.*;
import ratpack.http.*;
import ratpack.http.internal.DefaultResponse;
import ratpack.http.internal.DefaultSentResponse;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.render.internal.RenderController;
import ratpack.server.BindAddress;
import ratpack.test.handling.Invocation;
import ratpack.test.handling.InvocationTimeoutException;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultInvocation implements Invocation {

  private Exception exception;
  private Headers headers;
  private byte[] body = new byte[0];
  private Status status;
  private boolean calledNext;
  private boolean sentResponse;
  private Path sentFile;
  private Object rendered;
  private Integer clientError;

  private final ContextStorage contextStorage = new ContextStorage() {
    final Stack<Context> contextStack = new Stack<>();

    @Override
    public Context get() {
      return contextStack.peek();
    }

    @Override
    public void set(Context context) {
      contextStack.push(context);
    }

    @Override
    public void remove() {
      // no op
    }
  };

  public DefaultInvocation(final Request request, final MutableStatus status, final MutableHeaders responseHeaders, Registry registry, final int timeout, Handler handler) {

    // There are definitely concurrency bugs in here around timing out
    // ideally we should prevent the stat from changing after a timeout occurs

    this.headers = new DelegatingHeaders(responseHeaders);
    this.status = status;

    ListeningExecutorService backgroundExecutorService = listeningDecorator(newSingleThreadExecutor());
    ListeningScheduledExecutorService foregroundExecutorService = listeningDecorator(Executors.newScheduledThreadPool(1));

    final CountDownLatch latch = new CountDownLatch(1);

    FileHttpTransmitter fileHttpTransmitter = new FileHttpTransmitter() {
      @Override
      public void transmit(Background background, BasicFileAttributes basicFileAttributes, Path file) {
        sentFile = file;
        latch.countDown();
      }
    };

    final EventController<RequestOutcome> eventController = new DefaultEventController<>();

    Action<ByteBuf> committer = new Action<ByteBuf>() {
      public void execute(ByteBuf byteBuf) {
        sentResponse = true;
        body = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(body);
        byteBuf.release();
        eventController.fire(new DefaultRequestOutcome(request, new DefaultSentResponse(headers, status), System.currentTimeMillis()));
        latch.countDown();
      }
    };

    Handler next = new Handler() {
      public void handle(Context context) {
        calledNext = true;
        latch.countDown();
      }
    };

    BindAddress bindAddress = new BindAddress() {
      @Override
      public int getPort() {
        return 5050;
      }

      @Override
      public String getHost() {
        return "localhost";
      }
    };

    ClientErrorHandler clientErrorHandler = new ClientErrorHandler() {
      @Override
      public void error(Context context, int statusCode) throws Exception {
        DefaultInvocation.this.clientError = statusCode;
        latch.countDown();
      }
    };

    ServerErrorHandler serverErrorHandler = new ServerErrorHandler() {
      @Override
      public void error(Context context, Exception exception) throws Exception {
        DefaultInvocation.this.exception = exception;
        latch.countDown();
      }
    };

    FinishedOnThreadCallbackManager finishedOnThreadCallbackManager = new FinishedOnThreadCallbackManager();

    Registry effectiveRegistry = Registries.join(
      Registries.registry().
        add(ClientErrorHandler.class, clientErrorHandler).
        add(ServerErrorHandler.class, serverErrorHandler).
        add(finishedOnThreadCallbackManager).
        build(),
      registry
    );

    RenderController renderController = new RenderController() {
      @Override
      public void render(Object object, Context context) {
        rendered = object;
        latch.countDown();
      }
    };


    Response response = new DefaultResponse(status, responseHeaders, fileHttpTransmitter, UnpooledByteBufAllocator.DEFAULT, committer);

    Foreground foreground = new DefaultForeground(contextStorage, foregroundExecutorService);

    Background background = new DefaultBackground(foregroundExecutorService, backgroundExecutorService, contextStorage);
    DefaultContext.ApplicationConstants applicationConstants = new DefaultContext.ApplicationConstants(foreground, background, contextStorage, renderController);
    DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(
      applicationConstants, bindAddress, request, response, null, eventController.getRegistry()
    );

    Context context = new DefaultContext(requestConstants, effectiveRegistry, new Handler[0], 0, next);

    contextStorage.set(context);
    try {
      handler.handle(context);
      finishedOnThreadCallbackManager.fire();
    } catch (Exception e) {
      exception = e;
      latch.countDown();
    }

    try {
      if (!latch.await(timeout, TimeUnit.SECONDS)) {
        throw new InvocationTimeoutException(this, timeout);
      }
    } catch (InterruptedException e) {
      throw uncheck(e); // what to do here?
    }
  }

  @Override
  public Registry getRegistry() {
    return contextStorage.get();
  }

  @Override
  public Registry getRequestRegistry() {
    return contextStorage.get().getRequest();
  }

  @Override
  public Exception getException() {
    return exception;
  }

  @Nullable
  @Override
  public Integer getClientError() {
    return clientError;
  }

  @Override
  public Headers getHeaders() {
    return headers;
  }

  @Override
  public String getBodyText() {
    if (sentResponse) {
      return new String(body, CharsetUtil.UTF_8);
    } else {
      return null;
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
  public Path getSentFile() {
    return sentFile;
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
