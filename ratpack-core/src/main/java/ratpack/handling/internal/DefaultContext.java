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
import ratpack.event.internal.EventRegistry;
import ratpack.file.FileSystemBinding;
import ratpack.handling.*;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.parse.*;
import ratpack.path.PathBinding;
import ratpack.path.PathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.NoSuchRendererException;
import ratpack.render.Renderer;
import ratpack.server.BindAddress;
import ratpack.util.*;

import javax.inject.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class DefaultContext implements Context {

  public static class ApplicationConstants {
    private final ListeningExecutorService backgroundExecutorService;
    private final Provider<Context> contextProvider;
    private final ThreadLocal<Context> contextThreadLocal;

    public ApplicationConstants(ListeningExecutorService backgroundExecutorService, Provider<Context> contextProvider, ThreadLocal<Context> contextThreadLocal) {
      this.backgroundExecutorService = backgroundExecutorService;
      this.contextProvider = contextProvider;
      this.contextThreadLocal = contextThreadLocal;
    }
  }

  public static class RequestConstants {
    private final ApplicationConstants applicationConstants;

    private final BindAddress bindAddress;
    private final Request request;
    private final Response response;

    private final DirectChannelAccess directChannelAccess;
    private final EventRegistry<RequestOutcome> onCloseRegistry;

    private final ScheduledExecutorService foregroundExecutorService;

    public RequestConstants(
      ApplicationConstants applicationConstants, BindAddress bindAddress, Request request, Response response,
      DirectChannelAccess directChannelAccess, EventRegistry<RequestOutcome> onCloseRegistry,
      ScheduledExecutorService foregroundExecutorService
    ) {
      this.applicationConstants = applicationConstants;
      this.bindAddress = bindAddress;
      this.request = request;
      this.response = response;
      this.directChannelAccess = directChannelAccess;
      this.onCloseRegistry = onCloseRegistry;
      this.foregroundExecutorService = foregroundExecutorService;
    }
  }

  private final static Logger LOGGER = Logger.getLogger(Context.class.getName());

  private final RequestConstants requestConstants;

  private final Registry registry;

  private final Handler[] nextHandlers;
  private final int nextIndex;
  private final Handler exhausted;

  public DefaultContext(RequestConstants requestConstants, Registry registry, Handler[] nextHandlers, int nextIndex, Handler exhausted) {
    this.requestConstants = requestConstants;
    this.registry = registry;
    this.nextHandlers = nextHandlers;
    this.nextIndex = nextIndex;
    this.exhausted = exhausted;
  }

  @Override
  public Context getContext() {
    return this;
  }

  @Override
  public Provider<Context> getProvider() {
    return requestConstants.applicationConstants.contextProvider;
  }

  public Request getRequest() {
    return requestConstants.request;
  }

  public Response getResponse() {
    return requestConstants.response;
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

  @Override
  public boolean isEmpty() {
    return registry.isEmpty();
  }

  public void next() {
    doNext(this, registry, nextIndex, nextHandlers, exhausted);
  }

  @Override
  public void next(Registry registry) {
    doNext(this, RegistryBuilder.join(this.registry, registry), nextIndex, nextHandlers, exhausted);
  }

  public <T> void next(Class<T> publicType, Factory<? extends T> factory) {
    next(RegistryBuilder.builder().add(publicType, factory).build());
  }

  public void next(Object object) {
    next(RegistryBuilder.builder().add(object).build());
  }

  public <P, T extends P> void next(Class<P> publicType, T impl) {
    next(RegistryBuilder.builder().add(publicType, impl).build());
  }

  public void insert(Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    doNext(this, registry, 0, handlers, new RejoinHandler());
  }

  public void insert(Registry registry, Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    doNext(this, RegistryBuilder.join(this.registry, registry), 0, handlers, new RejoinHandler());
  }

  public <T> void insert(Class<T> publicType, Factory<? extends T> factory, Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    insert(RegistryBuilder.builder().add(publicType, factory).build(), handlers);
  }

  public <P, T extends P> void insert(Class<P> publicType, T implementation, Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }
    insert(RegistryBuilder.builder().add(publicType, implementation).build(), handlers);
  }

  public void insert(Object object, Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }
    insert(RegistryBuilder.builder().add(object).build(), handlers);
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

  public Path file(String path) {
    return get(FileSystemBinding.class).file(path);
  }

  public void render(Object object) throws NoSuchRendererException {
    if (object == null) {
      clientError(404);
      return;
    }

    @SuppressWarnings("rawtypes")
    List<Renderer> all = registry.getAll(Renderer.class);
    for (Renderer<?> renderer : all) {
      if (maybeRender(object, renderer)) {
        return;
      }
    }

    throw new NoSuchRendererException("No renderer for object '" + object + "' of type '" + object.getClass() + "'");
  }

  private <T> boolean maybeRender(Object object, Renderer<T> renderer) {
    if (renderer.getType().isInstance(object)) {
      @SuppressWarnings("unchecked") T cast = (T) object;
      try {
        renderer.render(this, cast);
      } catch (Exception e) {
        error(e);
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public <T> T parse(Parse<T> parse) throws ParserException, NoSuchParserException {
    @SuppressWarnings("rawtypes")
    List<Parser> all = registry.getAll(Parser.class);
    String requestContentType = requestConstants.request.getBody().getContentType().getType();
    if (requestContentType == null) {
      requestContentType = "text/plain";
    }
    for (Parser<?, ?> parser : all) {
      T parsed = maybeParse(requestContentType, parse, parser);
      if (parsed != null) {
        return parsed;
      }
    }

    throw new NoSuchParserException(parse, requestContentType);
  }

  @Override
  public <T> T parse(Class<T> type) throws NoSuchParserException, ParserException {
    return parse(NoOptParse.to(type));
  }

  private <P, S extends Parse<P>> P maybeParse(String requestContentType, S parseSpec, Parser<?, ?> parser) throws ParserException {
    if (requestContentType.equalsIgnoreCase(parser.getContentType()) && parser.getParseType().isInstance(parseSpec)) {
      @SuppressWarnings("unchecked") Parser<P, S> castParser = (Parser<P, S>) parser;
      try {
        return castParser.parse(this, getRequest().getBody(), parseSpec);
      } catch (Exception e) {
        throw new ParserException(parser, e);
      }
    } else {
      return null;
    }
  }

  @Override
  public void onClose(Action<? super RequestOutcome> callback) {
    requestConstants.onCloseRegistry.register(callback);
  }

  @Override
  public DirectChannelAccess getDirectChannelAccess() {
    return requestConstants.directChannelAccess;
  }

  @Override
  public Background getBackground() {
    return new DefaultBackground(requestConstants.foregroundExecutorService, requestConstants.applicationConstants.backgroundExecutorService, this, requestConstants.applicationConstants.contextThreadLocal);
  }

  @Override
  public <T> Background.SuccessOrError<T> background(Callable<T> backgroundOperation) {
    return getBackground().exec(backgroundOperation);
  }

  @Override
  public ScheduledExecutorService getForegroundExecutorService() {
    return requestConstants.foregroundExecutorService;
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
    Date ifModifiedSinceHeader = requestConstants.request.getHeaders().getDate(IF_MODIFIED_SINCE);
    long lastModifiedSecs = date.getTime() / 1000;

    if (ifModifiedSinceHeader != null) {
      long time = ifModifiedSinceHeader.getTime();
      long ifModifiedSinceSecs = time / 1000;

      if (lastModifiedSecs == ifModifiedSinceSecs) {
        requestConstants.response.status(NOT_MODIFIED.code(), NOT_MODIFIED.reasonPhrase()).send();
        return;
      }
    }

    requestConstants.response.getHeaders().setDate(HttpHeaders.Names.LAST_MODIFIED, date);
    runnable.run();
  }

  @Override
  public BindAddress getBindAddress() {
    return requestConstants.bindAddress;
  }

  public void error(Exception exception) {
    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.class);

    Exception unpacked = unpackException(exception);

    try {
      serverErrorHandler.error(this, unpacked);
    } catch (Exception errorHandlerException) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      stringWriter.
        append("Exception thrown by error handler ").
        append(serverErrorHandler.toString()).
        append(" while handling exception\nOriginal exception: ");
      unpacked.printStackTrace(printWriter);
      stringWriter.
        append("Error handler exception: ");
      errorHandlerException.printStackTrace(printWriter);
      LOGGER.warning(stringWriter.toString());
      requestConstants.response.status(500).send();
    }
  }

  private Exception unpackException(Exception exception) {
    if (exception instanceof UndeclaredThrowableException) {
      return ExceptionUtils.toException(exception.getCause());
    } else {
      return exception;
    }
  }

  public void clientError(int statusCode) {
    try {
      get(ClientErrorHandler.class).error(this, statusCode);
    } catch (Exception e) {
      dispatchException(e);
    }
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
          try {
            action.execute(result.getValue());
          } catch (Exception e) {
            dispatchException(e);
          }
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

  protected void doNext(Context parentContext, final Registry registry, final int nextIndex, final Handler[] nextHandlers, Handler exhausted) {
    Context context;
    Handler handler;

    if (nextIndex >= nextHandlers.length) {
      context = parentContext;
      handler = exhausted;
    } else {
      handler = nextHandlers[nextIndex];
      context = createContext(registry, nextHandlers, nextIndex + 1, exhausted);
    }

    requestConstants.applicationConstants.contextThreadLocal.set(context);

    try {
      handler.handle(context);
    } catch (Exception e) {
      if (e instanceof HandlerException) {
        throw (HandlerException) e;
      } else {
        throw new HandlerException(context, e);
      }
    }
  }

  private DefaultContext createContext(Registry registry, Handler[] nextHandlers, int nextIndex, Handler exhausted) {
    return new DefaultContext(requestConstants, registry, nextHandlers, nextIndex, exhausted);
  }

  private class RejoinHandler implements Handler {
    public void handle(Context context) throws Exception {
      doNext(DefaultContext.this, registry, nextIndex, nextHandlers, exhausted);
    }
  }
}
