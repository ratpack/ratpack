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

import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.file.FileHandlerSpec;
import ratpack.func.Action;
import ratpack.func.Predicate;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.util.Exceptions;

import java.util.Optional;

/**
 * A chain is a write only builder for composing handlers.
 * <p>
 * A chain object can't be used to handle requests.
 * It can be thought of as a Domain Specific Language (DSL), or API, for constructing a {@code List<Handler>}.
 * <p>
 * To understand the concept of a all chain, it is important to understand that a {@link Handler} can do one of three things:
 * <ol>
 * <li>Respond to the request (terminating processing);</li>
 * <li>{@link Context#insert(Handler...) Insert handlers} and delegate processing;</li>
 * <li>Delegate to the {@link Context#next() next handler}.</li>
 * </ol>
 * <p>
 * Methods like {@link Handlers#chain(ServerConfig, ratpack.func.Action)} take a function that acts on a {@code Chain}, and return a {@link Handler}.
 * The returned handler effectively just performs an insert of the handlers added to the chain during the action..
 * <p>
 * It is very common to use this API to declare the handlers for an application as part of startup via the {@link RatpackServerSpec#handlers(Action)} method.
 *
 * <h3>Registry</h3>
 * <p>
 * Chains <i>may</i> be backed by a {@link Registry registry}, depending on how the chain was constructed.
 * The {@link RatpackServerSpec#handlers(Action)} method backs the chain instance with the server registry.
 * The backing registry can be obtained via {@link #getRegistry()} on the chain instance.
 * <p>
 * This mechanism allows access to “supporting objects” while building the chain.
 * Methods such as {@link #all(Class)} also allow obtaining all implementations from the registry to use.
 * This can be useful when using the Guice integration (or similar) to allow all instance to be dependency injected through Guice.
 *
 * <h3>Adding handlers</h3>
 * <p>
 * The most basic method of Chain API is the {@link #all(Handler)} method.
 * The word “all” represents that all requests reaching this point in the chain will flow through the given handler.
 * This is in contrast to methods such as {@link #path(String, Handler)} that will only the request through the given handler if the request path matches.
 * <p>
 * Methods such as {@link #path(String, Handler)}, {@link #when(Predicate, Action)} etc. are merely more convenient forms of {@link #all(Handler)} and use of the static methods of {@link Handlers}.
 * <p>
 * For each method that takes a literal {@link Handler}, there exists a variant that takes a {@code Class<? extends Handler>}.
 * Such methods obtain an instance of the given handler by asking the chain registry for an instance of the given type.
 * This is generally most useful if the chain registry is backed by some kind of dependency injection mechanism (like Google Guice)
 * that can construct the handler and inject its dependencies as needed.
 *
 * <h3><a name="path-binding">Path Binding</a></h3>
 * <p>
 * Methods such as {@link #get(String, Handler)}, {@link #prefix(String, Action)}, accept a string argument as a request path binding specification.
 * These strings can contain symbols that allow {@link ratpack.path.PathTokens} to be captured and for path binding to be dynamic.
 * For example, the path string {@code "foo/:val"} will match paths such as {@code "foo/bar"}, {@code "foo/123"} or indeed <code>"foo/<i>«anything»</i>"</code>.
 * <p>
 * The following table describes the types of symbols that can be used in path strings…
 * <table>
 *   <caption>Path binding symbols</caption>
 *   <tr>
 *     <th>Path Type</th>
 *     <th>Syntax</th>
 *     <th>Example</th>
 *   </tr>
 *   <tr>
 *     <td>Literal</td>
 *     <td>{@code foo}</td>
 *     <td>{@code "foo"}</td>
 *   </tr>
 *   <tr>
 *     <td>Regular Expression Literal</td>
 *     <td><code>::<i>«regex»</i></code></td>
 *     <td>{@code "foo/::\d+"}</td>
 *   </tr>
 *   <tr>
 *     <td>Optional Path Token</td>
 *     <td><code>:<i>«token-name»</i>?</code></td>
 *     <td>{@code "foo/:val?"}</td>
 *   </tr>
 *   <tr>
 *     <td>Mandatory Path Token</td>
 *     <td><code>:<i>«token-name»</i></code></td>
 *     <td>{@code "foo/:val"}</td>
 *   </tr>
 *   <tr>
 *     <td>Optional Regular Expression Path Token</td>
 *     <td><code>:<i>«token-name»</i>?:<i>«regex»</i></code></td>
 *     <td>{@code "foo/:val?:\d+"}</td>
 *   </tr>
 *   <tr>
 *     <td>Mandatory Regular Expression Path Token</td>
 *     <td><code>:<i>«token-name»</i>:<i>«regex»</i></code></td>
 *     <td>{@code "foo/:val:\d+"}</td>
 *   </tr>
 * </table>
 * <p>The following example shows different kinds of binding paths in action.</p>
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import com.google.common.base.MoreObjects;
 * import com.google.common.io.BaseEncoding;
 * import java.util.Arrays;
 * import java.util.Locale;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(c -> c
 *       .get("favorites/food", ctx -> ctx.render("pizza")) // Literal
 *       .get("favorites/::colou?r", ctx -> ctx.render("blue")) // Regular expression literal
 *       .get("optionalToken/:tkn?", ctx -> ctx.render(ctx.getPathTokens().toString())) // Optional path token
 *       .get("greeting/:name?", ctx -> // Optional path token with default handling
 *         ctx.render("Hello " + MoreObjects.firstNonNull(ctx.getPathTokens().get("name"), "world"))
 *       )
 *       .get("convert/hex/:tkn", ctx -> // Mandatory path token
 *         ctx.render("Hello " + BaseEncoding.base64().encode(ctx.getPathTokens().get("tkn").getBytes("UTF-8")))
 *       )
 *       .get("pi/:precision?:[\\d]+", ctx -> // Optional regular expression path token
 *         ctx.render(String.format(Locale.ENGLISH, "%1." + MoreObjects.firstNonNull(ctx.getPathTokens().get("precision"), "5") + "f", Math.PI))
 *       )
 *       .get("sum/:num1:[\\d]+/:num2:[\\d]+", ctx -> // Mandatory regular expression path tokens
 *         ctx.render(
 *           Arrays.asList("num1", "num2")
 *           .stream()
 *           .map(it -> ctx.getPathTokens().get(it))
 *           .mapToInt(Integer::valueOf)
 *           .sum() + ""
 *         )
 *       )
 *     ).test(httpClient -> {
 *       assertEquals("pizza", httpClient.getText("favorites/food")); // Literal value matched
 *       assertEquals("blue", httpClient.getText("favorites/color")); // Regular expression literal matched
 *       assertEquals("blue", httpClient.getText("favorites/colour")); // Regular expression literal matched
 *       assertEquals("{tkn=val}", httpClient.getText("optionalToken/val")); // Optional path token with value specified
 *       assertEquals("{tkn=}", httpClient.getText("optionalToken/")); // Optional path token with trailing slash treated as empty string
 *       assertEquals("{}", httpClient.getText("optionalToken")); // Optional path token without trailing slash treated as missing
 *       assertEquals("Hello Ratpack", httpClient.getText("greeting/Ratpack")); // Optional path token with value specified
 *       assertEquals("Hello world", httpClient.getText("greeting")); // Optional path token with default handling
 *       assertEquals("Hello UmF0cGFjaw==", httpClient.getText("convert/hex/Ratpack")); // Mandatory path token
 *       assertEquals("3.14159", httpClient.getText("pi")); // Optional regular expression path token with default handling
 *       assertEquals("3.14", httpClient.getText("pi/2")); // Optional regular expression path token with value specified
 *       assertEquals("3.1415927", httpClient.getText("pi/7")); // Optional regular expression path token with value specified
 *       assertEquals("42", httpClient.getText("sum/13/29")); // Mandatory regular expression path tokens
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3>HTTP Method binding</h3>
 * <p>
 * Methods such as {@link #get(Handler)}, {@link #post(Handler)} etc. bind based on the HTTP method of the request.
 * They are effectively a combination of the use of {@link #path(String, Handler)} and the {@link Context#byMethod(Action)} construct
 * to declare that the given path <b>ONLY</b> responds to the specified method.
 * <p>
 * The following two code snippets are identical:
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(c -> c
 *       .path("foo", ctx ->
 *         ctx.byMethod(m -> m
 *           .get(() -> ctx.render("ok"))
 *         )
 *       )
 *     ).test(httpClient -> {
 *       assertEquals("ok", httpClient.getText("foo"));
 *       assertEquals(405, httpClient.post("foo").getStatusCode());
 *     });
 *   }
 * }
 * }</pre>
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(c -> c
 *       .get("foo", ctx -> ctx.render("ok"))
 *     ).test(httpClient -> {
 *       assertEquals("ok", httpClient.getText("foo"));
 *       assertEquals(405, httpClient.post("foo").getStatusCode());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * That is, methods such as {@link #get(String, Handler)}, {@link #get(Handler)} etc. <b>terminate</b> processing with a
 * {@code 405} (method not supported) client error if the request path matches but the HTTP method does not.
 * They <b>should not be used</b> for URLs that respond differently depending on the method.
 * The correct way to do this is to use {@link #path(String, Handler)} and {@link Context#byMethod(Action)}.
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(c -> c
 *       .path("foo", ctx ->
 *         ctx.byMethod(m -> m
 *           .get(() -> ctx.render("GET"))
 *           .post(() -> ctx.render("POST"))
 *         )
 *       )
 *     ).test(httpClient -> {
 *       assertEquals("GET", httpClient.getText("foo"));
 *       assertEquals("POST", httpClient.postText("foo"));
 *       assertEquals(405, httpClient.delete("foo").getStatusCode());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * Given the following, a POST to /foo will yield a 405 response.
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(c -> c
 *       .get("foo", ctx -> ctx.render("GET"))
 *       .post("foo", ctx -> ctx.render("POST"))
 *     ).test(httpClient -> {
 *       assertEquals("GET", httpClient.getText("foo"));
 *
 *       // NOTE: returns 405, not 200 and "POST"
 *       assertEquals(405, httpClient.post("foo").getStatusCode());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * All methods that match HTTP methods, are synonyms for {@link #path(String, Class)} in terms of path binding.
 * That is, {@link #get(Handler)} behaves the same way with regard to path binding as {@link #path(Handler)}, and not {@link #all(Handler)}.
 */
