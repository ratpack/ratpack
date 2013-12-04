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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import ratpack.api.Nullable;
import ratpack.background.Background;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.event.internal.DefaultEventController;
import ratpack.event.internal.EventController;
import ratpack.file.internal.FileHttpTransmitter;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.handling.internal.DelegatingHeaders;
import ratpack.http.*;
import ratpack.http.internal.DefaultResponse;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.NoSuchRendererException;
import ratpack.server.BindAddress;
import ratpack.test.handling.Invocation;
import ratpack.test.handling.InvocationTimeoutException;
import ratpack.util.Action;

import java.io.File;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class DefaultInvocation implements Invocation {

  private Exception exception;
  private Headers headers;
  private ByteBuf body;
  private Status status;
  private boolean calledNext;
  private boolean sentResponse;
  private File sentFile;
  private Object rendered;
  private Integer clientError;

  public DefaultInvocation(final Request request, final Status status, final MutableHeaders responseHeaders, ByteBuf responseBody, Registry registry, final int timeout, Handler handler) {

    // There are definitely concurrency bugs in here around timing out
    // ideally we should prevent the stat from changing after a timeout occurs

    this.headers = new DelegatingHeaders(responseHeaders);
    this.status = status;

    ExecutorService mainExecutor = newSingleThreadExecutor();
    ListeningExecutorService backgroundExecutor = listeningDecorator(newSingleThreadExecutor());

    final CountDownLatch latch = new CountDownLatch(1);

    FileHttpTransmitter fileHttpTransmitter = new FileHttpTransmitter() {
      @Override
      public void transmit(Background background, BasicFileAttributes basicFileAttributes, File file) {
        sentFile = file;
        latch.countDown();
      }
    };

    final EventController<RequestOutcome> eventController = new DefaultEventController<>();

    Action<Response> committer = new Action<Response>() {
      public void execute(Response response) {
        sentResponse = true;
        eventController.fire(new DefaultRequestOutcome(request, response, System.currentTimeMillis()));
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

    Registry effectiveRegistry = RegistryBuilder.join(
      RegistryBuilder.builder().
        add(ClientErrorHandler.class, clientErrorHandler).
        add(ServerErrorHandler.class, serverErrorHandler).
        build(),
      registry
    );

    Response response = new DefaultResponse(status, responseHeaders, responseBody, fileHttpTransmitter, committer);
    Context context = new DefaultContext(request, response, bindAddress, effectiveRegistry, mainExecutor, backgroundExecutor, eventController.getRegistry(), new Handler[0], 0, next) {
      @Override
      public void render(Object object) throws NoSuchRendererException {
        rendered = object;
        latch.countDown();
      }
    };

    try {
      handler.handle(context);
    } catch (Exception e) {
      exception = e;
      latch.countDown();
    }

    try {
      if (!latch.await(timeout, TimeUnit.SECONDS)) {
        throw new InvocationTimeoutException(this, timeout);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e); // what to do here?
    }

    this.body = Unpooled.unmodifiableBuffer(responseBody);
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
      body.resetReaderIndex();
      return body.toString(CharsetUtil.UTF_8);
    } else {
      return null;
    }
  }

  @Override
  public byte[] getBodyBytes() {
    if (sentResponse) {
      body.resetReaderIndex();
      byte[] bytes = new byte[body.writerIndex()];
      body.readBytes(bytes, 0, bytes.length);
      return bytes;
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
  public File getSentFile() {
    return sentFile;
  }

  @Override
  public <T> T rendered(Class<T> type) {
    if (type.isAssignableFrom(rendered.getClass())) {
      return type.cast(rendered);
    } else {
      throw new AssertionError(String.format("Wrong type of object rendered. Was expecting %s but got %s", type, rendered.getClass()));
    }
  }
}
