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
import ratpack.file.internal.AssetHandler;
import ratpack.file.internal.FileSystemBindingHandler;
import ratpack.func.Action;
import ratpack.handling.internal.*;
import ratpack.http.internal.*;
import ratpack.server.ServerConfig;
import ratpack.path.PathBinder;
import ratpack.path.PathBinders;
import ratpack.path.internal.PathHandler;
import ratpack.registry.Registry;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

/**
 * Factory methods for handler decorations.
 */
public abstract class Handlers {

  private Handlers() {
  }

  /**
   * A handler that delegates to the next handler if the request claims that it can accept one of the given types, otherwise raises a 406 client error.
   *
   * @param contentTypes The content types to verify that the request can support for the response
   * @return A handler
   */
  public static Handler accepts(String... contentTypes) {
    return new AcceptsHandler(contentTypes);
  }

  /**
   * A handler that serves static assets at the given file system path, relative to the contextual file system binding.
   * <p>
   * The file to serve is calculated based on the contextual {@link ratpack.file.FileSystemBinding} and the
   * contextual {@link ratpack.path.PathBinding}.
   * The {@link ratpack.path.PathBinding#getPastBinding()} of the contextual path binding is used to find a file/directory
   * relative to the contextual file system binding.
   * <p>
   * If the request matches a directory, an index file may be served.
   * The {@code indexFiles} array specifies the names of files to look for in order to serve.
   * <p>
   * If no file can be found to serve, then control will be delegated to the next handler.
   *
   * @param serverConfig The application server config
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   * @return A handler
   */
  public static Handler assets(ServerConfig serverConfig, String path, List<String> indexFiles) {
    Handler handler = new AssetHandler(copyOf(indexFiles));
    return fileSystem(serverConfig, path, handler);
  }

  /**
   * Builds a handler chain, with no backing registry.
   *
   * @param serverConfig The server config
   * @param action The chain definition
   * @return A handler
   * @throws Exception any thrown by {@code action}
   */
  public static Handler chain(ServerConfig serverConfig, Action<? super Chain> action) throws Exception {
    return chain(serverConfig, null, action);
  }

  /**
   * Builds a chain, backed by the given registry.
   *
   * @param serverConfig The server config
   * @param registry The registry.
   * @param action The chain building action.
   * @return A handler
   * @throws Exception any thrown by {@code action}
   */
  public static Handler chain(@Nullable ServerConfig serverConfig, @Nullable Registry registry, Action<? super Chain> action) throws Exception {
    return ChainBuilders.build(serverConfig != null && serverConfig.isDevelopment(), new ChainActionTransformer(serverConfig, registry), action);
  }

  /**
   * Builds a chain, backed by the given registry.
   *
   * @param registry The registry.
   * @param action The chain building action.
   * @return A handler
   * @throws Exception any thrown by {@code action}
   */
  public static Handler chain(Registry registry, Action<? super Chain> action) throws Exception {
    return chain(registry.get(ServerConfig.class), registry, action);
  }

  /**
   * Creates a handler chain from the given handlers.
   *
   * @param handlers The handlers to connect into a chain
   * @return A new handler that is the given handlers connected into a chain
   */
  public static Handler chain(List<? extends Handler> handlers) {
    if (handlers.size() == 0) {
      return ctx -> ctx.next();
    } else if (handlers.size() == 1) {
      return handlers.get(0);
    } else {
      return new ChainHandler(handlers);
    }
  }

  /**
   * Creates a handler chain from the given handlers.
   *
   * @param handlers The handlers to connect into a chain
   * @return A new handler that is the given handlers connected into a chain
   */
  public static Handler chain(Handler... handlers) {
    if (handlers.length == 0) {
      return ctx -> ctx.next();
    } else if (handlers.length == 1) {
      return handlers[0];
    } else {
      return new ChainHandler(handlers);
    }
  }

  /**
   * A handler that simply calls {@link Context#clientError(int)} with the given status code.
   *
   * @param statusCode The 4xx client error status code
   * @return A handler
   */
  public static Handler clientError(int statusCode) {
    return new ClientErrorForwardingHandler(statusCode);
  }

  /**
   * A handler that delegates to the next handler if the content type of the request is one of the given types, otherwise raises a 415 client error.
   *
   * @param contentTypes The request content types to require
   * @return A handler
   */
  public static Handler contentTypes(String... contentTypes) {
    return new ContentTypeHandler(contentTypes);
  }

  /**
   * A handler that delegates to the next handler if the request is DELETE, otherwise raises a 405 client error.
   *
   * @return A handler
   */
  public static Handler delete() {
    return MethodHandler.DELETE;
  }

  /**
   * A handlers that changes the {@link ratpack.file.FileSystemBinding} for the given handlers.
   * <p>
   * The new file system binding will be created by the {@link ratpack.file.FileSystemBinding#binding(String)} method of the contextual binding.
   *
   * @param serverConfig The application server config
   * @param path The relative path to the new file system binding point
   * @param handler The handler to execute with the new file system binding
   * @return A handler
   */
  public static Handler fileSystem(ServerConfig serverConfig, String path, Handler handler) {
    return new FileSystemBindingHandler(serverConfig, path, handler);
  }

