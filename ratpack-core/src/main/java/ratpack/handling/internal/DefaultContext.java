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
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.event.internal.EventRegistry;
import ratpack.exec.*;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.func.NoArgAction;
import ratpack.handling.*;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.internal.ContentNegotiationHandler;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.MultiMethodHandler;
import ratpack.server.ServerConfig;
import ratpack.parse.NoSuchParserException;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.parse.ParserException;
import ratpack.parse.internal.ParserForParsePredicate;
import ratpack.path.PathBinding;
import ratpack.path.PathTokens;
import ratpack.path.internal.DefaultPathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.render.internal.RenderController;
import ratpack.stream.TransformablePublisher;
import ratpack.util.ExceptionUtils;
import ratpack.util.Types;

import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class DefaultContext implements Context {

  private static final TypeToken<Parser<?>> PARSER_TYPE_TOKEN = new TypeToken<Parser<?>>() {
  };

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultContext.class);

  public static class ApplicationConstants {
    private final RenderController renderController;
    private final ServerConfig serverConfig;
    private final ExecControl execControl;
    private final Handler end;

    public ApplicationConstants(Registry registry, RenderController renderController, Handler end) {
      this.renderController = renderController;
      this.serverConfig = registry.get(ServerConfig.class);
      this.execControl = registry.get(ExecController.class).getControl();
      this.end = end;
    }
  }

  public static class RequestConstants {
    private final ApplicationConstants applicationConstants;

    private final Request request;
    private final Response response;

    private final DirectChannelAccess directChannelAccess;
    private final EventRegistry<RequestOutcome> onCloseRegistry;

    private final Deque<ChainIndex> indexes = Lists.newLinkedList();

    public Context context;
    public Handler handler;

    public RequestConstants(
      ApplicationConstants applicationConstants, Request request, Response response,
      DirectChannelAccess directChannelAccess, EventRegistry<RequestOutcome> onCloseRegistry
    ) {
      this.applicationConstants = applicationConstants;
      this.request = request;
      this.response = response;
      this.directChannelAccess = directChannelAccess;
      this.onCloseRegistry = onCloseRegistry;
    }

  }

  private static class ChainIndex implements Iterator<Handler> {
    final Handler[] handlers;
    Registry registry;
    final boolean first;
    int i;

    private ChainIndex(Handler[] handlers, Registry registry, boolean first) {
      this.handlers = handlers;
      this.registry = registry;
      this.first = first;
    }

    public Handler next() {
      return handlers[i++];
    }

    @Override
    public boolean hasNext() {
      return i < handlers.length;
    }
  }

  private final RequestConstants requestConstants;

  public static void start(EventLoop eventLoop, ExecControl execControl, final RequestConstants requestConstants, Registry registry, Handler[] handlers, Action<? super Execution> onComplete) {
    ChainIndex index = new ChainIndex(handlers, registry, true);
    requestConstants.indexes.push(index);

    DefaultContext context = new DefaultContext(requestConstants);
    requestConstants.context = context;

    //noinspection Convert2MethodRef
    execControl.exec()
      .onError(throwable -> requestConstants.context.error(throwable instanceof HandlerException ? throwable.getCause() : throwable))
      .onComplete(onComplete)
      .eventLoop(eventLoop)
      .start(e -> context.next());
  }

  public DefaultContext(RequestConstants requestConstants) {
    this.requestConstants = requestConstants;
  }

  private Registry getRegistry() {
    return requestConstants.indexes.peek().registry;
  }

  private void setRegistry(Registry registry) {
    requestConstants.indexes.peek().registry = registry;
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
  public ExecStarter exec() {
    return requestConstants.applicationConstants.execControl.exec();
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, NoArgAction continuation) throws Exception {
    requestConstants.applicationConstants.execControl.addInterceptor(execInterceptor, continuation);
  }

  @Override
  public <T> TransformablePublisher<T> stream(Publisher<T> publisher) {
    return requestConstants.applicationConstants.execControl.stream(publisher);
  }

  @Override
  public ServerConfig getServerConfig() {
    return requestConstants.applicationConstants.serverConfig;
  }

  public Request getRequest() {
    return requestConstants.request;
  }

  public Response getResponse() {
    return requestConstants.response;
  }

  public void next() {
    Handler handler = null;

    ChainIndex index = requestConstants.indexes.peek();
    while (handler == null) {
      if (index.hasNext()) {
        handler = index.next();
        if (handler.getClass().equals(ChainHandler.class)) {
          requestConstants.indexes.push(new ChainIndex(((ChainHandler) handler).getHandlers(), getRegistry(), false));
          index = requestConstants.indexes.peek();
          handler = null;
        }
      } else if (index.first) {
        handler = requestConstants.applicationConstants.end;
      } else {
        requestConstants.indexes.pop();
        index = requestConstants.indexes.peek();
      }
    }

    try {
      requestConstants.handler = handler;
      handler.handle(this);
    } catch (Throwable e) {
      if (e instanceof HandlerException) {
        throw (HandlerException) e;
      } else {
        throw new HandlerException(e);
      }
    }
  }

  @Override
  public void next(Registry registry) {
    setRegistry(getRegistry().join(registry));
    next();
  }

  public void insert(Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    requestConstants.indexes.push(new ChainIndex(handlers, getRegistry(), false));
    next();
  }

  public void insert(final Registry registry, final Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    requestConstants.indexes.push(new ChainIndex(handlers, getRegistry().join(registry), false));
    next();
  }

  public PathTokens getPathTokens() {
    return maybeGet(PathBinding.class)
      .map(PathBinding::getTokens)
      .orElseGet(DefaultPathTokens::empty);
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

    final String finalRequestContentType = requestContentType;
    return getRegistry().first(PARSER_TYPE_TOKEN, new ParserForParsePredicate(parse, requestContentType))
      .<T>map(parser -> {
        Parser<O> castParser = Types.cast(parser);
        try {
          return castParser.parse(this, getRequest().getBody(), parse);
        } catch (Exception e) {
          throw new ParserException(parser, e);
        }
      })
      .orElseThrow(() -> new NoSuchParserException(parse.getType(), parse.getOpts(), finalRequestContentType));
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
    Redirector redirector = getRegistry().get(Redirector.class);
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

  public void error(Throwable throwable) {
    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.class);
    throwable = unpackThrowable(throwable);

    ThrowableHolder throwableHolder = getRequest().maybeGet(ThrowableHolder.class).orElse(null);
    if (throwableHolder == null) {
      getRequest().add(ThrowableHolder.class, new ThrowableHolder(throwable));

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
    if (getServerConfig().isDevelopment()) {
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
    Map<String, NoArgAction> handlers = new LinkedHashMap<>(2);
    DefaultByMethodSpec spec = new DefaultByMethodSpec(handlers);
    action.execute(spec);
    new MultiMethodHandler(handlers).handle(this);
  }

  public void byContent(Action<? super ByContentSpec> action) throws Exception {
    Map<String, NoArgAction> handlers = new LinkedHashMap<>(2);
    DefaultByContentSpec spec = new DefaultByContentSpec(handlers);
    action.execute(spec);
    new ContentNegotiationHandler(handlers, spec.getNoMatchHandler()).handle(this);
  }

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    return getRegistry().get(type);
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return getRegistry().maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return getRegistry().getAll(type);
  }

  @Override
  public <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate) {
    return getRegistry().first(type, predicate);
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return getRegistry().all(type, predicate);
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    return getRegistry().each(type, predicate, action);
  }

}
