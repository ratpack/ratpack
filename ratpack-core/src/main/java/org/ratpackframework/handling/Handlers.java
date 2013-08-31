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

package org.ratpackframework.handling;

import com.google.common.collect.ImmutableList;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.file.internal.AssetHandler;
import org.ratpackframework.file.internal.FileSystemBindingHandler;
import org.ratpackframework.handling.internal.*;
import org.ratpackframework.http.internal.MethodHandler;
import org.ratpackframework.path.PathBinder;
import org.ratpackframework.path.internal.PathHandler;
import org.ratpackframework.path.internal.TokenPathBinder;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.util.Action;

import java.io.File;
import java.util.List;

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
   * To make it available by a different type (perhaps one of its interfaces) use {@link #register(Class, Object, List)}.
   *
   * @param object The object to add to the service, only for the handlers defined by {@code builder}
   * @param handlers The handler to
   * @param <T> The concrete type of the service addition
   * @return A handler
   */
  public static <T> Handler register(T object, List<? extends Handler> handlers) {
    return new RegisteringHandler(object, ImmutableList.copyOf(handlers));
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given service addition.
   *
   * @param type The type by which to make the service addition available
   * @param object The object to add to the service, only for the handlers defined by {@code builder}
   * @param handlers The handler to
   * @param <T> The concrete type of the service addition
   * @return A handler
   */
  public static <T> Handler register(Class<? super T> type, T object, List<? extends Handler> handlers) {
    return new RegisteringHandler(type, object, ImmutableList.copyOf(handlers));
  }

  /**
   * Builds a handler chain, with no backing registry.
   *
   * @param action The chain definition
   * @return A handler
   */
  public static Handler chain(Action<? super Chain> action) {
    return chain(null, action);
  }

  /**
   * Builds a chain, backed by the given registry.
   *
   * @param registry The registry.
   * @param action The chain building action.
   * @return A handler
   */
  public static Handler chain(@Nullable Registry<Object> registry, Action<? super Chain> action) {
    return ChainBuilder.INSTANCE.buildHandler(new ChainActionTransformer(registry), action);
  }

  /**
   * Builds a list of handlers using the given chain action.
   * <p>
   * The chain given to the action will have no backing registry.
   *
   * @param action The chain building action
   * @return The handlers added by the chain action
   */
  public static List<Handler> chainList(Action<? super Chain> action) {
    return chainList(null, action);
  }

  /**
   * Builds a list of handlers using the given chain action.
   * <p>
   * The chain given to the action will have the given backing registry.
   *
   * @param action The chain building action
   * @param registry The registry to back the chain with
   * @return The handlers added by the chain action
   */
  public static List<Handler> chainList(@Nullable Registry<Object> registry, Action<? super Chain> action) {
    return ChainBuilder.INSTANCE.buildList(new ChainActionTransformer(registry), action);
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
      return new ChainHandler(ImmutableList.copyOf(handlers));
    }
  }

  /**
   * A handlers that changes the {@link org.ratpackframework.file.FileSystemBinding} for the given handlers.
   * <p>
   * The new file system binding will be created by the {@link org.ratpackframework.file.FileSystemBinding#binding(String)} method of the contextual binding.
   *
   * @param path The relative path to the new file system binding point
   * @param handlers The handlers to execute with the new file system binding
   * @return A handler
   */
  public static Handler fileSystem(String path, List<? extends Handler> handlers) {
    return new FileSystemBindingHandler(new File(path), ImmutableList.copyOf(handlers));
  }

  /**
   * A handler that serves static assets at the given file system path, relative to the contextual file system binding.
   * <p>
   * The file to serve is calculated based on the contextual {@link org.ratpackframework.file.FileSystemBinding} and the
   * contextual {@link org.ratpackframework.path.PathBinding}.
   * The {@link org.ratpackframework.path.PathBinding#getPastBinding()} of the contextual path binding is used to find a file/directory
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

  public static Handler assets(String path, String... indexFiles) {
    Handler handler = new AssetHandler(ImmutableList.<String>builder().add(indexFiles).build());
    return fileSystem(path, ImmutableList.of(handler));
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
   * The {@code prefix} is relative to the contextual {@link org.ratpackframework.path.PathBinding} of the exchange.
   * <p>
   * A new contextual {@link org.ratpackframework.path.PathBinding} will be established for the given handlers,
   * using the given prefix as the bind point.
   *
   * @param prefix The path prefix to match
   * @param handlers The handlers to delegate to
   * @return A handler
   */
  public static Handler prefix(String prefix, List<? extends Handler> handlers) {
    return path(new TokenPathBinder(prefix, false), handlers);
  }

  /**
   * Creates a handler that delegates to the given handlers if the request matches the given path exactly.
   * <p>
   * The {@code path} is relative to the contextual {@link org.ratpackframework.path.PathBinding} of the exchange.
   * <p>
   * A new contextual {@link org.ratpackframework.path.PathBinding} will be established for the given handlers,
   * using the given path as the bind point.
   *
   * @param path The exact path to match to
   * @param handlers The handlers to delegate to if the path matches
   * @return A handler
   */
  public static Handler path(String path, List<? extends Handler> handlers) {
    return path(new TokenPathBinder(path, true), handlers);
  }

  /**
   * Creates a handler that delegates to the given handlers if the request can be bound by the given path binder.
   *
   * @param pathBinder The path binder that may bind to the request path
   * @param handlers The handlers to delegate to if path binder does bind to the path
   * @return A handler
   */
  public static Handler path(PathBinder pathBinder, List<? extends Handler> handlers) {
    return new PathHandler(pathBinder, ImmutableList.copyOf(handlers));
  }
}
