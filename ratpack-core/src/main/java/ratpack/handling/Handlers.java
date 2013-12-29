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
import ratpack.handling.internal.*;
import ratpack.http.internal.HeaderHandler;
import ratpack.http.internal.MethodHandler;
import ratpack.launch.LaunchConfig;
import ratpack.path.PathBinder;
import ratpack.path.internal.PathHandler;
import ratpack.path.internal.TokenPathBinder;
import ratpack.registry.Registry;
import ratpack.util.Action;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

/**
 * Factory methods for handler decorations.
 */
public abstract class Handlers {

  private Handlers() {
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given service addition.
   * <p>
   * The service object will be available by its concrete type.
   * To make it available by a different type (perhaps one of its interfaces) use {@link #register(Class, Object, Handler)}.
   *
   * @param object The object to add to the service, only for the handlers defined by {@code builder}
   * @param handler The handler to
   * @param <T> The concrete type of the service addition
   * @return A handler
   */
  public static <T> Handler register(T object, Handler handler) {
    return new RegisteringHandler(object, handler);
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given service addition.
   *
   * @param type The type by which to make the service addition available
   * @param object The object to add to the service, only for the handlers defined by {@code builder}
   * @param handler The handler to
   * @param <T> The concrete type of the service addition
   * @return A handler
   */
  public static <T> Handler register(Class<? super T> type, T object, Handler handler) {
    return new RegisteringHandler(type, object, handler);
  }

  /**
   * Builds a handler chain, with no backing registry.
   *
   * @param launchConfig The application launch config
   * @param action The chain definition
   * @return A handler
   */
  public static Handler chain(LaunchConfig launchConfig, Action<? super Chain> action) throws Exception {
    return chain(launchConfig, null, action);
  }

  /**
   * Builds a chain, backed by the given registry.
   *
   * @param launchConfig The application launch config
   * @param registry The registry.
   * @param action The chain building action.
   * @return A handler
   */
  public static Handler chain(LaunchConfig launchConfig, @Nullable Registry registry, Action<? super Chain> action) throws Exception {
    return ChainBuilders.build(launchConfig, new ChainActionTransformer(launchConfig, registry), action);
  }

  /**
   * Creates a handler chain from the given handlers.
   *
   * @param handlers The handlers to connect into a chain
   * @return A new handler that is the given handlers connected into a chain
   */
  public static Handler chain(List<? extends Handler> handlers) {
    if (handlers.size() == 0) {
      return next();
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
      return next();
    } else if (handlers.length == 1) {
      return handlers[0];
    } else {
      return new ChainHandler(handlers);
    }
  }

  /**
   * A handlers that changes the {@link ratpack.file.FileSystemBinding} for the given handlers.
   * <p>
   * The new file system binding will be created by the {@link ratpack.file.FileSystemBinding#binding(String)} method of the contextual binding.
   *
   * @param path The relative path to the new file system binding point
   * @param handler The handler to execute with the new file system binding
   * @return A handler
   */
  public static Handler fileSystem(String path, Handler handler) {
    return new FileSystemBindingHandler(path, handler);
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
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   * @return A handler
   */
  public static Handler assets(String path, List<String> indexFiles) {
    Handler handler = new AssetHandler(copyOf(indexFiles));
    return fileSystem(path, handler);
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
   * A handler that delegates to the next handler if the request is GET, otherwise raises a 405 client error.
   *
   * @return A handler
   */
  public static Handler get() {
    return MethodHandler.GET;
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
   * A handler that delegates to the next handler if the request is PUT, otherwise raises a 405 client error.
   *
   * @return A handler
   */
  public static Handler put() {
    return MethodHandler.PUT;
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
    return path(new TokenPathBinder(prefix, false), handler);
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
    return path(new TokenPathBinder(path, true), handler);
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

}
