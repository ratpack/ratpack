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

import ratpack.func.Action;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.ServerConfig;

/**
 * A chain can be used to build a linked series of handlers.
 * <p>
 * A handler chain can be constructed using the {@link Handlers#chain(ServerConfig, ratpack.func.Action)} like methods.
 * <p>
 * Chains <i>may</i> be backed by a {@link Registry registry}, depending on how the chain was constructed.
 * For example, the Ratpack Guice module makes it possible to create a Guice backed registry that can be used to
 * construct dependency injected handlers. See the {@code ratpack-guice} library for details.
 * </p>
 * <p>
 * A Groovy specific subclass of this interface is provided by the Groovy module that overloads methods here with {@code Closure} based variants.
 * See the {@code ratpack-groovy} library for details.
 * </p>
 * <h3>Path Binding</h3>
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
 * import com.google.common.base.Objects;
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
 *         ctx.render("Hello " + Objects.firstNonNull(ctx.getPathTokens().get("name"), "world"))
 *       )
 *       .get("convert/hex/:tkn", ctx -> // Mandatory path token
 *         ctx.render("Hello " + BaseEncoding.base64().encode(ctx.getPathTokens().get("tkn").getBytes("UTF-8")))
 *       )
 *       .get("pi/:precision?:[\\d]+", ctx -> // Optional regular expression path token
 *         ctx.render(String.format(Locale.ENGLISH, "%1." + Objects.firstNonNull(ctx.getPathTokens().get("precision"), "5") + "f", Math.PI))
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
 */
public interface Chain {

  /**
   * Adds a handler that serves static assets at the given file system path, relative to the contextual file system binding.
   * <p>
   * See {@link Handlers#assets(ServerConfig, String, java.util.List)} for more details on the handler created
   * <pre>
   *    prefix("foo") {
   *      assets("d1", "index.html", "index.xhtml")
   *    }
   * </pre>
   * In the above configuration a request like "/foo/app.js" will return the static file "app.js" that is
   * located in the directory "d1".
   * <p>
   * If the request matches a directory e.g. "/foo", an index file may be served.  The {@code indexFiles}
   * array specifies the names of files to look for in order to serve.
   *
   * @param path the relative path to the location of the assets to serve
   * @param indexFiles the index files to try if the request is for a directory
   * @return this
   */
  Chain assets(String path, String... indexFiles);

  /**
   * Constructs a handler using the given action to define a chain.
   *
   * @param action The action that defines the handler chain
   * @return A handler representing the chain
   * @throws Exception any thrown by {@code action}
   */
  Handler chain(Action<? super Chain> action) throws Exception;

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
   * @see Chain#handler(String, Handler)
   */
  Chain delete(String path, Handler handler);

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
  Chain delete(Handler handler);

  default Chain delete(Class<? extends Handler> handler) {
    return delete(getRegistry().get(handler));
  }

  /**
   * Adds a handler to this chain that changes the {@link ratpack.file.FileSystemBinding} for the given handler chain.
   *
   * @param path the relative path to the new file system binding point
   * @param action the definition of the handler chain
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  Chain fileSystem(String path, Action<? super Chain> action) throws Exception;

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
   * @see Chain#handler(String, Handler)
   */
  Chain get(String path, Handler handler);

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
  Chain get(Handler handler);

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
  Chain handler(Handler handler);

