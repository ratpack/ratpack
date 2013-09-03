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

package org.ratpackframework.handling.internal;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.block.Blocking;
import org.ratpackframework.block.internal.DefaultBlocking;
import org.ratpackframework.error.ClientErrorHandler;
import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.*;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.path.PathTokens;
import org.ratpackframework.handling.Redirector;
import org.ratpackframework.registry.NotInRegistryException;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.ObjectHoldingChildRegistry;
import org.ratpackframework.render.controller.NoSuchRendererException;
import org.ratpackframework.render.controller.RenderController;
import org.ratpackframework.util.Action;
import org.ratpackframework.util.Result;
import org.ratpackframework.util.ResultAction;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class DefaultContext implements Context {

  private final Request request;
  private final Response response;

  private final ExecutorService mainExecutorService;
  private final ListeningExecutorService blockingExecutorService;
  private final Handler next;
  private final Registry<Object> registry;

  public DefaultContext(Request request, Response response, Registry<Object> registry, ExecutorService mainExecutorService, ListeningExecutorService blockingExecutorService, Handler next) {
    this.request = request;
    this.response = response;
    this.registry = registry;
    this.mainExecutorService = mainExecutorService;
    this.blockingExecutorService = blockingExecutorService;
    this.next = next;
  }

  public Request getRequest() {
    return request;
  }

  public Response getResponse() {
    return response;
  }

  public <O> O get(Class<O> type) throws NotInRegistryException {
    return registry.get(type);
  }

  public <O> O maybeGet(Class<O> type) {
    return registry.maybeGet(type);
  }

  public void next() {
    next.handle(this);
  }

  public void insert(List<Handler> handlers) {
    doNext(this, registry, handlers, 0, next);
  }

  public void insert(Registry<Object> registry, List<Handler> handlers) {
    doNext(this, registry, handlers, 0, next);
  }

  public <P, T extends P> void insert(Class<P> publicType, T implementation, List<Handler> handlers) {
    doNext(this, new ObjectHoldingChildRegistry<>(registry, publicType, implementation), handlers, 0, next);
  }

  public void insert(Object object, List<Handler> handlers) {
    doNext(this, new ObjectHoldingChildRegistry<>(registry, object), handlers, 0, next);
  }

  public void respond(Handler handler) {
    handler.handle(this);
  }

  public PathTokens getPathTokens() {
    return get(PathBinding.class).getTokens();
  }

  public PathTokens getAllPathTokens() {
    return get(PathBinding.class).getAllTokens();
  }

  public File file(String path) {
    return get(FileSystemBinding.class).file(path);
  }

  public void render(Object object) throws NoSuchRendererException {
    get(RenderController.class).render(this, object);
  }

  @Override
  public Blocking getBlocking() {
    return new DefaultBlocking(mainExecutorService, blockingExecutorService, this);
  }

  public void redirect(String location) {
    redirect(HttpResponseStatus.FOUND.code(), location);
  }

  public void redirect(int code, String location) {
    Redirector redirector = registry.get(Redirector.class);
    redirector.redirect(this, location, code);
  }

  @Override
  public void lastModified(Date date, Runnable runnable) {
    Date ifModifiedSinceHeader = request.getHeaders().getDate(IF_MODIFIED_SINCE);
    long lastModifiedSecs = date.getTime() / 1000;

    if (ifModifiedSinceHeader != null) {
      long time = ifModifiedSinceHeader.getTime();
      long ifModifiedSinceSecs = time / 1000;

      if (lastModifiedSecs == ifModifiedSinceSecs) {
        response.status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
        return;
      }
    }

    response.getHeaders().setDate(HttpHeaders.Names.LAST_MODIFIED, date);
    runnable.run();
  }

  public void error(Exception exception) {
    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.class);
    serverErrorHandler.error(this, exception);
  }

  public void clientError(int statusCode) {
    get(ClientErrorHandler.class).error(this, statusCode);
  }

  public void withErrorHandling(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      if (e instanceof HandlerException) {
        ((HandlerException) e).getContext().error((Exception) e.getCause());
      } else {
        error(e);
      }
    }
  }

  @Override
  public <T> ResultAction<T> resultAction(final Action<T> action) {
    return new ResultAction<T>() {
      @Override
      public void execute(Result<T> result) {
        if (result.isFailure()) {
          error(result.getFailure());
        } else {
          action.execute(result.getValue());
        }
      }
    };
  }

  public ByMethodHandler getByMethod() {
    return new DefaultByMethodHandler();
  }

  public ByContentHandler getByContent() {
    return new DefaultByContentHandler();
  }

  protected void doNext(final Context parentContext, final Registry<Object> registry, final List<Handler> handlers, final int index, final Handler exhausted) {
    assert registry != null;
    if (index == handlers.size()) {
      try {
        exhausted.handle(parentContext);
      } catch (Exception e) {
        if (e instanceof HandlerException) {
          throw (HandlerException) e;
        } else {
          throw new HandlerException(this, e);
        }
      }

    } else {
      Handler handler = handlers.get(index);
      Handler nextHandler = new Handler() {
        public void handle(Context exchange) {
          ((DefaultContext) exchange).doNext(parentContext, registry, handlers, index + 1, exhausted);
        }
      };
      DefaultContext childExchange = new DefaultContext(request, response, registry, mainExecutorService, blockingExecutorService, nextHandler);
      try {
        handler.handle(childExchange);
      } catch (Exception e) {
        if (e instanceof HandlerException) {
          throw (HandlerException) e;
        } else {
          throw new HandlerException(childExchange, e);
        }
      }
    }
  }

}