public interface Chain {

  /**
   * Adds a handler that serves files from the file system.
   * <p>
   * The given action configures how and what files will be served.
   * The handler binds to a {@link FileHandlerSpec#path(String) request path}
   * and a {@link FileHandlerSpec#dir(String) directory} within the current filesystem binding.
   * The portion of the request path <i>past</i> the path binding identifies the target file within the directory.
   *
   * <pre class="java">{@code
   * import ratpack.test.embed.EphemeralBaseDir;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EphemeralBaseDir.tmpDir().use(baseDir -> {
   *       baseDir.write("public/some.text", "foo");
   *       baseDir.write("public/index.html", "bar");
   *       EmbeddedApp.of(s -> s
   *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
   *         .handlers(c -> c
   *           .files(f -> f.dir("public").indexFiles("index.html"))
   *         )
   *       ).test(httpClient -> {
   *         assertEquals("foo", httpClient.getText("some.text"));
   *         assertEquals("bar", httpClient.getText());
   *         assertEquals(404, httpClient.get("no-file-here").getStatusCode());
   *       });
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param config the file handler configuration
   * @return {@code this}
   * @throws Exception any thrown by {@code config}
   * @see Handlers#files(ServerConfig, Action)
   * @see FileHandlerSpec
   */
  default Chain files(Action<? super FileHandlerSpec> config) throws Exception {
    return all(Handlers.files(getServerConfig(), config));
  }

