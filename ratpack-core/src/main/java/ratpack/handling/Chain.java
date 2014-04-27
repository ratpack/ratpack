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

import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

/**
 * A chain can be used to build a linked series of handlers.
 * <p>
 * The {@code GroovyChain} type does not represent the handlers "in action".
 * That is, it is the construction of a handler chain.
 * <p>
 * A chain can be constructed using the {@link Handlers#chain(LaunchConfig, ratpack.func.Action)} like methods.
 * For example, from a {@link ratpack.launch.HandlerFactory} implementation…
 * <pre class="tested">
 * import ratpack.launch.HandlerFactory;
 * import ratpack.launch.LaunchConfig;
 * import ratpack.handling.Chain;
 * import ratpack.handling.Handler;
 * import ratpack.handling.Handlers;
 * import ratpack.handling.Context;
 * import ratpack.func.Action;
 *
 * public class MyHandlerBootstrap implements HandlerFactory {
 *   public Handler create(LaunchConfig launchConfig) {
 *
 *     return Handlers.chain(launchConfig, new Action&lt;Chain&gt;() {
 *       public void execute(Chain chain) {
 *         chain
 *           .assets("public")
 *           .prefix("api", chain.chain(new Action&lt;Chain&gt;() {
 *             public void execute(Chain api) {
 *               api
 *                 .get("people", new PeopleHandler())
 *                 .post( "person/:id", new Handler() {
 *                   public void handle(Context context) {
 *                     // handle
 *                   }
 *                 });
 *             }
 *           }));
 *       }
 *     });
 *   }
 * }
 *
 * public class PeopleHandler implements Handler {
 *   public void handle(Context context) {
 *     // handle
 *   }
 * }
 * </pre>
 * <p>
 * Chains <i>may</i> be backed by a {@link Registry registry}, depending on how the chain was constructed.
 * For example, the Ratpack Guice module makes it possible to create a Guice backed registry that can be used to
 * construct dependency injected handlers. See the {@code ratpack-guice} library for details.
 * </p>
 * <p>
 * A Groovy specific subclass of this interface is provided by the Groovy module that overloads methods here with {@code Closure} based variants.
 * See the {@code ratpack-groovy} library for details.
 * </p>
 */
public interface Chain {

  /**
   * Adds a handler that serves static assets at the given file system path, relative to the contextual file system binding.
   * <p>
   * See {@link Handlers#assets(LaunchConfig, String, java.util.List)} for more details on the handler created
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

  /**
   * Adds a handler to this chain that changes the {@link ratpack.file.FileSystemBinding} for the given handler.
   *
   * @param path the relative path to the new file system binding point
   * @param handler the handler
   * @return this}
   */
  Chain fileSystem(String path, Handler handler);

  /**
   * Adds a handler to this chain that changes the {@link ratpack.file.FileSystemBinding} for the given handler chain.
   *
   * @param path the relative path to the new file system binding point
   * @param action the definition of the handler chain
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  Chain fileSystem(String path, Action<? super Chain> action) throws Exception;

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

  /**
   * The launch config of the application that this chain is being created for.
   *
   * @return The launch config of the application that this chain is being created for.
   */
  LaunchConfig getLaunchConfig();

  /**
   * The registry that backs this.
   * <p>
   * The registry that is available is dependent on how the {@code GroovyChain} was constructed.
   *
   * @see Handlers#chain(LaunchConfig, Registry, ratpack.func.Action)
   * @return The registry that backs this, or {@code null} if this has no registry.
   */
  @Nullable
  Registry getRegistry();

  /**
   * Adds the given handler to this.
   *
   * @param handler the handler to add
   * @return this
   */
  Chain handler(Handler handler);

  /**
   * Adds a handler that delegates to the given handler if the relative {@code path}
   * matches the given {@code path} exactly.
   * <p>
   * Nesting {@code path} handlers will not work due to the exact matching, use a combination of {@code path}
   * and {@code prefix} instead.  See {@link Chain#prefix(String, Handler)} for details.
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

  /**
   * Adds a handler to the chain that delegates to the given handler if the request has a header with the given name and a its value matches the given value exactly.
   *
   * <pre tested="java-chain-dsl>
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

  /**
   * Adds a handler that delegates to the given handler if the relative path starts with the given {@code prefix}.
   * <p>
   * All path based handlers become relative to the given {@code prefix}.
   * <p>
   * See {@link ratpack.handling.Handlers#prefix(String, Handler)} for format details on the {@code prefix} string.
   *
   * @param prefix the relative path to match on
   * @param handler the handler to delegate to if the prefix matches
   * @return this
   */
  Chain prefix(String prefix, Handler handler);

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
   * Builds a new registry via the given action, then registers it via {@link #register(Registry)}
   *
   * @param action the definition of a registry
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  Chain register(Action<? super RegistrySpec> action) throws Exception;

  /**
   * Adds a handler that inserts the given handler with the given registry via {@link Context#insert(ratpack.registry.Registry, Handler...)}.
   *
   * @param registry the registry to insert
   * @param handler the handler to insert
   * @return this
   */
  Chain register(Registry registry, Handler handler);

  /**
   * Adds a handler that inserts the given handler chain with the given registry via {@link Context#insert(ratpack.registry.Registry, Handler...)}.
   *
   * @param registry the registry to insert
   * @param action the definition of the handler chain
   * @return this
   */
  Chain register(Registry registry, Action<? super Chain> action) throws Exception;

  /**
   * Adds a handler that inserts the given handler with the a registry built by the given action via {@link Context#insert(ratpack.registry.Registry, Handler...)}.
   *
   * @param registryAction the definition of the registry to insert
   * @param handler the handler to insert
   * @return this
   */
  Chain register(Action<? super RegistrySpec> registryAction, Handler handler) throws Exception;

  /**
   * Adds a handler that inserts the given handler chain with a registry built by the given action via {@link Context#insert(ratpack.registry.Registry, Handler...)}.
   *
   * @param registryAction the definition of the registry to insert]
   * @param action the definition of the handler chain
   * @return this
   */
  Chain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> action) throws Exception;

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

}
