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

package ratpack.handling.internal;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import ratpack.background.Background;
import ratpack.background.internal.DefaultBackground;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.file.FileSystemBinding;
import ratpack.handling.*;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.path.PathBinding;
import ratpack.path.PathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.NoSuchRendererException;
import ratpack.render.Renderer;
import ratpack.server.BindAddress;
import ratpack.util.Action;
import ratpack.util.Factory;
import ratpack.util.Result;
import ratpack.util.ResultAction;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class DefaultContext implements Context {

  private final static Logger LOGGER = Logger.getLogger(Context.class.getName());

  private final Request request;
  private final Response response;

  private final ExecutorService mainExecutorService;
  private final ListeningExecutorService backgroundExecutorService;
  private final Handler next;
  private final Registry registry;
  private final BindAddress bindAddress;

  public DefaultContext(Request request, Response response, BindAddress bindAddress, Registry registry, ExecutorService mainExecutorService, ListeningExecutorService backgroundExecutorService, Handler next) {
    this.request = request;
    this.response = response;
    this.bindAddress = bindAddress;
    this.registry = registry;
    this.mainExecutorService = mainExecutorService;
    this.backgroundExecutorService = backgroundExecutorService;
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

  public <O> List<O> getAll(Class<O> type) {
    return registry.getAll(type);
  }

  public <O> O maybeGet(Class<O> type) {
    return registry.maybeGet(type);
  }

  public void next() {
    try {
      next.handle(this);
    } catch (Exception e) {
      dispatchException(e);
    }
  }

  public void insert(List<Handler> handlers) {
    doNext(this, registry, handlers, 0, next);
  }

  public void insert(List<Handler> handlers, Registry registry) {
    doNext(this, RegistryBuilder.join(this.registry, registry), handlers, 0, next);
  }

  @Override
  public <T> void insert(List<Handler> handlers, Class<T> publicType, Factory<? extends T> factory) {
    insert(handlers, RegistryBuilder.builder().add(publicType, factory).build());
  }

  public <P, T extends P> void insert(List<Handler> handlers, Class<P> publicType, T implementation) {
    insert(handlers, RegistryBuilder.builder().add(publicType, implementation).build());
  }

  public void insert(List<Handler> handlers, Object object) {
    insert(handlers, RegistryBuilder.builder().add(object).build());
  }

  public void respond(Handler handler) {
    try {
      handler.handle(this);
    } catch (Exception e) {
      dispatchException(e);
    }
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
    @SuppressWarnings("rawtypes")
    List<Renderer> all = registry.getAll(Renderer.class);
    for (Renderer<?> renderer : all) {
      if (maybeRender(object, renderer)) {
        return;
      }
    }

    throw new NoSuchRendererException("No renderer for object '" + object + "'");
  }

  private <T> boolean maybeRender(Object object, Renderer<T> renderer) {
    if (renderer.getType().isInstance(object)) {
      @SuppressWarnings("unchecked") T cast = (T) object;
      renderer.render(this, cast);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public <T> T parse(Parse<T> parse) {
    @SuppressWarnings("rawtypes")
    List<Parser> all = registry.getAll(Parser.class);
    String requestContentType = request.getContentType().getType();
    if (requestContentType == null) {
      requestContentType = "text/plain";
    }
    T parsed;
    for (Parser<?, ?> parser : all) {
      parsed = maybeParse(requestContentType, parse, parser);
      if (parsed != null) {
        return parsed;
      }
    }

    throw new RuntimeException("No parser for " + parse);
  }

  private <P, S extends Parse<P>> P maybeParse(String requestContentType, S parseSpec, Parser<?, ?> parser) {
    if (requestContentType.equalsIgnoreCase(parser.getContentType()) && parser.getParseType().isInstance(parseSpec)) {
      @SuppressWarnings("unchecked") Parser<P, S> castParser = (Parser<P, S>) parser;
      return castParser.parse(this, parseSpec);
    } else {
      return null;
    }
  }

  @Override
  public Background getBackground() {
    return new DefaultBackground(mainExecutorService, backgroundExecutorService, this);
  }

  @Override
  public <T> Background.SuccessOrError<T> background(Callable<T> backgroundOperation) {
    return getBackground().exec(backgroundOperation);
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

  @Override
  public BindAddress getBindAddress() {
    return bindAddress;
  }

  public void error(Exception exception) {
    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.class);
    try {
      serverErrorHandler.error(this, exception);
    } catch (Exception errorHandlerException) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      stringWriter.
        append("Exception thrown by error handler ").
        append(serverErrorHandler.toString()).
        append(" while handling exception\nOriginal exception: ");
      exception.printStackTrace(printWriter);
      stringWriter.
        append("Error handler exception: ");
      errorHandlerException.printStackTrace(printWriter);
      LOGGER.warning(stringWriter.toString());
      response.status(500).send();
    }
  }

  public void clientError(int statusCode) {
    get(ClientErrorHandler.class).error(this, statusCode);
  }

  public void withErrorHandling(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      dispatchException(e);
    }
  }

  private void dispatchException(Exception e) {
    if (e instanceof HandlerException) {
      ((HandlerException) e).getContext().error((Exception) e.getCause());
    } else {
      error(e);
    }
  }

  @Override
  public <T> ResultAction<T> resultAction(final Action<T> action) {
    return new ResultAction<T>() {
      @Override
      public void execute(Result<T> result) {
        if (result.isFailure()) {
          dispatchException(result.getFailure());
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

  protected void doNext(final Context parentContext, final Registry registry, final List<Handler> handlers, final int index, final Handler exhausted) {
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
      DefaultContext childExchange = new DefaultContext(request, response, bindAddress, registry, mainExecutorService, backgroundExecutorService, nextHandler);
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
