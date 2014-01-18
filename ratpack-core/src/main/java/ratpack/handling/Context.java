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

package ratpack.handling;

import ratpack.api.NonBlocking;
import ratpack.background.Background;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.parse.NoSuchParserException;
import ratpack.parse.Parse;
import ratpack.parse.ParserException;
import ratpack.path.PathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.server.BindAddress;
import ratpack.util.Action;
import ratpack.util.Factory;
import ratpack.util.ResultAction;

import javax.inject.Provider;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The context of an individual {@link Handler} invocation.
 * <p>
 * It provides:
 * <ul>
 * <li>Access the HTTP {@link #getRequest() request} and {@link #getResponse() response}</li>
 * <li>Delegation (via the {@link #next} and {@link #insert} family of methods)</li>
 * <li>Access to <i>contextual objects</i> (see below)</li>
 * <li>Convenience for common handler operations</li>
 * </ul>
 * </p>
 * <h4>Contextual objects</h4>
 * <p>
 * A context is also a {@link Registry} of objects.
 * Arbitrary objects can be "pushed" into the context for use by <i>downstream</i> handlers.
 * <p>
 * There are some significant contextual objects that drive key infrastructure.
 * For example, error handling is based on informing the contextual {@link ratpack.error.ServerErrorHandler} of exceptions.
 * The error handling strategy for an application can be changed by pushing a new implementation of this interface into the context that is used downstream.
 * <p>
 * See {@link #insert(Handler...)} for more on how to do this.
 * <h5>Default contextual objects</h5>
 * <p>There is also a set of default objects that are made available via the Ratpack infrastructure:
 * <ul>
 * <li>The effective {@link ratpack.launch.LaunchConfig}</li>
 * <li>A {@link ratpack.file.FileSystemBinding} that is the application {@link ratpack.launch.LaunchConfig#getBaseDir()}</li>
 * <li>A {@link ratpack.file.MimeTypes} implementation</li>
 * <li>A {@link ratpack.error.ServerErrorHandler}</li>
 * <li>A {@link ratpack.error.ClientErrorHandler}</li>
 * <li>A {@link ratpack.file.FileRenderer}</li>
 * <li>A {@link ratpack.server.BindAddress}</li>
 * <li>A {@link ratpack.server.PublicAddress}</li>
 * <li>A {@link Redirector}</li>
 * </ul>
 */
@SuppressWarnings("UnusedDeclaration")
public interface Context extends Registry {

  /**
   * Returns this.
   *
   * @return this.
   */
  Context getContext();

  /**
   * A provider that always returns the current context for the current thread.
   * <p>
   * This DOES NOT always return <i>this</i> context.
   * The context returned by this provider is the context being used on the current thread.
   * That is, it acts like thread local storage of the current context.
   * Moreover, the provider returned by successive calls to this method on any context instance will provide a functionally identical provider.
   * <p>
   * This method is primary provided for integration with dependency injection frameworks.
   *
   * @return A provider that always provides the context object for the current thread.
   */
  Provider<Context> getProvider();

  /**
   * The HTTP request.
   *
   * @return The HTTP request.
   */
  Request getRequest();

  /**
   * The HTTP response.
   *
   * @return The HTTP response.
   */
  Response getResponse();

  /**
   * Delegate handling to the next handler in line.
   * <p>
   * The request and response of this object should not be accessed after this method is called.
   */
  @NonBlocking
  void next();

  /**
   * Invokes the next handler, after adding the given registry.
   * <p>
   * The given registry is appended to the existing.
   * This means that it can shadow objects previously available.
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Handlers;
   * import ratpack.handling.Chain;
   * import ratpack.handling.ChainAction;
   * import ratpack.handling.Context;
   * import ratpack.launch.HandlerFactory;
   * import ratpack.launch.LaunchConfig;
   * import ratpack.launch.LaunchConfigBuilder;
   * import ratpack.util.Factory;
   * import ratpack.registry.Registry;
   * import ratpack.registry.RegistryBuilder;
   *
   * public interface SomeThing {}
   * public class SomeThingImpl implements SomeThing {}
   *
   * public class UpstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     Registry registry = RegistryBuilder.builder().
   *       add(SomeThing.class, new SomeThingImpl()).
   *       build();
   *
   *     context.next(registry);
   *   }
   * }
   *
   * public class DownstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     SomeThing someThing = context.get(SomeThing.class); // instance provided upstream
   *     assert someThing instanceof SomeThingImpl;
   *     // …
   *   }
   * }
   *
   * LaunchConfigBuilder.baseDir(new File("base")).build(new HandlerFactory() {
   *   public Handler create(LaunchConfig launchConfig) {
   *     return Handlers.chain(launchConfig, new ChainAction() {
   *       public void execute(Chain chain) {
   *         chain.handler(new UpstreamHandler());
   *         chain.handler(new DownstreamHandler());
   *       }
   *     });
   *   }
   * });
   * </pre>
   *
   * @param registry The registry to make available for subsequent handlers.
   */
  @NonBlocking
  void next(Registry registry);

  /**
   * Invokes the next handler, after adding the given object into the registry.
   * <p>
   * The object will be available by the given public type.
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Handlers;
   * import ratpack.handling.Chain;
   * import ratpack.handling.ChainAction;
   * import ratpack.handling.Context;
   * import ratpack.launch.HandlerFactory;
   * import ratpack.launch.LaunchConfig;
   * import ratpack.launch.LaunchConfigBuilder;
   * import ratpack.util.Factory;
   *
   * public interface SomeThing {}
   * public class SomeThingImpl implements SomeThing {}
   *
   * public class UpstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     context.next(SomeThing.class, new SomeThingImpl());
   *   }
   * }
   *
   * public class DownstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     SomeThing someThing = context.get(SomeThing.class); // instance provided upstream
   *     assert someThing instanceof SomeThingImpl;
   *     // …
   *   }
   * }
   *
   * LaunchConfigBuilder.baseDir(new File("base")).build(new HandlerFactory() {
   *   public Handler create(LaunchConfig launchConfig) {
   *     return Handlers.chain(launchConfig, new ChainAction() {
   *       public void execute(Chain chain) {
   *         chain.handler(new UpstreamHandler());
   *         chain.handler(new DownstreamHandler());
   *       }
   *     });
   *   }
   * });
   * </pre>
   *
   * @param publicType The public (i.e. advertised) type of the object
   * @param implementation The implementation object
   * @param <P> The public (i.e. advertised) type of the object
   * @param <T> The concrete type of the implementation object
   */
  @NonBlocking
  <P, T extends P> void next(Class<P> publicType, T implementation);

  /**
   * Invokes the next handler, after adding the given object into the registry (provided by factory).
   * <p>
   * The object will be available by the given public type.
   * <p>
   * If an object of the given type is requested, the given factory will be executed to provide the object.
   * It is guaranteed to only be executed once, and need not be thread safe.
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Handlers;
   * import ratpack.handling.Chain;
   * import ratpack.handling.ChainAction;
   * import ratpack.handling.Context;
   * import ratpack.launch.HandlerFactory;
   * import ratpack.launch.LaunchConfig;
   * import ratpack.launch.LaunchConfigBuilder;
   * import ratpack.util.Factory;
   *
   * public interface SomeThing {}
   * public class SomeThingImpl implements SomeThing {}
   *
   * public class UpstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     context.next(SomeThing.class, new Factory&lt;SomeThing&gt;() {
   *       public SomeThing create() {
   *         return new SomeThingImpl();
   *       }
   *     });
   *   }
   * }
   *
   * public class DownstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     SomeThing someThing = context.get(SomeThing.class); // instance provided upstream
   *     assert someThing instanceof SomeThingImpl;
   *     // …
   *   }
   * }
   *
   * LaunchConfigBuilder.baseDir(new File("base")).build(new HandlerFactory() {
   *   public Handler create(LaunchConfig launchConfig) {
   *     return Handlers.chain(launchConfig, new ChainAction() {
   *       public void execute(Chain chain) {
   *         chain.handler(new UpstreamHandler());
   *         chain.handler(new DownstreamHandler());
   *       }
   *     });
   *   }
   * });
   * </pre>
   *
   * @param publicType The public (i.e. advertised) type of the object
   * @param <T> The public (i.e. advertised) type of the object
   * @param factory The factory that can create the object if it is requested
   */
  @NonBlocking
  <T> void next(Class<T> publicType, Factory<? extends T> factory);

  /**
   * Invokes the next handler, after adding the given object into the registry.
   * <p>
   * The object will be available only by its concrete type.
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Handlers;
   * import ratpack.handling.Chain;
   * import ratpack.handling.ChainAction;
   * import ratpack.handling.Context;
   * import ratpack.launch.HandlerFactory;
   * import ratpack.launch.LaunchConfig;
   * import ratpack.launch.LaunchConfigBuilder;
   *
   * public class SomeThing {}
   *
   * public class UpstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     context.next(new SomeThing());
   *   }
   * }
   *
   * public class DownstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     SomeThing someThing = context.get(SomeThing.class); // instance provided upstream
   *     // …
   *   }
   * }
   *
   * LaunchConfigBuilder.baseDir(new File("base")).build(new HandlerFactory() {
   *   public Handler create(LaunchConfig launchConfig) {
   *     return Handlers.chain(launchConfig, new ChainAction() {
   *       public void execute(Chain chain) {
   *         chain.handler(new UpstreamHandler());
   *         chain.handler(new DownstreamHandler());
   *       }
   *     });
   *   }
   * });
   * </pre>
   *
   * @param object The object to insert into the registry.
   */
  @NonBlocking
  void next(Object object);

  /**
   * Inserts some handlers into the pipeline, then delegates to the first.
   * <p>
   * The request and response of this object should not be accessed after this method is called.
   *
   * @param handlers The handlers to insert.
   */
  @NonBlocking
  void insert(Handler... handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the given registry, then delegates to the first.
   * <p>
   * The given registry is only applicable to the inserted handlers.
   * <p>
   * Almost always, the registry should be a super set of the current registry.
   *
   * @param handlers The handlers to insert
   * @param registry The registry for the inserted handlers
   */
  @NonBlocking
  void insert(Registry registry, Handler... handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the given object created by the factory made available, then delegates to the first.
   * <p>
   * The given object will take precedence over an existing contextual object advertised by the given advertised type.
   * <p>
   * The object will only be retrievable by the type that is given and will be created on demand (once) from the factory.
   *
   * @param handlers The handlers to insert
   * @param publicType The advertised type of the object (i.e. what it is retrievable by)
   * @param <T> The advertised type of the object (i.e. what it is retrievable by)
   * @param factory The factory that creates the object lazily
   */
  @NonBlocking
  <T> void insert(Class<T> publicType, Factory<? extends T> factory, Handler... handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the given object made available, then delegates to the first.
   * <p>
   * The given object will take precedence over an existing contextual object advertised by the given advertised type.
   * <p>
   * The object will only be retrievable by the type that is given.
   *
   * @param handlers The handlers to insert
   * @param publicType The advertised type of the object (i.e. what it is retrievable by)
   * @param <P> The advertised type of the object (i.e. what it is retrievable by)
   * @param <T> The type of the implementation object
   * @param implementation The actual implementation
   */
  @NonBlocking
  <P, T extends P> void insert(Class<P> publicType, T implementation, Handler... handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the the given object added to the service, then delegates to the first.
   * <p>
   * The given object will take precedence over any existing object available via its concrete type.
   *
   * @param handlers The handlers to insert
   * @param object The object to add to the service for the handlers
   */
  @NonBlocking
  void insert(Object object, Handler... handlers);

  /**
   * Convenience method for delegating to a single handler.
   * <p>
   * Designed to be used in conjunction with the {@link #getByMethod()} and {@link #getByContent()} methods.
   *
   * @param handler The handler to invoke
   * @see ByContentHandler
   * @see ByMethodHandler
   */
  @NonBlocking
  void respond(Handler handler);

  /**
   * A buildable handler for conditional processing based on the HTTP request method.
   *
   * @return A buildable handler for conditional processing based on the HTTP request method.
   */
  ByMethodHandler getByMethod();

  /**
   * A buildable handler useful for performing content negotiation.
   *
   * @return A buildable handler useful for performing content negotiation.
   */
  ByContentHandler getByContent();


  // Shorthands for common service lookups

  /**
   * Forwards the exception to the {@link ratpack.error.ServerErrorHandler} in this service.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.error.ServerErrorHandler} in all contexts.
   * A {@link ratpack.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param exception The exception that occurred
   * @throws NotInRegistryException if no {@link ratpack.error.ServerErrorHandler} can be found in the service
   */
  @NonBlocking
  void error(Exception exception) throws NotInRegistryException;

  /**
   * Forwards the error to the {@link ratpack.error.ClientErrorHandler} in this service.
   *
   * The default configuration of Ratpack includes a {@link ratpack.error.ClientErrorHandler} in all contexts.
   * A {@link ratpack.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param statusCode The 4xx range status code that indicates the error type
   * @throws NotInRegistryException if no {@link ratpack.error.ClientErrorHandler} can be found in the service
   */
  @NonBlocking
  void clientError(int statusCode) throws NotInRegistryException;

  /**
   * Executes the given runnable in a try/catch, where exceptions are given to {@link #error(Exception)}.
   * <p>
   * This can be used by handlers when they are jumping off thread.
   * Exceptions raised on the thread that called the handler's {@linkplain Handler#handle(Context) handle} will always be caught.
   * If the handler “moves” to another thread, it should call this method no the new thread to ensure that any thrown exceptions
   * are caught and forwarded appropriately.
   *
   * @param runnable The code to surround with error handling
   */
  void withErrorHandling(Runnable runnable);

  /**
   * Creates a result action that uses the contextual error handler if the result is failure.
   * <p>
   * The given action is invoked if the result is successful.
   *
   * @param action The action to invoke on a successful result.
   * @param <T> The type of the successful result value
   * @return An action that takes {@code Result<T>}
   */
  <T> ResultAction<T> resultAction(Action<T> action);

  /**
   * The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * <p>
   * Shorthand for {@code get(PathBinding.class).getPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link ratpack.path.PathBinding} in the current service
   */
  PathTokens getPathTokens() throws NotInRegistryException;

  /**
   * The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * <p>
   * Shorthand for {@code get(PathBinding.class).getAllPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link ratpack.path.PathBinding} in the current service
   */
  PathTokens getAllPathTokens() throws NotInRegistryException;

  /**
   * Gets the file relative to the contextual {@link ratpack.file.FileSystemBinding}.
   * <p>
   * Shorthand for {@code get(FileSystemBinding.class).file(path)}.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.file.FileSystemBinding} in all contexts.
   * A {@link ratpack.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   *
   * @param path The path to pass to the {@link ratpack.file.FileSystemBinding#file(String)} method.
   * @return The file relative to the contextual {@link ratpack.file.FileSystemBinding}
   * @throws NotInRegistryException if there is no {@link ratpack.file.FileSystemBinding} in the current service
   */
  Path file(String path) throws NotInRegistryException;

  /**
   * Render the given object, using the rendering framework.
   * <p>
   * The first {@link ratpack.render.Renderer}, that is able to render the given object will be delegated to.
   * If the given argument is {@code null}, this method will have the same effect as {@link #clientError(int) clientError(404)}.
   * <p>
   * Ratpack has built in support for rendering the following types:
   * <ul>
   * <li>{@link java.nio.file.Path} (see {@link ratpack.file.FileRenderer})</li>
   * <li>{@link java.lang.CharSequence} (see {@link ratpack.render.CharSequenceRenderer})</li>
   * </ul>
   * <p>
   * See {@link ratpack.render.Renderer} for more on how to contribute to the rendering framework.
   *
   * @param object The object to render
   * @throws NoSuchRendererException If there is no suitable renderer for the object
   */
  @NonBlocking
  void render(Object object) throws NoSuchRendererException;

  /**
   * An object to be used when executing blocking IO, or long operations.
   *
   * @return An object to be used when executing blocking IO, or long operations.
   * @see #background(java.util.concurrent.Callable)
   */
  Background getBackground();

  /**
   * Perform a blocking operation, off the request thread.
   * <p>
   * Ratpack apps typically do not use a large thread pool for handling requests. By default there is about one thread per core.
   * This means that blocking IO operations cannot be done on the thread invokes a handler. Background IO operations must be
   * offloaded in order to free the request handling thread to handle other requests while the IO operation is performed.
   * The {@code Background} object makes it easy to do this.
   * <p>
   * A callable is submitted to the {@link ratpack.background.Background#exec(Callable)} method. The implementation of this callable <b>can</b> background
   * as it will be executed on a non request handling thread. It should do not much more than initiate a blocking IO operation and return the result.
   * <p>
   * However, the callable is not executed immediately. The return value of {@link ratpack.background.Background#exec(Callable)} must be used to specify
   * how to proceed after the blocking operation. The {@code then()} method must be called for the work to be performed.
   * </p>
   * Example usage (Java):
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.util.Action;
   * import java.util.concurrent.Callable;
   *
   * class MyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.background(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "foo";
   *        }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.getResponse().send(result);
   *       }
   *     });
   *   }
   * }
   * </pre>
   *
   * <h4>Error Handling</h4>
   * <p>
   * Unless otherwise specified, any exceptions that are raised during the blocking operation callable are forwarded
   * to the {@link ratpack.handling.Context#error(Exception)} method of the current context.
   * Similarly, errors that occur during the result handler are forwarded.
   * </p>
   * <p>
   * To use a custom error handling strategy, use the {@link ratpack.background.Background.SuccessOrError#onError(Action)} method
   * of the return of {@link ratpack.background.Background#exec(Callable)}.
   * </p>
   * <p>
   * Example usage:
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.util.Action;
   * import java.util.concurrent.Callable;
   *
   * class MyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.background(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "foo";
   *        }
   *     }).onError(new Action&lt;Exception&gt;() {
   *       public void execute(Exception exception) {
   *         // do something with the exception
   *       }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.getResponse().send(result);
   *       }
   *     });
   *   }
   * }
   * </pre>
   *
   * @param backgroundOperation The blocking operation to perform off of the request thread
   * @param <T> The type of object returned by the background operation
   * @return A builder for specifying the result handling strategy for a blocking operation.
   * @see #getBackground()
   */
  <T> Background.SuccessOrError<T> background(Callable<T> backgroundOperation);

  /**
   * Returns the executor that managed foreground (i.e. request handling) threads.
   * <p>
   * Useful for deferring computation work.
   *
   * @return the executor that managed foreground (i.e. request handling) threads.
   */
  ScheduledExecutorService getForegroundExecutorService();

  /**
   * Sends a temporary redirect response (i.e. statusCode 302) to the client using the specified redirect location URL.
   *
   * @param location the redirect location URL
   * @throws NotInRegistryException if there is no {@link Redirector} in the current service but one is provided by default
   */
  void redirect(String location) throws NotInRegistryException;

  /**
   * Sends a redirect response location URL and status code (which should be in the 3xx range).
   *
   * @param code The status code of the redirect
   * @param location the redirect location URL
   * @throws NotInRegistryException if there is no {@link Redirector} in the current service but one is provided by default
   */
  void redirect(int code, String location) throws NotInRegistryException;

  /**
   * Convenience method for handling last-modified based HTTP caching.
   * <p>
   * The given date is the "last modified" value of the response.
   * If the client sent an "If-Modified-Since" header that is of equal or greater value than date, a 304
   * will be returned to the client. Otherwise, the given runnable will be executed (it should send a response)
   * and the "Last-Modified" header will be set by this method.
   *
   * @param date The effective last modified date of the response
   * @param runnable The response sending action if the response needs to be sent
   */
  @NonBlocking
  void lastModified(Date date, Runnable runnable);

  /**
   * The address that this request was received on.
   *
   * @return The address that this request was received on.
   */
  BindAddress getBindAddress();

  /**
   * Parses the request body into an object.
   * <p>
   * How to parse the request is controlled by the given {@link Parse} object.
   * <h5>Parser Resolution</h5>
   * <p>
   * Parser resolution happens as follows:
   * <ol>
   * <li>All {@link ratpack.parse.Parser parsers} are retrieved from the context registry (i.e. {@link #getAll(Class) getAll(Parser.class)});</li>
   * <li>Found parsers are checked (in order returned by {@code getAll()}) for compatibility with the given {@code parse} object and the current request content type;</li>
   * <li>If a parser is found that is compatible, its {@link ratpack.parse.Parser#parse(Context, ratpack.http.TypedData, ratpack.parse.Parse)} method is called, of which the return value is returned by this method;</li>
   * <li>If no compatible parser could be found, a {@link NoSuchParserException} will be thrown.</li>
   * </ol>
   * <h5>Parser Compatibility</h5>
   * <p>
   * A parser is compatible if all of the following hold true:
   * <ul>
   * <li>Its {@link ratpack.parse.Parser#getContentType()} is exactly equal to {@link ratpack.http.MediaType#getType() getRequest().getBody().getContentType().getType()}</li>
   * <li>The given {@code parse} object is an {@code instanceof} its {@link ratpack.parse.Parser#getParseType()}</li>
   * <li>The {@link ratpack.parse.Parser#getParsedType()} is {@link Class#isAssignableFrom(Class)} the {@link Parse#getType()} of the {@code parse} object</li>
   * </ul>
   * <p>
   * If the request has no declared content type, {@code text/plain} will be assumed.
   * <h5>Core Parsers</h5>
   * <p>
   * Ratpack core provides implicit parsers for the following parse and content types:
   * <ul>
   * <li>{@link ratpack.form.Form}</li>
   * <ul>
   *   <li>multipart/form-data</li>
   *   <li>application/x-www-form-urlencoded</li>
   * </ul>
   * </ul>
   * <h5>Example Usage</h5>
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.form.Form;
   * import static ratpack.parse.NoOptParse.to;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     Form form = context.parse(to(Form.class));
   *     context.render(form.get("someFormParam"));
   *   }
   * }
   * </pre>
   * @param parse The specification of how to parse the request
   * @param <T> The type of object the request is parsed into
   * @return The parsed object
   * @throws NoSuchParserException if no suitable parser could be found in the registry
   * @throws ParserException if a suitable parser was found, but it threw an exception while parsing
   * @see ratpack.parse.Parser
   */
  <T> T parse(Parse<T> parse) throws NoSuchParserException, ParserException;

  /**
   * Shorthand for no option parses.
   * <p>
   * The code sample is functionally identical to the sample given for the {@link #parse(ratpack.parse.Parse)} variant…
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.form.Form;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     Form form = context.parse(Form.class);
   *     context.render(form.get("someFormParam"));
   *   }
   * }
   * </pre>
   * <p>
   * That is, it is effectively a convenient wrapper around {@link ratpack.parse.NoOptParse#to(Class)}.
   *
   * @param type the type to parse to
   * @param <T> the type to parse to
   * @return The parsed object
   * @throws NoSuchParserException if no suitable parser could be found in the registry
   * @throws ParserException if a suitable parser was found, but it threw an exception while parsing
   */
  <T> T parse(Class<T> type) throws NoSuchParserException, ParserException;

  /**
   * Registers a callback to be notified when the request for this context is “closed” (i.e. responded to).
   *
   * @param onClose A notification callback
   */
  void onClose(Action<? super RequestOutcome> onClose);

  /**
   * Provides direct access to the backing Netty channel.
   * <p>
   * General only useful for low level extensions. Avoid if possible.
   *
   * @return Direct access to the underlying channel.
   */
  DirectChannelAccess getDirectChannelAccess();

}
