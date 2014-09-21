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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.event.internal.EventRegistry;
import ratpack.exec.*;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.handling.*;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.internal.ContentNegotiationHandler;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.MultiMethodHandler;
import ratpack.launch.LaunchConfig;
import ratpack.parse.NoSuchParserException;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.parse.ParserException;
import ratpack.path.PathBinding;
import ratpack.path.PathTokens;
import ratpack.path.internal.DefaultPathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.render.internal.RenderController;
import ratpack.server.BindAddress;
import ratpack.util.ExceptionUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class DefaultContext implements Context {

  private static final TypeToken<Parser<?>> PARSER_TYPE_TOKEN = new TypeToken<Parser<?>>() {
    private static final long serialVersionUID = 0;
  };

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultContext.class);

  public static class ApplicationConstants {
    private final RenderController renderController;
    private final LaunchConfig launchConfig;
    private final ExecControl execControl;

    public ApplicationConstants(LaunchConfig launchConfig, RenderController renderController) {
      this.renderController = renderController;
      this.launchConfig = launchConfig;
      this.execControl = launchConfig.getExecController().getControl();
    }
  }

  public static class RequestConstants {
    private final ApplicationConstants applicationConstants;

    private final BindAddress bindAddress;
    private final Request request;
    private final Response response;

    private final DirectChannelAccess directChannelAccess;
    private final EventRegistry<RequestOutcome> onCloseRegistry;

    public Context context;
    public Handler handler;

    public RequestConstants(
      ApplicationConstants applicationConstants, BindAddress bindAddress, Request request, Response response,
      DirectChannelAccess directChannelAccess, EventRegistry<RequestOutcome> onCloseRegistry
    ) {
      this.applicationConstants = applicationConstants;
      this.bindAddress = bindAddress;
      this.request = request;
      this.response = response;
      this.directChannelAccess = directChannelAccess;
      this.onCloseRegistry = onCloseRegistry;
    }
  }

  private final RequestConstants requestConstants;

  private final Registry registry;

  private final Handler[] nextHandlers;
  private final int nextIndex;
  private final Handler exhausted;

  public static void start(ExecControl execControl, final RequestConstants requestConstants, Registry registry, Handler[] nextHandlers, Handler exhausted, Action<? super Execution> onComplete) {
    final DefaultContext context = new DefaultContext(requestConstants, registry, nextHandlers, 0, exhausted);

    execControl.fork(execution -> context.next(), throwable -> requestConstants.context.error(throwable instanceof HandlerException ? throwable.getCause() : throwable), onComplete);
  }

  public DefaultContext(RequestConstants requestConstants, Registry registry, Handler[] nextHandlers, int nextIndex, Handler exhausted) {
    this.requestConstants = requestConstants;
    this.registry = registry;
    this.nextHandlers = nextHandlers;
    this.nextIndex = nextIndex;
    this.exhausted = exhausted;

    this.requestConstants.context = this;
  }

  @Override
  public Context getContext() {
    return this;
  }

  @Override
  public ExecController getController() {
    return requestConstants.applicationConstants.execControl.getController();
  }

  @Override
  public Execution getExecution() {
    return requestConstants.applicationConstants.execControl.getExecution();
  }

  @Override
  public <T> Promise<T> blocking(Callable<T> blockingOperation) {
    return requestConstants.applicationConstants.execControl.blocking(blockingOperation);
  }

  @Override
  public <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return requestConstants.applicationConstants.execControl.promise(action);
  }

  @Override
  public void fork(Action<? super Execution> action) {
    requestConstants.applicationConstants.execControl.fork(action);
  }

  @Override
  public void fork(Action<? super Execution> action, Action<? super Throwable> onError) {
    requestConstants.applicationConstants.execControl.fork(action, onError);
  }

  @Override
  public void fork(Action<? super Execution> action, Action<? super Throwable> onError, Action<? super Execution> onComplete) {
    requestConstants.applicationConstants.execControl.fork(action, onError, onComplete);
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, Action<? super Execution> continuation) throws Exception {
    requestConstants.applicationConstants.execControl.addInterceptor(execInterceptor, continuation);
  }

  @Override
  public <T> void stream(Publisher<T> publisher, Subscriber<? super T> subscriber) {
    requestConstants.applicationConstants.execControl.stream(publisher, subscriber);
  }

  @Override
  public LaunchConfig getLaunchConfig() {
    return requestConstants.applicationConstants.launchConfig;
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

  public <O> Iterable<? extends O> getAll(Class<O> type) {
    return registry.getAll(type);
  }

  public <O> O maybeGet(Class<O> type) {
    return registry.maybeGet(type);
  }

  public void next() {
    doNext(this, registry, nextIndex, nextHandlers, exhausted);
  }

  @Override
  public void next(Registry registry) {
    Registry joinedRegistry = Registries.join(DefaultContext.this.registry, registry);
    doNext(DefaultContext.this, joinedRegistry, nextIndex, nextHandlers, new RejoinHandler());
  }

  public void insert(Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    doNext(this, registry, 0, handlers, new RejoinHandler());
  }

  public void insert(final Registry registry, final Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    final Registry joinedRegistry = Registries.join(DefaultContext.this.registry, registry);
    doNext(DefaultContext.this, joinedRegistry, 0, handlers, new RejoinHandler());
  }

  public PathTokens getPathTokens() {
    PathBinding pathBinding = maybeGet(PathBinding.class);
    if (pathBinding == null) {
      return new DefaultPathTokens(ImmutableMap.<String, String>of());
    } else {
      return pathBinding.getTokens();
    }
  }

  public PathTokens getAllPathTokens() {
    return get(PathBinding.class).getAllTokens();
  }

  public Path file(String path) {
    return get(FileSystemBinding.class).file(path);
  }

  public void render(Object object) throws NoSuchRendererException {
    try {
      requestConstants.applicationConstants.renderController.render(object, this);
    } catch (NoSuchRendererException e) {
      throw e;
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public <T, O> T parse(Parse<T, O> parse) throws ParserException, NoSuchParserException {
    String requestContentType = requestConstants.request.getBody().getContentType().getType();
    if (requestContentType == null) {
      requestContentType = "text/plain";
    }

    Parser<?> parser = registry.first(PARSER_TYPE_TOKEN, new ParserForParsePredicate(parse, requestContentType));
    if (parser != null) {
      @SuppressWarnings("unchecked") Parser<O> castParser = (Parser<O>) parser;
      try {
        return castParser.parse(this, getRequest().getBody(), parse);
      } catch (Exception e) {
        throw new ParserException(parser, e);
      }
    } else {
      throw new NoSuchParserException(parse.getType(), parse.getOpts(), requestContentType);
    }
  }

  @Override
  public <T> T parse(Class<T> type) throws NoSuchParserException, ParserException {
    return parse(Parse.of(type));
  }

  @Override
  public <T> T parse(TypeToken<T> type) throws NoSuchParserException, ParserException {
    return parse(Parse.of(type));
  }

  public <T, O> T parse(Class<T> type, O opts) {
    return parse(Parse.of(type, opts));
  }

  public <T, O> T parse(TypeToken<T> type, O opts) {
    return parse(Parse.of(type, opts));
  }

  @Override
  public void onClose(Action<? super RequestOutcome> callback) {
    requestConstants.onCloseRegistry.register(callback);
  }

  @Override
  public DirectChannelAccess getDirectChannelAccess() {
    return requestConstants.directChannelAccess;
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
        requestConstants.response.status(NOT_MODIFIED.code()).send();
        return;
      }
    }

    requestConstants.response.getHeaders().setDate(HttpHeaderConstants.LAST_MODIFIED, date);
    runnable.run();
  }

  @Override
  public BindAddress getBindAddress() {
    return requestConstants.bindAddress;
  }

  public void error(Throwable throwable) {
    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.class);
    throwable = unpackThrowable(throwable);

    ThrowableHolder throwableHolder = getRequest().maybeGet(ThrowableHolder.class);
    if (throwableHolder == null) {
      getRequest().register(ThrowableHolder.class, new ThrowableHolder(throwable));

      try {
        serverErrorHandler.error(this, throwable);
      } catch (Throwable errorHandlerThrowable) {
        onErrorHandlerError(serverErrorHandler, throwable, errorHandlerThrowable);
      }
    } else {
      onErrorHandlerError(serverErrorHandler, throwableHolder.getThrowable(), throwable);
    }
  }

  private void onErrorHandlerError(ServerErrorHandler serverErrorHandler, Throwable original, Throwable errorHandlerThrowable) {
    String msg = "Throwable thrown by error handler " + serverErrorHandler + " while handling throwable\n"
      + "Original throwable: " + getStackTraceAsString(original) + "\n"
      + "Error handler throwable: " + getStackTraceAsString(errorHandlerThrowable);

    LOGGER.error(msg);

    Response response = requestConstants.response.status(500);
    if (getLaunchConfig().isDevelopment()) {
      response.send(msg);
    } else {
      response.send();
    }
  }

  private Throwable unpackThrowable(Throwable throwable) {
    if (throwable instanceof UndeclaredThrowableException) {
      return throwable.getCause();
    } else {
      return throwable;
    }
  }

  public void clientError(int statusCode) {
    try {
      get(ClientErrorHandler.class).error(this, statusCode);
    } catch (Throwable e) {
      error(ExceptionUtils.toException(e));
    }
  }

  public void byMethod(Action<? super ByMethodSpec> action) throws Exception {
    Map<String, Handler> handlers = new LinkedHashMap<>(2);
    DefaultByMethodSpec spec = new DefaultByMethodSpec(handlers);
    action.execute(spec);
    new MultiMethodHandler(handlers).handle(this);
  }

  public void byContent(Action<? super ByContentSpec> action) throws Exception {
    Map<String, Handler> handlers = new LinkedHashMap<>(2);
    DefaultByContentSpec spec = new DefaultByContentSpec(handlers);
    action.execute(spec);
    new ContentNegotiationHandler(handlers).handle(this);
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

    try {
      requestConstants.handler = handler;
      handler.handle(context);
    } catch (Throwable e) {
      if (e instanceof HandlerException) {
        throw (HandlerException) e;
      } else {
        throw new HandlerException(e);
      }
    }
  }

  private static class HandlerException extends Error {
    private static final long serialVersionUID = 0;

    private HandlerException(Throwable cause) {
      super(cause);
    }
  }

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    return registry.get(type);
  }

  @Override
  @Nullable
  public <O> O maybeGet(TypeToken<O> type) {
    return registry.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return registry.getAll(type);
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.first(type, predicate);
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.all(type, predicate);
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    return registry.each(type, predicate, action);
  }

  private DefaultContext createContext(Registry registry, Handler[] nextHandlers, int nextIndex, Handler exhausted) {
    return new DefaultContext(requestConstants, registry, nextHandlers, nextIndex, exhausted);
  }

  private class RejoinHandler implements Handler {
    public void handle(Context context) throws Exception {
      doNext(DefaultContext.this, registry, nextIndex, nextHandlers, exhausted);
    }
  }

  private static class ParserForParsePredicate implements Predicate<Parser<?>> {
    private final Parse<?, ?> parse;
    private final String contentType;

    private ParserForParsePredicate(Parse<?, ?> parse, String contentType) {
      this.parse = parse;
      this.contentType = contentType;
    }

    @Override
    public boolean apply(Parser<?> parser) {
      return contentType.equalsIgnoreCase(parser.getContentType()) && parser.getOptsType().isInstance(parse.getOpts());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ParserForParsePredicate that = (ParserForParsePredicate) o;
      return contentType.equalsIgnoreCase(that.contentType) && parse.equals(that.parse);
    }

    @Override
    public int hashCode() {
      int result = contentType.hashCode();
      result = 31 * result + parse.hashCode();
      return result;
    }
  }

  private static class ThrowableHolder {

    private final Throwable throwable;

    public ThrowableHolder(Throwable throwable) {
      this.throwable = throwable;
    }

    public Throwable getThrowable() {
      return throwable;
    }

  }
}
