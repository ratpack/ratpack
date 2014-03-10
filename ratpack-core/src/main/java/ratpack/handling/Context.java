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
import ratpack.func.Action;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Response;
import ratpack.parse.NoSuchParserException;
import ratpack.parse.Parse;
import ratpack.parse.ParserException;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.util.ResultAction;

import java.util.Date;

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
public interface Context extends ReadOnlyContext {

  /**
   * Returns this.
   *
   * @return this.
   */
  Context getContext();

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
   * import static ratpack.registry.Registries.registry;
   *
   * public interface SomeThing {}
   * public class SomeThingImpl implements SomeThing {}
   *
   * public class UpstreamHandler implements Handler {
   *   public void handle(Context context) {
   *     context.next(registry(SomeThing.class, new SomeThingImpl()));
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
   * Render the given object, using the rendering framework.
   * <p>
   * The first {@link ratpack.render.Renderer}, that is able to render the given object will be delegated to.
   * If the given argument is {@code null}, this method will have the same effect as {@link #clientError(int) clientError(404)}.
   * <p>
   * If no renderer can be found for the given type, a {@link ratpack.render.NoSuchRendererException} will be given to {@link #error(Exception)}.
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
   */
  @NonBlocking
  void render(Object object);

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
   * Provides direct access to the backing Netty channel.
   * <p>
   * General only useful for low level extensions. Avoid if possible.
   *
   * @return Direct access to the underlying channel.
   */
  DirectChannelAccess getDirectChannelAccess();

}