  default Chain handler(Class<? extends Handler> handler) {
    return handler(getRegistry().get(handler));
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
   *       // a request of /person/2/child/1 will not get passed the first handler as it will try
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
  Chain handler(String path, Handler handler);

  default Chain handler(String path, Class<? extends Handler> handler) {
    return handler(path, getRegistry().get(handler));
  }

  /**
   * Adds a handler to the chain that delegates to the given handler if the request has a header with the given name and a its value matches the given value exactly.
   *
   * <pre class="java-chain-dsl">
   *  chain.
   *    header("foo", "bar", new Handler() {
   *      public void handle(Context context) {
   *        context.getResponse().send("Header Handler");
   *      }
   *    });
   * </pre>
   *
   * @param headerName the name of the HTTP Header to match on
   * @param headerValue the value of the HTTP Header to match on
   * @param handler the handler to delegate to
   * @return this
   */
  Chain header(String headerName, String headerValue, Handler handler);

  default Chain header(String headerName, String headerValue, Class<? extends Handler> handler) {
    return header(headerName, headerValue, getRegistry().get(handler));
  }

  /**
   * Adds a handler to the chain that delegates to the given handler chain if the request has a {@code Host} header that matches the given value exactly.
   *
   * <pre class="java-chain-dsl">
   *  chain.
   *    host("foo.com", new Action&lt;Chain&gt;() {
   *      public void execute(Chain hostChain) {
   *        hostChain.handler(new Handler() {
   *          public void handle(Context context) {
   *            context.getResponse().send("Host Handler");
   *          }
   *        });
   *      }
   *    });
   * </pre>
   *
   * @param hostName the name of the HTTP Header to match on
   * @param action the handler chain to delegate to if the host matches
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  Chain host(String hostName, Action<? super Chain> action) throws Exception;

  default Chain host(String hostName, Class<? extends Action<? super Chain>> action) throws Exception {
    return host(hostName, getRegistry().get(action));
  }

  /**
   * Inserts the given nested handler chain.
   * <p>
   * Shorter form of {@link #handler(Handler)} handler}({@link #chain(ratpack.func.Action) chain}({@code action}).
   *
   * @param action the handler chain to insert
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  Chain insert(Action<? super Chain> action) throws Exception;

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
   * @see Chain#handler(String, Handler)
   */
  Chain patch(String path, Handler handler);

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
  Chain patch(Handler handler);

  default Chain patch(Class<? extends Handler> handler) {
    return patch(getRegistry().get(handler));
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
   * @see Chain#handler(String, Handler)
   */
  Chain post(String path, Handler handler);

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
  Chain post(Handler handler);

  default Chain post(Class<? extends Handler> handler) {
    return post(getRegistry().get(handler));
  }

  /**
   * Adds a handler that delegates to the given handlers if the
   * relative path starts with the given {@code prefix}.
   * <p>
   * All path based handlers become relative to the given {@code prefix}.
   * <pre class="java-chain-dsl">
   *   chain
   *     .prefix("person/:id", new Action&lt;Chain&gt;() {
   *       public void execute(Chain personChain) {
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
   *           .prefix("child/:childId", new Action&lt;Chain&gt;() {
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
   * </pre>
   * <p>
   * See {@link ratpack.handling.Handlers#prefix(String, Handler)}
   * for format details on the {@code prefix} string.
   *
   * @param prefix the relative path to match on
   * @param action the handler chain to delegate to if the prefix matches
   * @throws Exception any thrown by {@code action}
   * @return this
   */
  Chain prefix(String prefix, Action<? super Chain> action) throws Exception;

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
   * @see Chain#handler(String, Handler)
   */
  Chain put(String path, Handler handler);

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
  Chain put(Handler handler);

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
  Chain redirect(int code, String location);

  /**
   * Makes the contents of the given registry available for downstream handlers of the same nesting level.
   * <p>
   * The registry is inserted via the {@link ratpack.handling.Context#next(Registry)} method.
   *
   * @param registry the registry whose contents should be made available to downstream handlers
   * @return this
   */
  Chain register(Registry registry);

  /**
   * Builds a new registry via the given action, then registers it via {@link #register(Registry)}.
   *
   * @param action the definition of a registry
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  Chain register(Action<? super RegistrySpec> action) throws Exception;

  /**
   * Adds a handler that inserts the given handler chain with the given registry via {@link Context#insert(ratpack.registry.Registry, Handler...)}.
   *
   * @param registry the registry to insert
   * @param action the definition of the handler chain
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  Chain register(Registry registry, Action<? super Chain> action) throws Exception;

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
  Chain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> action) throws Exception;

  default Chain register(Action<? super RegistrySpec> registryAction, Class<? extends Action<? super Chain>> action) throws Exception {
    return register(registryAction, getRegistry().get(action));
  }

}
