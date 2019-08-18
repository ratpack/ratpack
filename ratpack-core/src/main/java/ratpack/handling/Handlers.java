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
import ratpack.file.FileHandlerSpec;
import ratpack.file.internal.DefaultFileHandlerSpec;
import ratpack.file.internal.FileSystemBindingHandler;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.func.Predicate;
import ratpack.handling.internal.*;
import ratpack.path.PathBinder;
import ratpack.path.internal.PathHandler;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

import java.util.List;

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
   * Creates a handler that serves files from the file system.
   * <p>
   * This method is a standalone version of {@link Chain#files(Action)}.
   *
   * @param serverConfig the server config
   * @param config the configuration of the file handler
   * @return a file serving handler
   * @throws Exception any thrown by {@code config}
   */
  public static Handler files(ServerConfig serverConfig, Action<? super FileHandlerSpec> config) throws Exception {
    return DefaultFileHandlerSpec.build(serverConfig, config);
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
    return ChainBuilders.build(new ChainActionTransformer(serverConfig, registry), action);
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
      return Context::next;
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
      return Handlers.next();
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
   * A handler that delegates to the next handler if the request is OPTIONS, otherwise raises a 405 client error.
   *
   * @return A handler
   * @since 1.1
   */
  public static Handler options() {
    return MethodHandler.OPTIONS;
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
    return path(PathBinder.parse(path, true), handler);
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
    return path(PathBinder.parse(prefix, false), handler);
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

  /**
   * Creates a handler that inserts and delegates the given handler if the predicate applies to the context.
   * <p>
   * If the predicate does not apply, calls {@link Context#next()}.
   *
   * @param test the test whether to when to the given handler
   * @param handler the handler to insert if the predicate applies
   * @return a handler
   */
  public static Handler when(Predicate<? super Context> test, Handler handler) {
    return new WhenHandler(test, handler);
  }

  /**
   * Creates a handler that inserts and delegates to the appropriate handler depending if the predicate applies to the context.
   * <p>
   *
   * @param test the test whether to delegate to the appropriate handler
   * @param ifHandler the handler to insert if the predicate applies
   * @param elseHandler the handler to insert if the predicate doesn't apply
   * @return a handler
   * @since 1.5
   */
  public static Handler whenOrElse(Predicate<? super Context> test, Handler ifHandler, Handler elseHandler) {
    return new WhenHandler(test, ifHandler, elseHandler);
  }

  /**
   * Creates a handler that delegates to the given handler if the predicate applies to the context.
   * <p>
   * If the predicate does not apply, calls {@link Context#next()}.
   * <p>
   * This method does not {@link Context#insert(Handler...) insert} the handler as {@link #when(Predicate, Handler)} does;
   * it calls its {@link Handler#handle(Context)} method directly
   *
   * @param test the test whether to when to the given handler
   * @param handler the handler to call if the predicate applies
   * @return a handler
   */
  public static Handler onlyIf(Predicate<? super Context> test, Handler handler) {
    return new OnlyIfHandler(test, handler);
  }

  /**
   * Creates a handler from the given block
   * <p>
   * The created handler simply invokes the block.
   *
   * @param block the block to invoke
   * @return a handler
   * @since 1.5
   */
  public static Handler of(Block block) {
    return ctx -> block.execute();
  }

  /**
   * Builds a content negotiating handler.
   *
   * @param registry the registry to obtain handlers from for by-class lookups
   * @param action the spec action
   * @return a content negotiating handler
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   */
  public static Handler byContent(Registry registry, Action<? super ByContentSpec> action) throws Exception {
    return new ContentNegotiationHandler(registry, action);
  }

  /**
   * Builds a multi method handler.
   *
   * @param registry the registry to obtain handlers from for by-class lookups
   * @param action the spec action
   * @return a multi method handler
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   */
  public static Handler byMethod(Registry registry, Action<? super ByMethodSpec> action) throws Exception {
    return new MultiMethodHandler(registry, action);
  }

}