  /**
   * A handler that delegates to the next handler if the request is GET, otherwise raises a 405 client error.
   *
   * @return A handler
   */
  public static Handler get() {
    return MethodHandler.GET;
  }

  /**
   * Creates a handler that delegates to the given handler if the {@code request} has a {@code HTTPHeader} with the
   * given name and a it's value matches the given value exactly.
   *
   * @param headerName the name of the HTTP Header to match on
   * @param headerValue the value of the HTTP Header to match on
   * @param handler the handler to delegate to
   * @return A handler
   */
  public static Handler header(String headerName, String headerValue, Handler handler) {
    return new HeaderHandler(headerName, headerValue, handler);
  }

  /**
   * Creates a handler that delegates to the given handler if the {@code request} has a {@code HTTPHost} with the
   * given name that matches the given value exactly.
   *
   * @param hostName the name of the HTTP Header to match on
   * @param handler the handler to delegate to
   * @return A handler
   */
  public static Handler host(String hostName, Handler handler) {
    return new HeaderHandler("Host", hostName, handler);
  }

  /**
   * A handler that simply delegates to the next handler.
   * <p>
   * Effectively a noop.
   *
   * @return A handler
   */
  public static Handler next() {
    return NextHandler.INSTANCE;
  }

  /**
   * Convenience for {@link #clientError(int) clientError(404)}.
   *
   * @return A handler
   */
  public static Handler notFound() {
    return ClientErrorForwardingHandler.NOT_FOUND;
  }

  /**
   * A handler that delegates to the next handler if the request is PATCH, otherwise raises a 405 client error.
   *
   * @return A handler
   */
  public static Handler patch() {
    return MethodHandler.PATCH;
  }

  /**
   * Creates a handler that delegates to the given handlers if the request matches the given path exactly.
   * <p>
   * The {@code path} is relative to the contextual {@link ratpack.path.PathBinding} of the exchange.
   * <p>
   * A new contextual {@link ratpack.path.PathBinding} will be established for the given handlers,
   * using the given path as the bind point.
   *
   * @param path The exact path to match to
   * @param handler The handlers to delegate to if the path matches
   * @return A handler
   */
  public static Handler path(String path, Handler handler) {
    return path(PathBinders.parse(path, true), handler);
  }

  /**
   * Creates a handler that delegates to the given handlers if the request can be bound by the given path binder.
   *
   * @param pathBinder The path binder that may bind to the request path
   * @param handler The handlers to delegate to if path binder does bind to the path
   * @return A handler
   */
  public static Handler path(PathBinder pathBinder, Handler handler) {
    return new PathHandler(pathBinder, handler);
  }

  /**
   * A handler that delegates to the next handler if the request is POST, otherwise raises a 405 client error.
   *
   * @return A handler
   */
  public static Handler post() {
    return MethodHandler.POST;
  }

  /**
   * Creates a handler that delegates to the given handlers if the request path starts with the given prefix.
   * <p>
   * The {@code prefix} is relative to the contextual {@link ratpack.path.PathBinding} of the exchange.
   * <p>
   * A new contextual {@link ratpack.path.PathBinding} will be established for the given handlers,
   * using the given prefix as the bind point.
   *
   * @param prefix The path prefix to match
   * @param handler The handler to delegate to
   * @return A handler
   */
  public static Handler prefix(String prefix, Handler handler) {
    return path(PathBinders.parse(prefix, false), handler);
  }

  /**
   * A handler that delegates to the next handler if the request is PUT, otherwise raises a 405 client error.
   *
   * @return A handler
   */
  public static Handler put() {
    return MethodHandler.PUT;
  }

  /**
   * A handler that simply calls {@link Context#insert(Registry, Handler...)} with the given registry and handler.
   *
   * @param registry the registry to insert
   * @param handler The handler to insert
   * @return A handler
   */
  public static Handler register(Registry registry, Handler handler) {
    return new RegistryInsertHandler(registry, handler);
  }

  /**
   * A handler that simply calls {@link Context#next(Registry)} with the given registry.
   *
   * @param registry The registry to make available to the next handlers
   * @return A handler
   * @see Context#next(Registry)
   */
  public static Handler register(Registry registry) {
    return new RegistryNextHandler(registry);
  }

  /**
   * Creates a handler that always issues a redirect using {@link Context#redirect(int, String)} with exactly the given code and location.
   * <p>
   * This method will immediate throw an {@link IllegalArgumentException} if code is &lt; 300 || &gt; 399.
   *
   * @param code the 3XX HTTP status code
   * @param location the URL to set in the Location response header
   * @return a handler
   * @see Context#redirect(int, String)
   */
  public static Handler redirect(int code, String location) {
    return new RedirectionHandler(location, code);
  }

}
