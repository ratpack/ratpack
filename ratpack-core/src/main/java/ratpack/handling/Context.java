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

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import ratpack.api.NonBlocking;
import ratpack.api.Nullable;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.launch.LaunchConfig;
import ratpack.parse.NoSuchParserException;
import ratpack.parse.Parse;
import ratpack.parse.ParserException;
import ratpack.path.PathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.server.BindAddress;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

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
public interface Context extends ExecContext, Registry {

  /**
   * Returns this.
   *
   * @return this.
   */
  Context getContext();


  /**
   * {@inheritDoc}
   */
  @Override
  Supplier getSupplier();

  LaunchConfig getLaunchConfig();

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
   * import ratpack.func.Factory;
   *
   * import static ratpack.registry.Registries.just;
   *
   * public interface SomeThing {}
   * public class SomeThingImpl implements SomeThing {}
   *
   * public class UpstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     context.next(just(SomeThing.class, new SomeThingImpl()));
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
   *       protected void execute() {
   *         handler(new UpstreamHandler());
   *         handler(new DownstreamHandler());
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
   * A {@link NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param exception The exception that occurred
   * @throws NotInRegistryException if no {@link ratpack.error.ServerErrorHandler} can be found in the service
   */
  @Override
  @NonBlocking
  void error(Exception exception);

  /**
   * Executes a blocking operation, returning a promise for its result.
   * <p>
   * This method executes asynchronously, in that it does not invoke the {@code operation} before returning the promise.
   * When the returned promise is subscribed to (i.e. its {@link ratpack.exec.SuccessPromise#then(Action)} method is called),
   * the given {@code operation} will be submitted to a thread pool that is different to the request handling thread pool.
   * Therefore, if the returned promise is never subscribed to, the {@code operation} will never be initiated.
   * <p>
   * The promise returned by this method, has the same default error handling strategy as those returned by {@link ratpack.exec.ExecControl#promise(ratpack.func.Action)}.
   * <p>
   * <pre class="tested">
   * import ratpack.handling.*;
   * import ratpack.func.Action;

   * import java.util.concurrent.Callable;
   *
   * public class BlockingJavaHandler implements Handler {
   *   void handle(final Context context) {
   *     context.blocking(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "hello world!";
   *        }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.render(result);
   *       }
   *     });
   *   }
   * }
   *
   * public class BlockingGroovyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.blocking {
   *       "hello world!"
   *     } then { String result ->
   *       context.render(result)
   *     }
   *   }
   * }
   *
   * // Test (Groovy) &hellip;
   *
   * import static ratpack.groovy.test.TestHttpClients.testHttpClient
   * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
   *
   * def app = embeddedApp {
   *   handlers {
   *     get("java", new BlockingJavaHandler())
   *     get("groovy", new BlockingGroovyHandler())
   *   }
   * }
   *
   * def client = testHttpClient(app)
   *
   * assert client.getText("java") == "hello world!"
   * assert client.getText("groovy") == "hello world!"
   *
   * app.close()
   * </pre>
   *
   * @param blockingOperation The operation to perform
   * @param <T> The type of result object that the operation produces
   * @return a promise for the return value of the callable.
   */
  @Override
  <T> Promise<T> blocking(Callable<T> blockingOperation);

  /**
   * Creates a promise of a value that will made available asynchronously.
   * <p>
   * The {@code action} given to this method receives a {@link Fulfiller}, which can be used to fulfill the promise at any time in the future.
   * The {@code action} is not required to fulfill the promise during the execution of the {@code execute()} method (i.e. it can be asynchronous).
   * The {@code action} MUST call one of the fulfillment methods.
   * Otherwise, the promise will go unfulfilled.
   * There is no time limit or timeout on fulfillment.
   * <p>
   * The promise returned has a default error handling strategy of forwarding exceptions to {@link #error(Exception)} of this context.
   * To use a different error strategy, supply it to the {@link ratpack.exec.Promise#onError(Action)} method.
   * <p>
   * The promise will always be fulfilled on a thread managed by Ratpack.
   * <pre class="tested">
   * import ratpack.handling.*;
   * import ratpack.exec.Fulfiller;
   * import ratpack.func.Action;
   *
   * public class PromiseUsingJavaHandler implements Handler {
   *   public void handle(final Context context) {
   *     context.promise(new Action&lt;Fulfiller&lt;String&gt;&gt;() {
   *       public void execute(final Fulfiller&lt;String&gt; fulfiller) {
   *         new Thread(new Runnable() {
   *           public void run() {
   *             fulfiller.success("hello world!");
   *           }
   *         }).start();
   *       }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String string) {
   *         context.render(string);
   *       }
   *     });
   *   }
   * }
   *
   * class PromiseUsingGroovyHandler implements Handler {
   *   void handle(Context context) {
   *     context.promise { Fulfiller&lt;String&gt; fulfiller ->
   *       Thread.start {
   *         fulfiller.success("hello world!")
   *       }
   *     } then { String string ->
   *       context.render(string)
   *     }
   *   }
   * }
   *
   * // Test (Groovy) &hellip;
   *
   * import static ratpack.groovy.test.TestHttpClients.testHttpClient
   * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
   *
   * def app = embeddedApp {
   *   handlers {
   *     get("java", new PromiseUsingJavaHandler())
   *     get("groovy", new PromiseUsingGroovyHandler())
   *   }
   * }
   *
   * def client = testHttpClient(app)
   *
   * assert client.getText("java") == "hello world!"
   * assert client.getText("groovy") == "hello world!"
   *
   * app.close()
   * </pre>
   * @param <T> the type of value promised
   */
  @Override
  <T> Promise<T> promise(Action<? super Fulfiller<T>> action);

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
   * Render the given object, using the rendering framework.
   * <p>
   * The first {@link ratpack.render.Renderer}, that is able to render the given object will be delegated to.
   * If the given argument is {@code null}, this method will have the same effect as {@link #clientError(int) clientError(404)}.
   * <p>
   * If no renderer can be found for the given type, a {@link NoSuchRendererException} will be given to {@link #error(Exception)}.
   * <p>
   * If a renderer throws an exception during its execution it will be wrapped in a {@link ratpack.render.RendererException} and given to {@link #error(Exception)}.
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
   * @throws NoSuchRendererException if no suitable renderer can be found
   */
  @NonBlocking
  void render(Object object) throws NoSuchRendererException;

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
   * Parse the request into the given type, using no options (or more specifically an instance of {@link ratpack.parse.NullParseOpts} as the options).
   * <p>
   * The code sample is functionally identical to the sample given for the {@link #parse(Parse)} variant…
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
   * That is, it is a convenient form of {@code parse(Parse.of(T))}.
   *
   * @param type the type to parse to
   * @param <T> the type to parse to
   * @return The parsed object
   * @throws NoSuchParserException if no suitable parser could be found in the registry
   * @throws ParserException if a suitable parser was found, but it threw an exception while parsing
   */
  <T> T parse(Class<T> type) throws NoSuchParserException, ParserException;

  /**
   * Parse the request into the given type, using no options (or more specifically an instance of {@link ratpack.parse.NullParseOpts} as the options).
   * <p>
   * The code sample is functionally identical to the sample given for the {@link #parse(Parse)} variant…
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.form.Form;
   * import com.google.common.reflect.TypeToken;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     Form form = context.parse(new TypeToken&lt;Form&gt;() {});
   *     context.render(form.get("someFormParam"));
   *   }
   * }
   * </pre>
   * <p>
   * That is, it is a convenient form of {@code parse(Parse.of(T))}.
   *
   * @param type the type to parse to
   * @param <T> the type to parse to
   * @return The parsed object
   * @throws NoSuchParserException if no suitable parser could be found in the registry
   * @throws ParserException if a suitable parser was found, but it threw an exception while parsing
   */
  <T> T parse(TypeToken<T> type) throws NoSuchParserException, ParserException;

  /**
   * Constructs a {@link Parse} from the given args and delegates to {@link #parse(Parse)}.
   *
   * @param type The type to parse to
   * @param options The parse options
   * @param <T> The type to parse to
   * @param <O> The type of the parse opts
   * @return The parsed object
   * @throws NoSuchParserException if no suitable parser could be found in the registry
   * @throws ParserException if a suitable parser was found, but it threw an exception while parsing
   */
  <T, O> T parse(Class<T> type, O options) throws NoSuchParserException, ParserException;

  /**
   * Constructs a {@link Parse} from the given args and delegates to {@link #parse(Parse)}.
   *
   * @param type The type to parse to
   * @param options The parse options
   * @param <T> The type to parse to
   * @param <O> The type of the parse opts
   * @return The parsed object
   * @throws NoSuchParserException if no suitable parser could be found in the registry
   * @throws ParserException if a suitable parser was found, but it threw an exception while parsing
   */
  <T, O> T parse(TypeToken<T> type, O options) throws NoSuchParserException, ParserException;

  /**
   * Parses the request body into an object.
   * <p>
   * How to parse the request is determined by the given {@link Parse} object.
   * <h5>Parser Resolution</h5>
   * <p>
   * Parser resolution happens as follows:
   * <ol>
   * <li>All {@link ratpack.parse.Parser parsers} are retrieved from the context registry (i.e. {@link #getAll(Class) getAll(Parser.class)});</li>
   * <li>Found parsers are checked (in order returned by {@code getAll()}) for compatibility with the current request content type and options type;</li>
   * <li>If a parser is found that is compatible, its {@link ratpack.parse.Parser#parse(Context, ratpack.http.TypedData, Parse)} method is called;</li>
   * <li>If the parser returns {@code null} the next parser will be tried, if it returns a value it will be returned by this method;</li>
   * <li>If no compatible parser could be found, a {@link NoSuchParserException} will be thrown.</li>
   * </ol>
   * <h5>Parser Compatibility</h5>
   * <p>
   * A parser is compatible if all of the following hold true:
   * <ul>
   * <li>Its {@link ratpack.parse.Parser#getContentType()} is exactly equal to {@link ratpack.http.MediaType#getType() getRequest().getBody().getContentType().getType()}</li>
   * <li>The opts of the given {@code parse} object is an {@code instanceof} its {@link ratpack.parse.Parser#getOptsType()} ()}</li>
   * <li>The {@link ratpack.parse.Parser#parse(Context, ratpack.http.TypedData, Parse)} method returns a non null value.</li>
   * </ul>
   * <p>
   * If the request has no declared content type, {@code text/plain} will be assumed.
   * <h5>Core Parsers</h5>
   * <p>
   * Ratpack core provides implicit {@link ratpack.parse.NoOptParserSupport no opt parsers} for the following types and content types:
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
   * import ratpack.parse.Parse;
   * import ratpack.parse.NullParseOpts;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     Form form = context.parse(Parse.of(Form.class));
   *     context.render(form.get("someFormParam"));
   *   }
   * }
   * </pre>
   * @param parse The specification of how to parse the request
   * @param <T> The type of object the request is parsed into
   * @param <O> the type of the parse options object
   * @return The parsed object
   * @throws NoSuchParserException if no suitable parser could be found in the registry
   * @throws ParserException if a suitable parser was found, but it threw an exception while parsing
   * @see #parse(Class)
   * @see #parse(Class, Object)
   * @see ratpack.parse.Parser
   */
  <T, O> T parse(Parse<T, O> parse) throws NoSuchParserException, ParserException;

  /**
   * Provides direct access to the backing Netty channel.
   * <p>
   * General only useful for low level extensions. Avoid if possible.
   *
   * @return Direct access to the underlying channel.
   */
  DirectChannelAccess getDirectChannelAccess();

  /**
   * The address that this request was received on.
   *
   * @return The address that this request was received on.
   */
  BindAddress getBindAddress();

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
   * Registers a callback to be notified when the request for this context is “closed” (i.e. responded to).
   *
   * @param onClose A notification callback
   */
  void onClose(Action<? super RequestOutcome> onClose);

  /**
   * Gets the file relative to the contextual {@link ratpack.file.FileSystemBinding}.
   * <p>
   * Shorthand for {@code get(FileSystemBinding.class).file(path)}.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.file.FileSystemBinding} in all contexts.
   * A {@link NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param path The path to pass to the {@link ratpack.file.FileSystemBinding#file(String)} method.
   * @return The file relative to the contextual {@link ratpack.file.FileSystemBinding}
   * @throws NotInRegistryException if there is no {@link ratpack.file.FileSystemBinding} in the current service
   */
  Path file(String path) throws NotInRegistryException;

  /**
   * {@inheritDoc}
   */
  @Override
  ExecController getExecController();

  /**
   * {@inheritDoc}
   */
  @Override
  List<ExecInterceptor> getInterceptors();

  /**
   * {@inheritDoc}
   */
  @Override
  <O> O get(Class<O> type) throws NotInRegistryException;

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  <O> O maybeGet(Class<O> type);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Iterable<? extends O> getAll(Class<O> type);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> O get(TypeToken<O> type) throws NotInRegistryException;

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  <O> O maybeGet(TypeToken<O> type);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Iterable<? extends O> getAll(TypeToken<O> type);

  void addExecInterceptor(ExecInterceptor execInterceptor, Action<? super Context> action) throws Exception;

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  <T> T first(TypeToken<T> type, Predicate<? super T> predicate);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception;
}
