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

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.*;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.*;
import ratpack.http.internal.DefaultRequest;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.parse.NoSuchParserException;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.path.InvalidPathEncodingException;
import ratpack.path.PathBinding;
import ratpack.path.internal.PathBindingStorage;
import ratpack.path.internal.RootPathBinding;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.render.internal.RenderController;
import ratpack.server.ServerConfig;
import ratpack.util.Exceptions;
import ratpack.util.Types;

import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_MODIFIED_SINCE;
import static ratpack.util.Exceptions.uncheck;

public class DefaultContext implements Context {

  private static final TypeToken<Parser<?>> PARSER_TYPE_TOKEN = Types.intern(new TypeToken<Parser<?>>() {});

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultContext.class);
  private final RequestConstants requestConstants;
  private Registry joinedRegistry;
  private PathBindingStorage pathBindings;

  public DefaultContext(RequestConstants requestConstants) {
    this.requestConstants = requestConstants;
  }

  public static Context current() {
    return Execution.current().get(Context.TYPE);
  }

  public static void start(EventLoop eventLoop, final RequestConstants requestConstants, Registry registry, Handler[] handlers, Action<? super Execution> onComplete) {
    ChainIndex index = new ChainIndex(handlers, registry, true);
    requestConstants.indexes.push(index);

    DefaultContext context = new DefaultContext(requestConstants);
    requestConstants.context = context;

    context.pathBindings = new PathBindingStorage(new RootPathBinding(requestConstants.request.getPath()));

    requestConstants.applicationConstants.execController.fork()
      .onError(throwable -> requestConstants.context.error(throwable instanceof HandlerException ? throwable.getCause() : throwable))
      .onComplete(onComplete)
      .register(s -> s
        .add(Context.TYPE, context)
        .add(Request.TYPE, requestConstants.request)
        .add(Response.TYPE, requestConstants.response)
        .add(PathBindingStorage.TYPE, context.pathBindings)
        .addLazy(RequestId.TYPE, () -> registry.get(RequestId.Generator.TYPE).generate(requestConstants.request))
      )
      .eventLoop(eventLoop)
      .onStart(e -> DefaultRequest.setDelegateRegistry(requestConstants.request, e))
      .start(e -> {
        requestConstants.execution = e;
        context.joinedRegistry = new ContextRegistry(context).join(requestConstants.execution);
        context.next();
      });
  }

  private Registry getCurrentRegistry() {
    return requestConstants.indexes.peek().registry;
  }

  @Override
  public Context getContext() {
    return this;
  }

  @Override
  public Execution getExecution() {
    return requestConstants.execution;
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
          requestConstants.indexes.push(new ChainIndex(((ChainHandler) handler).getHandlers(), getCurrentRegistry(), false));
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
    requestConstants.indexes.peek().registry = getCurrentRegistry().join(registry);
    next();
  }

  public void insert(Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    requestConstants.indexes.push(new ChainIndex(handlers, getCurrentRegistry(), false));
    next();
  }

  public void insert(final Registry registry, final Handler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("handlers is zero length");
    }

    requestConstants.indexes.push(new ChainIndex(handlers, getCurrentRegistry().join(registry), false));
    next();
  }

  public void byMethod(Action<? super ByMethodSpec> action) throws Exception {
    Handlers.byMethod(this, action).handle(this);
  }

  public void byContent(Action<? super ByContentSpec> action) throws Exception {
    Handlers.byContent(this, action).handle(this);
  }

  public void error(Throwable throwable) {
    if (throwable instanceof ClientErrorException) {
      clientError(((ClientErrorException) throwable).getClientErrorCode());
      return;
    }

    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.TYPE);
    throwable = unpackThrowable(throwable);

    ThrowableHolder throwableHolder = getRequest().maybeGet(ThrowableHolder.TYPE).orElse(null);
    if (throwableHolder == null) {
      getRequest().add(ThrowableHolder.TYPE, new ThrowableHolder(throwable));

      try {
        if (throwable instanceof InvalidPathEncodingException) {
          serverErrorHandler.error(this, (InvalidPathEncodingException) throwable);
        } else {
          serverErrorHandler.error(this, throwable);
        }
      } catch (Throwable errorHandlerThrowable) {
        onErrorHandlerError(serverErrorHandler, throwable, errorHandlerThrowable);
      }
    } else {
      onErrorHandlerError(serverErrorHandler, throwableHolder.getThrowable(), throwable);
    }
  }

  public void clientError(int statusCode) {
    try {
      get(ClientErrorHandler.TYPE).error(this, statusCode);
    } catch (Throwable e) {
      error(Exceptions.toException(e));
    }
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

  public void redirect(Object to) {
    redirect(302, to);
  }

  public void redirect(int code, Object to) {
    Redirector redirector = joinedRegistry.get(Redirector.TYPE);
    redirector.redirect(this, code, to);
  }

  @Override
  public void lastModified(Instant lastModified, Runnable runnable) {
    Instant ifModifiedSince = requestConstants.request.getHeaders().getInstant(IF_MODIFIED_SINCE);

    if (ifModifiedSince != null) {
      // Normalise to second resolution
      ifModifiedSince = Instant.ofEpochSecond(ifModifiedSince.getEpochSecond());
      lastModified = Instant.ofEpochSecond(lastModified.getEpochSecond());

      if (!lastModified.isAfter(ifModifiedSince)) {
        requestConstants.response.status(Status.NOT_MODIFIED).send();
        return;
      }
    }

    requestConstants.response.getHeaders().setDate(HttpHeaderConstants.LAST_MODIFIED, lastModified);
    runnable.run();
  }

  @Override
  public <T> Promise<T> parse(Class<T> type) {
    return parse(Parse.of(type));
  }

  @Override
  public <T> Promise<T> parse(TypeToken<T> type) {
    return parse(Parse.of(type));
  }

  public <T, O> Promise<T> parse(Class<T> type, O opts) {
    return parse(Parse.of(type, opts));
  }

  public <T, O> Promise<T> parse(TypeToken<T> type, O opts) {
    return parse(Parse.of(type, opts));
  }

  @Override
  public <T, O> Promise<T> parse(Parse<T, O> parse) {
    return getRequest().getBody()
      .map(b -> {
        try {
          return parse(b, parse);
        } finally {
          b.getBuffer().release();
        }
      });
  }

  @Override
  public <T, O> T parse(TypedData body, Parse<T, O> parse) throws Exception {
    Function<Parser<?>, T> parserPredicate;
    List<Parser<?>> parsers = Lists.newArrayList();
    if (parse.getOpts().isPresent()) {
      parserPredicate = parser -> {
        if (parser.getOptsType().isInstance(parse.getOpts().get())) {
          parsers.add(parser);
          Parser<O> cast = Types.cast(parser);
          return cast.parse(DefaultContext.this, body, parse);
        }
        return null;
      };
    } else {
      parserPredicate = parser -> {
        parsers.add(parser);
        Parser<O> cast = Types.cast(parser);
        return cast.parse(DefaultContext.this, body, parse);
      };
    }

    return joinedRegistry
      .first(PARSER_TYPE_TOKEN, parserPredicate)
      .orElseThrow(() -> new NoSuchParserException(parse.getType(), parse.getOpts().orElse(null), body.getContentType().getType(), parsers));
  }

  @Override
  public DirectChannelAccess getDirectChannelAccess() {
    return requestConstants;
  }

  @Override
  public PathBinding getPathBinding() {
    return pathBindings.peek();
  }

  @Override
  public void onClose(Action<? super RequestOutcome> callback) {
    requestConstants.responseTransmitter.addOutcomeListener(callback);
  }

  public Path file(String path) {
    return get(FileSystemBinding.TYPE).file(path);
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

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    if (type.equals(PathBinding.TYPE)) {
      return Types.cast(getPathBinding());
    }
    return joinedRegistry.get(type);
  }

  public static class ApplicationConstants {
    private final RenderController renderController;
    private final ExecController execController;
    private final ServerConfig serverConfig;
    private final Handler end;

    public ApplicationConstants(Registry registry, RenderController renderController, ExecController execController, Handler end) {
      this.renderController = renderController;
      this.execController = execController;
      this.serverConfig = registry.get(ServerConfig.TYPE);
      this.end = end;
    }
  }

  public static class RequestConstants implements DirectChannelAccess {
    private final ApplicationConstants applicationConstants;
    private final DefaultRequest request;
    private final Channel channel;
    private final ResponseTransmitter responseTransmitter;
    private final Action<Action<Object>> onTakeOwnership;
    private final Deque<ChainIndex> indexes = new ArrayDeque<>();
    public Response response;
    public Context context;
    public Handler handler;
    private Execution execution;

    public RequestConstants(ApplicationConstants applicationConstants, DefaultRequest request, Channel channel, ResponseTransmitter responseTransmitter, Action<Action<Object>> onTakeOwnership) {
      this.applicationConstants = applicationConstants;
      this.request = request;
      this.channel = channel;
      this.responseTransmitter = responseTransmitter;
      this.onTakeOwnership = onTakeOwnership;
    }

    @Override
    public Channel getChannel() {
      return channel;
    }

    @Override
    public void takeOwnership(Action<Object> messageReceiver) {
      try {
        onTakeOwnership.execute(messageReceiver);
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

  }

  private static class ChainIndex implements Iterator<Handler> {
    final Handler[] handlers;
    final boolean first;
    Registry registry;
    int i;

    private ChainIndex(Handler[] handlers, Registry registry, boolean first) {
      this.handlers = handlers;
      this.registry = registry;
      this.first = first;
    }

    @Override
    public boolean hasNext() {
      return i < handlers.length;
    }

    public Handler next() {
      return handlers[i++];
    }
  }

  private static class ContextRegistry implements Registry {
    private final DefaultContext context;

    public ContextRegistry(DefaultContext context) {
      this.context = context;
    }

    @Override
    public <O> Optional<O> maybeGet(TypeToken<O> type) {
      return context.getCurrentRegistry().maybeGet(type);
    }

    @Override
    public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
      return context.getCurrentRegistry().getAll(type);
    }
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return joinedRegistry.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return joinedRegistry.getAll(type);
  }

  @Override
  public <T, O> Optional<O> first(TypeToken<T> type, Function<? super T, ? extends O> function) throws Exception {
    return joinedRegistry.first(type, function);
  }

}