  /**
   * {@link #files(Action)}, using the default config.
   *
   * @return {@code this}
   */
  default Chain files() {
    return Exceptions.uncheck(() -> files(Action.noop()));
  }

  /**
   * Constructs a handler using the given action to define a chain.
   *
   * @param action The action that defines the all chain
   * @return A all representing the chain
   * @throws Exception any thrown by {@code action}
   */
  default Handler chain(Action<? super Chain> action) throws Exception {
    return Handlers.chain(getServerConfig(), getRegistry(), action);
  }

  default Handler chain(Class<? extends Action<? super Chain>> action) throws Exception {
    return chain(getRegistry().get(action));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod}
   * is {@code DELETE}.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(String, Handler)
   * @see Chain#post(String, Handler)
   * @see Chain#put(String, Handler)
   * @see Chain#patch(String, Handler)
   * @see Chain#path(String, Handler)
   */
  default Chain delete(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.delete(), handler)));
  }

  default Chain delete(String path, Class<? extends Handler> handler) {
    return delete(path, getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the {@code request} {@code HTTPMethod} is {@code DELETE} and the {@code path} is at the current root.
   *
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(Handler)
   * @see Chain#post(Handler)
   * @see Chain#put(Handler)
   * @see Chain#patch(Handler)
   */
  default Chain delete(Handler handler) {
    return delete("", handler);
  }

  default Chain delete(Class<? extends Handler> handler) {
    return delete(getRegistry().get(handler));
  }

  /**
   * Adds a handler to this chain that changes the {@link ratpack.file.FileSystemBinding} for the given handler chain.
   *
   * @param path the relative path to the new file system binding point
   * @param action the definition of the all chain
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  default Chain fileSystem(String path, Action<? super Chain> action) throws Exception {
    return all(Handlers.fileSystem(getServerConfig(), path, chain(action)));
  }

  default Chain fileSystem(String path, Class<? extends Action<? super Chain>> action) throws Exception {
    return fileSystem(path, getRegistry().get(action));
  }

  /**
   * Adds a handler that delegates to the given handler
   * if the relative {@code path} matches the given {@code path} and the {@code request}
   * {@code HTTPMethod} is {@code GET}.
   * <p>
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#post(String, Handler)
   * @see Chain#put(String, Handler)
   * @see Chain#patch(String, Handler)
   * @see Chain#delete(String, Handler)
   * @see Chain#path(String, Handler)
   */
  default Chain get(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.get(), handler)));
  }

  default Chain get(String path, Class<? extends Handler> handler) {
    return get(path, getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler
   * if the {@code request} {@code HTTPMethod} is {@code GET} and the {@code path} is at the
   * current root.
   *
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#post(Handler)
   * @see Chain#put(Handler)
   * @see Chain#patch(Handler)
   * @see Chain#delete(Handler)
   */
  default Chain get(Handler handler) {
    return get("", handler);
  }

  default Chain get(Class<? extends Handler> handler) {
    return get(getRegistry().get(handler));
  }

  /**
   * The server config of the application that this chain is being created for.
   *
   * @return The server config of the application that this chain is being created for.
   */
  ServerConfig getServerConfig();

  /**
   * The registry that backs this chain.
   * <p>
   * What the registry is depends on how the chain was created.
   * The {@link Handlers#chain(ServerConfig, Registry, Action)} allows the registry to be specified.
   * For a Guice based application, the registry is backed by Guice.
   *
   * @see Handlers#chain(ServerConfig, Registry, Action)
   * @return The registry that backs this
   * @throws IllegalStateException if there is no backing registry for this chain
   */
  Registry getRegistry() throws IllegalStateException;

  /**
   * Adds the given handler to this.
   *
   * @param handler the handler to add
   * @return this
   */
  Chain all(Handler handler);

  default Chain all(Class<? extends Handler> handler) {
    return all(getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if the relative {@code path}
   * matches the given {@code path} exactly.
   * <p>
   * Nesting {@code path} handlers will not work due to the exact matching, use a combination of {@code path}
   * and {@code prefix} instead.  See {@link Chain#prefix(String, ratpack.func.Action)} for details.
   * <pre>
   *   // this will not work
   *   path("person/:id") {
   *     path("child/:childId") {
   *       // a request of /person/2/child/1 will not get passed the first all as it will try
   *       // to match "person/2/child/1" with "person/2" which does not match
   *     }
   *
   *   // this will work
   *   prefix("person/:id") {
   *     path("child/:childId") {
   *       // a request of /person/2/child/1 will work this time
   *     }
   *   }
   * </pre>
   * <p>
   * See {@link Handlers#path(String, Handler)} for the details on how {@code path} is interpreted.
   *
   * @param path the relative path to match exactly on
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#post(String, Handler)
   * @see Chain#get(String, Handler)
   * @see Chain#put(String, Handler)
   * @see Chain#patch(String, Handler)
   * @see Chain#delete(String, Handler)
   */
  default Chain path(String path, Handler handler) {
    return all(Handlers.path(path, handler));
  }

  default Chain path(Handler handler) {
    return path("", handler);
  }

  default Chain path(String path, Class<? extends Handler> handler) {
    return path(path, getRegistry().get(handler));
  }

  default Chain path(Class<? extends Handler> handler) {
    return path("", handler);
  }


  /**
   * Adds a handler to the chain that delegates to the given handler chain if the request has a {@code Host} header that matches the given value exactly.
   *
   * <pre class="java-chain-dsl">{@code
   *  chain.
   *    host("foo.com", new Action<Chain>() {
   *      public void execute(Chain hostChain) {
   *        hostChain.all(new Handler() {
   *          public void handle(Context context) {
   *            context.getResponse().send("Host Handler");
   *          }
   *        });
   *      }
   *    });
   * }</pre>
   *
   * @param hostName the name of the HTTP Header to match on
   * @param action the handler chain to delegate to if the host matches
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  default Chain host(String hostName, Action<? super Chain> action) throws Exception {
    return when(ctx ->
        Optional.ofNullable(ctx.getRequest().getHeaders().get(HttpHeaderNames.HOST))
          .map(s -> s.equals(hostName))
          .orElse(false),
      action
    );
  }

  default Chain host(String hostName, Class<? extends Action<? super Chain>> action) throws Exception {
    return host(hostName, getRegistry().get(action));
  }

  /**
   * Inserts the given nested handler chain.
   * <p>
   * Shorter form of {@link #all(Handler)} handler}({@link #chain(ratpack.func.Action) chain}({@code action}).
   *
   * @param action the handler chain to insert
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  default Chain insert(Action<? super Chain> action) throws Exception {
    return all(chain(action));
  }

  default Chain insert(Class<? extends Action<? super Chain>> action) throws Exception {
    return insert(getRegistry().get(action));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod}
   * is {@code PATCH}.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(String, Handler)
   * @see Chain#post(String, Handler)
   * @see Chain#put(String, Handler)
   * @see Chain#delete(String, Handler)
   * @see Chain#path(String, Handler)
   */
  default Chain patch(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.patch(), handler)));
  }

  default Chain patch(String path, Class<? extends Handler> handler) {
    return patch(path, getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the {@code request} {@code HTTPMethod} is {@code PATCH} and the {@code path} is at the current root.
   *
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(Handler)
   * @see Chain#post(Handler)
   * @see Chain#put(Handler)
   * @see Chain#delete(Handler)
   */
  default Chain patch(Handler handler) {
    return patch("", handler);
  }

  default Chain patch(Class<? extends Handler> handler) {
    return patch(getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod}
   * is {@code OPTIONS}.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this
   * @since 1.1
   * @see Chain#get(String, Handler)
   * @see Chain#post(String, Handler)
   * @see Chain#put(String, Handler)
   * @see Chain#delete(String, Handler)
   * @see Chain#path(String, Handler)
   */
  default Chain options(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.options(), handler)));
  }

  /**
   * @param path the path to bind to
   * @param handler a handler
   * @return {@code this}
   * @since 1.1
   */
  default Chain options(String path, Class<? extends Handler> handler) {
    return options(path, getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the {@code request} {@code HTTPMethod} is {@code OPTIONS} and the {@code path} is at the current root.
   *
   * @param handler the handler to delegate to
   * @return this
   * @since 1.1
   * @see Chain#get(Handler)
   * @see Chain#post(Handler)
   * @see Chain#put(Handler)
   * @see Chain#delete(Handler)
   */
  default Chain options(Handler handler) {
    return options("", handler);
  }

  /**
   * @param handler a handler
   * @return {code this}
   * @since 1.1
   */
  default Chain options(Class<? extends Handler> handler) {
    return options(getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod}
   * is {@code POST}.
   * <p>
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(String, Handler)
   * @see Chain#put(String, Handler)
   * @see Chain#patch(String, Handler)
   * @see Chain#delete(String, Handler)
   * @see Chain#path(String, Handler)
   */
  default Chain post(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.post(), handler)));
  }

  default Chain post(String path, Class<? extends Handler> handler) {
    return post(path, getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the {@code request} {@code HTTPMethod} is {@code POST} and the {@code path} is at the current root.
   * <p>
   *
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(Handler)
   * @see Chain#put(Handler)
   * @see Chain#patch(Handler)
   * @see Chain#delete(Handler)
   */
  default Chain post(Handler handler) {
    return post("", handler);
  }

  default Chain post(Class<? extends Handler> handler) {
    return post(getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handlers if the
   * relative path starts with the given {@code prefix}.
   * <p>
   * All path based handlers become relative to the given {@code prefix}.
   * <pre class="java-chain-dsl">{@code
   *   chain
   *     .prefix("person/:id", new Action<Chain>() {
   *       public void execute(Chain personChain) throws Exception {
   *         personChain
   *           .get("info", new Handler() {
   *             public void handle(Context context) {
   *               // e.g. /person/2/info
   *             }
   *           })
   *           .post("save", new Handler() {
   *             public void handle(Context context) {
   *               // e.g. /person/2/save
   *             }
   *           })
   *           .prefix("child/:childId", new Action<Chain>() {
   *             public void execute(Chain childChain) {
   *               childChain
   *                 .get("info", new Handler() {
   *                   public void handle(Context context) {
   *                     // e.g. /person/2/child/1/info
   *                   }
   *                 });
   *             }
   *           });
   *       }
   *     });
   * }</pre>
   * <p>
   * See {@link ratpack.handling.Handlers#prefix(String, Handler)}
   * for format details on the {@code prefix} string.
   *
   * @param prefix the relative path to match on
   * @param action the handler chain to delegate to if the prefix matches
   * @throws Exception any thrown by {@code action}
   * @return this
   */
  default Chain prefix(String prefix, Action<? super Chain> action) throws Exception {
    return all(Handlers.prefix(prefix, chain(action)));
  }

  default Chain prefix(String prefix, Class<? extends Action<? super Chain>> action) throws Exception {
    return prefix(prefix, getRegistry().get(action));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod}
   * is {@code PUT}.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(String, Handler)
   * @see Chain#post(String, Handler)
   * @see Chain#patch(String, Handler)
   * @see Chain#delete(String, Handler)
   * @see Chain#path(String, Handler)
   */
  default Chain put(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.put(), handler)));
  }

  default Chain put(String path, Class<? extends Handler> handler) {
    return put(path, getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handler if
   * the {@code request} {@code HTTPMethod} is {@code PUT} and the {@code path} is at the current root.
   *
   * @param handler the handler to delegate to
   * @return this
   * @see Chain#get(Handler)
   * @see Chain#post(Handler)
   * @see Chain#patch(Handler)
   * @see Chain#delete(Handler)
   */
  default Chain put(Handler handler) {
    return put("", handler);
  }

  default Chain put(Class<? extends Handler> handler) {
    return put(getRegistry().get(handler));
  }

  /**
   * Sends an HTTP redirect to the specified location.
   * <p>
   * The handler to add is created via {@link Handlers#redirect(int, String)}.
   *
   * @param code the 3XX HTTP status code.
   * @param location the URL to set in the Location response header
   * @return this
   * @see Handlers#redirect(int, String)
   */
  default Chain redirect(int code, String location) {
    return all(Handlers.redirect(code, location));
  }

  /**
   * Makes the contents of the given registry available for downstream handlers of the same nesting level.
   * <p>
   * The registry is inserted via the {@link ratpack.handling.Context#next(Registry)} method.
   *
   * @param registry the registry whose contents should be made available to downstream handlers
   * @return this
   */
  default Chain register(Registry registry) {
    return all(Handlers.register(registry));
  }

  /**
   * Builds a new registry via the given action, then registers it via {@link #register(Registry)}.
   *
   * @param action the definition of a registry
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  default Chain register(Action<? super RegistrySpec> action) throws Exception {
    return register(Registry.of(action));
  }

  /**
   * Adds a handler that inserts the given handler chain with the given registry via {@link Context#insert(ratpack.registry.Registry, Handler...)}.
   *
   * @param registry the registry to insert
   * @param action the definition of the handler chain
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  default Chain register(Registry registry, Action<? super Chain> action) throws Exception {
    return all(Handlers.register(registry, chain(action)));
  }

  default Chain register(Registry registry, Class<? extends Action<? super Chain>> action) throws Exception {
    return register(registry, getRegistry().get(action));
  }

  /**
   * Adds a handler that inserts the given handler chain with a registry built by the given action via {@link Context#insert(ratpack.registry.Registry, Handler...)}.
   *
   * @param registryAction the definition of the registry to insert]
   * @param action the definition of the handler chain
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  default Chain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> action) throws Exception {
    return register(Registry.of(registryAction), action);
  }

  default Chain register(Action<? super RegistrySpec> registryAction, Class<? extends Action<? super Chain>> action) throws Exception {
    return register(registryAction, getRegistry().get(action));
  }

  default Chain when(Predicate<? super Context> test, Action<? super Chain> action) throws Exception {
    return all(Handlers.when(test, chain(action)));
  }

  default Chain when(Predicate<? super Context> test, Class<? extends Action<? super Chain>> action) throws Exception {
    return all(Handlers.when(test, chain(action)));
  }

  /**
   * Inlines the given chain if {@code test} is {@code true}.
   * <p>
   * This is literally just sugar for wrapping the given action in an {@code if} statement.
   * It can be useful when conditionally adding handlers based on state available when building the chain.
   * <pre class="java">{@code
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(a -> a
   *       .registryOf(r -> r.add(1))
   *       .handlers(c -> c
   *         .when(c.getRegistry().get(Integer.class) == 0, i -> i
   *           .get(ctx -> ctx.render("ok"))
   *         )
   *       )
   *     ).test(httpClient ->
   *       assertEquals(httpClient.get().getStatusCode(), 404)
   *     );
   *
   *     EmbeddedApp.of(a -> a
   *       .registryOf(r -> r.add(0))
   *       .handlers(c -> c
   *         .when(c.getRegistry().get(Integer.class) == 0, i -> i
   *           .get(ctx -> ctx.render("ok"))
   *         )
   *       )
   *     ).test(httpClient ->
   *       assertEquals(httpClient.getText(), "ok")
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param test whether to include the given chain action
   * @param action the chain action to maybe include
   * @return this
   * @throws Exception any thrown by {@code action}
   * @since 1.4
   */
  default Chain when(boolean test, Action<? super Chain> action) throws Exception {
    if (test) {
      action.execute(this);
    }
    return this;
  }

  /**
   * Inlines the given chain if {@code test} is {@code true}.
   * <p>
   * Similar to {@link #when(boolean, Action)}, except obtains the action instance from the registry by the given type.
   *
   * @param test whether to include the given chain action
   * @param action the chain action to maybe include
   * @return this
   * @throws Exception any thrown by {@code action}
   * @since 1.4
   */
  default Chain when(boolean test, Class<? extends Action<? super Chain>> action) throws Exception {
    return when(test, getRegistry().get(action));
  }

  default Chain when(Predicate<? super Context> test, Action<? super Chain> onTrue, Action<? super Chain> onFalse) throws Exception {
    return all(Handlers.whenOrElse(test, chain(onTrue), chain(onFalse)));
  }

  default Chain when(Predicate<? super Context> test, Class<? extends Action<? super Chain>> onTrue, Class<? extends Action<? super Chain>> onFalse) throws Exception {
    return all(Handlers.whenOrElse(test, chain(onTrue), chain(onFalse)));
  }

  /**
   * Inlines the appropriate chain based on the given {@code test}.
   * <p>
   * This is literally just sugar for wrapping the given action in an {@code if/else} statement.
   * It can be useful when conditionally adding handlers based on state available when building the chain.
   * <pre class="java">{@code
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(a -> a
   *       .registryOf(r -> r.add(1))
   *       .handlers(c -> c
   *         .when(c.getRegistry().get(Integer.class) == 0,
   *            i -> i.get(ctx -> ctx.render("ok")),
   *            i -> i.get(ctx -> ctx.render("ko"))
   *         )
   *       )
   *     ).test(httpClient ->
   *       assertEquals(httpClient.getText(), "ko")
   *     );
   *
   *     EmbeddedApp.of(a -> a
   *       .registryOf(r -> r.add(0))
   *       .handlers(c -> c
   *         .when(c.getRegistry().get(Integer.class) == 0,
   *            i -> i.get(ctx -> ctx.render("ok")),
   *            i -> i.get(ctx -> ctx.render("ko"))
   *         )
   *       )
   *     ).test(httpClient ->
   *       assertEquals(httpClient.getText(), "ok")
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param test predicate to decide which action include
   * @param onTrue the chain action to include when the predicate is true
   * @param onFalse the chain action to include when the predicate is false
   * @return this
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   */
  default Chain when(boolean test, Action<? super Chain> onTrue, Action<? super Chain> onFalse) throws Exception {
    if (test) {
      onTrue.execute(this);
    } else {
      onFalse.execute(this);
    }
    return this;
  }

  /**
   * Inlines the appropriate chain based on the given {@code test}.
   * <p>
   * Similar to {@link #when(boolean, Action, Action)}, except obtains the action instance from the registry by the given type.
   *
   * @param test predicate to decide which action to include
   * @param onTrue the chain action to include when the predicate is true
   * @param onFalse the chain action to include when the predicate is false
   * @return this
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   */
  default Chain when(boolean test, Class<? extends Action<? super Chain>> onTrue, Class<? extends Action<? super Chain>> onFalse) throws Exception {
    return when(test, getRegistry().get(onTrue), getRegistry().get(onFalse));
  }

  /**
   * Invokes the given handler only if the predicate passes.
   * <p>
   * This method differs from {@link #when when()} in that it does not insert the handler;
   * but directly calls its {@link Handler#handle(Context)} method.
   *
   * @param test the predicate
   * @param handler the handler
   * @return {@code this}
   */
  default Chain onlyIf(Predicate<? super Context> test, Handler handler) {
    return all(Handlers.onlyIf(test, handler));
  }

  default Chain onlyIf(Predicate<? super Context> test, Class<? extends Handler> handler) {
    return all(Handlers.onlyIf(test, getRegistry().get(handler)));
  }

  /**
   * Raises a 404 {@link Context#clientError(int)}.
   * <p>
   * This can be used to effectively terminate processing early.
   * This is sometimes useful when using a scoped client error handler.
   * <pre class="java">{@code
   * import ratpack.error.ClientErrorHandler;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registryOf(r -> r
   *             .add(ClientErrorHandler.class, (ctx, code) -> ctx.render("global"))
   *         )
   *         .handlers(c -> c
   *             .prefix("api", api -> api
   *                 .register(r -> r.add(ClientErrorHandler.class, (ctx, code) -> ctx.render("scoped")))
   *                 .get("foo", ctx -> ctx.render("foo"))
   *                 .notFound()
   *             )
   *         )
   *     ).test(http -> {
   *       assertEquals(http.getText("not-there"), "global");
   *       assertEquals(http.getText("api/foo"), "foo");
   *       assertEquals(http.getText("api/not-there"), "scoped");
   *     });
   *   }
   * }
   * }</pre>
   *
   * @return {@code this}
   * @since 1.1
   */
  default Chain notFound() {
    return all(Handlers.notFound());
  }

}
