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

import com.google.common.reflect.TypeToken;
import org.reactivestreams.Publisher;
import ratpack.api.NonBlocking;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.TypedData;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.path.PathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.server.ServerConfig;
import ratpack.util.Types;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
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
 *
 * <h3>Contextual objects</h3>
 * <p>
 * A context is also a {@link Registry} of objects.
 * Arbitrary objects can be "pushed" into the context for use by <i>downstream</i> handlers.
 * <p>
 * There are some significant contextual objects that drive key infrastructure.
 * For example, error handling is based on informing the contextual {@link ratpack.error.ServerErrorHandler} of exceptions.
 * The error handling strategy for an application can be changed by pushing a new implementation of this interface into the context that is used downstream.
 * <p>
 * See {@link #insert(Handler...)} for more on how to do this.
 * <h4>Default contextual objects</h4>
 * <p>There is also a set of default objects that are made available via the Ratpack infrastructure:
 * <ul>
 * <li>A {@link ratpack.file.FileSystemBinding} that is the application {@link ratpack.server.ServerConfig#getBaseDir()}</li>
 * <li>A {@link ratpack.file.MimeTypes} implementation</li>
 * <li>A {@link ratpack.error.ServerErrorHandler}</li>
 * <li>A {@link ratpack.error.ClientErrorHandler}</li>
 * <li>A {@link ratpack.server.PublicAddress}</li>
 * <li>A {@link Redirector}</li>
 * </ul>
 */
public interface Context extends Registry {

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<Context> TYPE = Types.token(Context.class);

  /**
   * Returns this.
   *
   * @return this.
   */
  Context getContext();

  /**
   * The execution of handling this request.
   *
   * @return the execution of handling this request
   */
  Execution getExecution();

  /**
   * The server configuration for the application.
   * @return the server configuration for the application
   */
  ServerConfig getServerConfig();

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
   * <pre class="java">{@code
   * import ratpack.registry.Registry;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.fromHandlers(chain -> chain
   *         .all(ctx -> ctx.next(Registry.single("foo")))
   *         .all(ctx -> ctx.render(ctx.get(String.class)))
   *     ).test(httpClient -> {
   *       assertEquals("foo", httpClient.getText());
   *     });
   *   }
   * }
   * }</pre>
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
   * Respond to the request based on the request method.
   *
   * <pre class="java">{@code
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     EmbeddedApp.fromHandlers(chain -> chain
   *       .path("a", ctx -> {
   *         String val = "a";
   *         ctx.byMethod(m -> m
   *           .get(() -> ctx.render(val + " - " + "GET"))
   *           .post(() -> ctx.render(val + " - " + "POST"))
   *         );
   *       })
   *       .path("b", ctx -> {
   *         String val = "b";
   *         ctx.byMethod(m -> m
   *           .get(() -> ctx.render(val + " - " + "GET"))
   *           .post(() -> ctx.render(val + " - " + "POST"))
   *         );
   *       })
   *     ).test(httpClient -> {
   *       assertEquals("a - GET", httpClient.getText("a"));
   *       assertEquals("a - POST", httpClient.postText("a"));
   *       assertEquals("b - GET", httpClient.getText("b"));
   *       assertEquals("b - POST", httpClient.postText("b"));
   *     });
   *   }
   * }
   * }</pre>
   *
   * <p>
   * Only the last added handler for a method will be used.
   * Adding a subsequent handler for the same method will replace the previous.
   * </p>
   * <p>
   * If no handler has been registered for the actual request method, a {@code 405} will be issued by {@link #clientError(int)}.
   * <p>
   * If the handler only needs to respond to one HTTP method it can be more convenient to use {@link Chain#get(Handler)} and friends.
   *
   * @param action the specification of how to handle the request based on the request method
   * @throws Exception any thrown by action
   */
  @NonBlocking
  void byMethod(Action<? super ByMethodSpec> action) throws Exception;

  /**
   * Respond to the request based on the requested content type (i.e. the request Accept header).
   * <p>
   * This is useful when a given handler can provide content of more than one type (i.e. <a href="http://en.wikipedia.org/wiki/Content_negotiation">content negotiation</a>).
   * <p>
   * The handler to use will be selected based on parsing the "Accept" header, respecting quality weighting and wildcard matching.
   * The order that types are specified is significant for wildcard matching.
   * The earliest registered type that matches the wildcard will be used.
   *
   * <pre class="java">{@code
   * import ratpack.test.embed.EmbeddedApp;
   * import ratpack.http.client.ReceivedResponse;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     EmbeddedApp.fromHandler(ctx -> {
   *       String message = "hello!";
   *       ctx.byContent(m -> m
   *         .json(() -> ctx.render("{\"msg\": \"" + message + "\"}"))
   *         .html(() -> ctx.render("<p>" + message + "</p>"))
   *       );
   *     }).test(httpClient -> {
   *       ReceivedResponse response = httpClient.requestSpec(s -> s.getHeaders().add("Accept", "application/json")).get();
   *       assertEquals("{\"msg\": \"hello!\"}", response.getBody().getText());
   *       assertEquals("application/json", response.getBody().getContentType().getType());
   *
   *       response = httpClient.requestSpec(s -> s.getHeaders().add("Accept", "text/plain; q=1.0, text/html; q=0.8, application/json; q=0.7")).get();
   *       assertEquals("<p>hello!</p>", response.getBody().getText());
   *       assertEquals("text/html", response.getBody().getContentType().getType());
   *     });
   *   }
   * }
   * }</pre>
   * If there is no type registered, or if the client does not accept any of the given types, by default a {@code 406} will be issued with {@link Context#clientError(int)}.
   * If you want a different behavior, use {@link ratpack.handling.ByContentSpec#noMatch}.
   * <p>
   * Only the last specified handler for a type will be used.
   * That is, adding a subsequent handler for the same type will replace the previous.
   *
   * @param action the specification of how to handle the request based on the clients preference of content type
   * @throws Exception any thrown by action
   */
  void byContent(Action<? super ByContentSpec> action) throws Exception;

  // Shorthands for common service lookups

  /**
   * Forwards the exception to the {@link ratpack.error.ServerErrorHandler} in this service.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.error.ServerErrorHandler} in all contexts.
   * A {@link NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param throwable The exception that occurred
   * @throws NotInRegistryException if no {@link ratpack.error.ServerErrorHandler} can be found in the service
   */
  @NonBlocking
  void error(Throwable throwable);

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
   * If no renderer can be found for the given type, a {@link NoSuchRendererException} will be given to {@link #error(Throwable)}.
   * <p>
   * If a renderer throws an exception during its execution it will be wrapped in a {@link ratpack.render.RendererException} and given to {@link #error(Throwable)}.
   * <p>
   * Ratpack has built in support for rendering the following types:
   * <ul>
   * <li>{@link java.nio.file.Path}</li>
   * <li>{@link java.lang.CharSequence}</li>
   * <li>{@link ratpack.jackson.JsonRender} (Typically created via {@link ratpack.jackson.Jackson#json(Object)})</li>
   * <li>{@link Promise} (renders the promised value, using this {@code render()} method)</li>
   * <li>{@link org.reactivestreams.Publisher} (converts the publisher to a promise using {@link ratpack.stream.Streams#toPromise(Publisher)} and renders it)</li>
   * <li>{@link ratpack.render.Renderable} (Delegates to the {@link ratpack.render.Renderable#render(Context)} method of the object)</li>
   * </ul>
   * <p>
   * See {@link ratpack.render.Renderer} for more on how to contribute to the rendering framework.
   * <p>
   * The object-to-render will be decorated by all registered {@link ratpack.render.RenderableDecorator} whose type is exactly equal to the type of the object-to-render, before being passed to the selected renderer.
   *
   * @param object The object-to-render
   * @throws NoSuchRendererException if no suitable renderer can be found
   */
  @NonBlocking
  void render(Object object) throws NoSuchRendererException;

  /**
   * Sends a temporary redirect response (i.e. 302) to the client using the specified redirect location.
   * <p>
   * This method is effectively deprecated and will be removed in Ratpack 2.0.
   * Note, this method simply delegates to {@link #redirect(Object)} which is the replacement.
   * It is not formally marked as deprecated as the replacement is source compatible, and a generalising overload of this method.
   *
   * @param to the location to redirect to
   * @see #redirect(Object)
   */
  default void redirect(String to) {
    redirect((Object) to);
  }

  /**
   * Sends a redirect response to the given location, and with the given status code.
   * <p>
   * This method is effectively deprecated and will be removed in Ratpack 2.0.
   * Note, this method simply delegates to {@link #redirect(int, Object)} which is the replacement.
   * It is not formally marked as deprecated as the replacement is source compatible, and a generalising overload of this method.
   *
   * @param code the redirect response status code
   * @param to the location to redirect to
   * @see #redirect(int, Object)
   */
  default void redirect(int code, String to) {
    redirect(code, (Object) to);
  }

  /**
   * Sends a temporary redirect response (i.e. 302) to the client using the specified redirect location.
   * <p>
   * This method is effectively deprecated and will be removed in Ratpack 2.0.
   * Note, this method simply delegates to {@link #redirect(Object)} which is the replacement.
   * It is not formally marked as deprecated as the replacement is source compatible, and a generalising overload of this method.
   *
   * @param to the location to redirect to
   * @see #redirect(Object)
   * @since 1.2
   */
  default void redirect(URI to) {
    redirect((Object) to);
  }

  /**
   * Sends a redirect response to the given location, and with the given status code.
   * <p>
   * This method is effectively deprecated and will be removed in Ratpack 2.0.
   * Note, this method simply delegates to {@link #redirect(int, Object)} which is the replacement.
   * It is not formally marked as deprecated as the replacement is source compatible, and a generalising overload of this method.
   *
   * @param code the redirect response status code
   * @param to the location to redirect to
   * @see #redirect(int, Object)
   */
  default void redirect(int code, URI to) {
    redirect(code, to.toASCIIString());
  }

  /**
   * Sends a temporary redirect response (i.e. 302) to the client using the specified redirect location.
   *
   * @param to the location to redirect to
   * @see #redirect(int, Object)
   * @since 1.3
   */
  void redirect(Object to);

  /**
   * Sends a redirect response to the given location, and with the given status code.
   * <p>
   * This method retrieves the {@link Redirector} from the registry, and forwards the given arguments along with {@code this} context.
   *
   * @param code The status code of the redirect
   * @param to the redirect location URL
   * @see Redirector
   * @since 1.3
   */
  void redirect(int code, Object to);

  /**
   * Convenience method for handling last-modified based HTTP caching.
   * <p>
   * The given date is the "last modified" value of the response.
   * If the client sent an "If-Modified-Since" header that is of equal or greater value than date,
   * a 304 will be returned to the client.
   * Otherwise, the given runnable will be executed (it should send a response)
   * and the "Last-Modified" header will be set by this method.
   *
   * @param lastModified the effective last modified date of the response
   * @param serve the response sending action if the response needs to be sent
   */
  @NonBlocking
  default void lastModified(Date lastModified, Runnable serve) {
    lastModified(lastModified.toInstant(), serve);
  }

  /**
   * Convenience method for handling last-modified based HTTP caching.
   * <p>
   * The given date is the "last modified" value of the response.
   * If the client sent an "If-Modified-Since" header that is of equal or greater value than date,
   * a 304 will be returned to the client.
   * Otherwise, the given runnable will be executed (it should send a response)
   * and the "Last-Modified" header will be set by this method.
   *
   * @param lastModified the effective last modified date of the response
   * @param serve the response sending action if the response needs to be sent
   * @since 1.4
   */
  @NonBlocking
  void lastModified(Instant lastModified, Runnable serve);

  /**
   * Parse the request into the given type, using no options (or more specifically an instance of {@link ratpack.parse.NullParseOpts} as the options).
   * <p>
   * The code sample is functionally identical to the sample given for the {@link #parse(Parse)} variant…
   * <pre class="java">{@code
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.form.Form;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     context.parse(Form.class).then(form -> context.render(form.get("someFormParam")));
   *   }
   * }
   * }</pre>
   * <p>
   * That is, it is a convenient form of {@code parse(Parse.of(T))}.
   *
   * @param type the type to parse to
   * @param <T> the type to parse to
   * @return a promise for the parsed object
   */
  <T> Promise<T> parse(Class<T> type);

  /**
   * Parse the request into the given type, using no options (or more specifically an instance of {@link ratpack.parse.NullParseOpts} as the options).
   * <p>
   * The code sample is functionally identical to the sample given for the {@link #parse(Parse)} variant&hellip;
   * <pre class="java">{@code
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.form.Form;
   * import com.google.common.reflect.TypeToken;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     context.parse(new TypeToken<Form>() {}).then(form -> context.render(form.get("someFormParam")));
   *   }
   * }
   * }</pre>
   * <p>
   * That is, it is a convenient form of {@code parse(Parse.of(T))}.
   *
   * @param type the type to parse to
   * @param <T> the type to parse to
   * @return a promise for the parsed object
   */
  <T> Promise<T> parse(TypeToken<T> type);

  /**
   * Constructs a {@link Parse} from the given args and delegates to {@link #parse(Parse)}.
   *
   * @param type The type to parse to
   * @param options The parse options
   * @param <T> The type to parse to
   * @param <O> The type of the parse opts
   * @return a promise for the parsed object
   */
  <T, O> Promise<T> parse(Class<T> type, O options);

  /**
   * Constructs a {@link Parse} from the given args and delegates to {@link #parse(Parse)}.
   *
   * @param type The type to parse to
   * @param options The parse options
   * @param <T> The type to parse to
   * @param <O> The type of the parse opts
   * @return a promise for the parsed object
   */
  <T, O> Promise<T> parse(TypeToken<T> type, O options);

  /**
   * Parses the request body into an object.
   * <p>
   * How to parse the request is determined by the given {@link Parse} object.
   *
   * <h3>Parser Resolution</h3>
   * <p>
   * Parser resolution happens as follows:
   * <ol>
   * <li>All {@link ratpack.parse.Parser parsers} are retrieved from the context registry (i.e. {@link #getAll(Class) getAll(Parser.class)});</li>
   * <li>Found parsers are checked (in order returned by {@code getAll()}) for compatibility with the options type;</li>
   * <li>If a parser is found that is compatible, its {@link ratpack.parse.Parser#parse(Context, ratpack.http.TypedData, Parse)} method is called;</li>
   * <li>If the parser returns {@code null} the next parser will be tried, if it returns a value it will be returned by this method;</li>
   * <li>If no compatible parser could be found, a {@link ratpack.parse.NoSuchParserException} will be thrown.</li>
   * </ol>
   *
   * <h3>Parser Compatibility</h3>
   * <p>
   * A parser is compatible if all of the following hold true:
   * <ul>
   * <li>The opts of the given {@code parse} object is an {@code instanceof} its {@link ratpack.parse.Parser#getOptsType()}, or the opts are {@code null}.</li>
   * <li>The {@link ratpack.parse.Parser#parse(Context, ratpack.http.TypedData, Parse)} method returns a non null value.</li>
   * </ul>
   *
   * <h3>Core Parsers</h3>
   * <p>
   * Ratpack core provides parsers for {@link ratpack.form.Form}, and JSON (see {@link ratpack.jackson.Jackson}).
   *
   * <h3>Example Usage</h3>
   * <pre class="java">{@code
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.form.Form;
   * import ratpack.parse.NullParseOpts;
   *
   * public class FormHandler implements Handler {
   *   public void handle(Context context) {
   *     context.parse(Form.class).then(form -> context.render(form.get("someFormParam")));
   *   }
   * }
   * }</pre>
   *
   * @param parse The specification of how to parse the request
   * @param <T> The type of object the request is parsed into
   * @param <O> the type of the parse options object
   * @return a promise for the parsed object
   * @see #parse(Class)
   * @see #parse(Class, Object)
   * @see ratpack.parse.Parser
   */
  <T, O> Promise<T> parse(Parse<T, O> parse);

  /**
   * Parses the provided request body into an object.
   * <p>
   * This variant can be used when a reference to the request body has already been obtained.
   * For example, this can be used during the implementation of a {@link Parser} that needs to delegate to another parser.
   * <p>
   * From within a handler, it is more common to use {@link #parse(Parse)} or similar.
   *
   * @param body The request body
   * @param parse The specification of how to parse the request
   * @param <T> The type of object the request is parsed into
   * @param <O> The type of the parse options object
   * @return a promise for the parsed object
   * @see #parse(Parse)
   * @throws Exception any thrown by the parser
   */
  <T, O> T parse(TypedData body, Parse<T, O> parse) throws Exception;

  /**
   * Provides direct access to the backing Netty channel.
   * <p>
   * General only useful for low level extensions. Avoid if possible.
   *
   * @return Direct access to the underlying channel.
   */
  DirectChannelAccess getDirectChannelAccess();

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
   * Returns the current filesystem binding from the context registry.
   *
   * @return the current filesystem binding from the context registry
   */
  default FileSystemBinding getFileSystemBinding() {
    return get(FileSystemBinding.TYPE);
  }

  /**
   * Issues a 404 client error.
   * <p>
   * This method is literally a shorthand for {@link #clientError(int) clientError(404)}.
   * <p>
   * This is a terminal handler operation.
   *
   * @since 1.1
   */
  default void notFound() {
    clientError(404);
  }

}
