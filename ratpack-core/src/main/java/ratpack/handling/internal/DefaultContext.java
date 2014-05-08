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
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpResponseStatus;
import ratpack.api.Nullable;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.event.internal.EventRegistry;
import ratpack.exec.ExecContext;
import ratpack.exec.ExecController;
import ratpack.exec.ExecException;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.internal.AbstractExecContext;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.handling.*;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.launch.LaunchConfig;
import ratpack.parse.NoSuchParserException;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.parse.ParserException;
import ratpack.path.PathBinding;
import ratpack.path.PathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.render.internal.RenderController;
import ratpack.server.BindAddress;
import ratpack.util.ExceptionUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class DefaultContext extends AbstractExecContext implements Context {

  public static class ApplicationConstants {
    private final RenderController renderController;
    private final LaunchConfig launchConfig;

    public ApplicationConstants(LaunchConfig launchConfig, RenderController renderController) {
      this.renderController = renderController;
      this.launchConfig = launchConfig;
    }
  }

  public static class RequestConstants {
    private final ApplicationConstants applicationConstants;

    private final BindAddress bindAddress;
    private final Request request;
    private final Response response;

    private final DirectChannelAccess directChannelAccess;
    private final EventRegistry<RequestOutcome> onCloseRegistry;

    private final List<ExecInterceptor> interceptors = new CopyOnWriteArrayList<>();

    public ExecContext context;

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
    requestConstants.context = this;
  }

  @Override
  public Context getContext() {
    return this;
  }

  @Override
  public Supplier getSupplier() {
    return new Supplier() {
      @Override
      public ExecContext get() {
        return requestConstants.context;
      }
    };
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

  public <O> List<O> getAll(Class<O> type) {
    return registry.getAll(type);
  }

  @Override
  public void addExecInterceptor(final ExecInterceptor execInterceptor, final Action<? super Context> action) throws Exception {
    requestConstants.interceptors.add(execInterceptor);
    new InterceptedOperation(ExecInterceptor.ExecType.COMPUTE, Collections.singletonList(execInterceptor)) {
      @Override
      protected void performOperation() throws Exception {
        action.execute(DefaultContext.this);
      }
    }.run();
  }

  public List<ExecInterceptor> getInterceptors() {
    return requestConstants.interceptors;
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

  public void respond(Handler handler) {
    try {
      handler.handle(this);
    } catch (Throwable e) {
      throw ExecException.wrap(this, e);
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
    @SuppressWarnings("rawtypes")
    List<Parser> all = registry.getAll(Parser.class);
    String requestContentType = requestConstants.request.getBody().getContentType().getType();
    if (requestContentType == null) {
      requestContentType = "text/plain";
    }
    for (Parser<?> parser : all) {
      T parsed = maybeParse(requestContentType, parser, parse);
      if (parsed != null) {
        return parsed;
      }
    }

    throw new NoSuchParserException(parse.getType(), parse.getOpts(), requestContentType);
  }


  @Override
  public <T> T parse(Class<T> type) throws NoSuchParserException, ParserException {
    return parse(Parse.of(type));
  }

  public <T, O> T parse(Class<T> type, O opts) {
    return parse(Parse.of(type, opts));
  }

  private <T, O> T maybeParse(String requestContentType, Parser<?> parser, Parse<T, O> parse) throws ParserException {
    Class<?> optsType = parser.getOptsType();
    String contentType = parser.getContentType();

    if (requestContentType.equalsIgnoreCase(contentType) && optsType.isInstance(parse.getOpts())) {
      @SuppressWarnings("unchecked") Parser<O> castParser = (Parser<O>) parser;
      try {
        return castParser.parse(this, getRequest().getBody(), parse);
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
  public ExecController getExecController() {
    return requestConstants.applicationConstants.launchConfig.getExecController();
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

    requestConstants.response.getHeaders().setDate(HttpHeaderConstants.LAST_MODIFIED, date);
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
    } catch (Throwable e) {
      throw ExecException.wrap(this, e);
    }
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

    try {
      handler.handle(context);
    } catch (Throwable e) {
      throw ExecException.wrap(context, e);
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
  public <O> List<O> getAll(TypeToken<O> type) {
    return registry.getAll(type);
  }

  @Nullable
  @Override
  public <T> T first(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.first(type, predicate);
  }

  @Override
  public <T> List<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return registry.all(type, predicate);
  }

  @Override
  public <T> boolean first(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    return registry.first(type, predicate, action);
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
}
