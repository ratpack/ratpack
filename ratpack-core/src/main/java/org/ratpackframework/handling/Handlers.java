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
import org.ratpackframework.file.internal.DirectoryStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileSystemBindingHandler;
import org.ratpackframework.file.internal.TargetFileStaticAssetRequestHandler;
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
 * Factory methods for certain types of handlers. <p> Typically used by {@link Chain} implementations to build a handler chain. <pre class="tested"> import static
 * org.ratpackframework.handling.Handlers.*; import org.ratpackframework.handling.Handler; import org.ratpackframework.handling.Chain; import org.ratpackframework.handling.Context; import
 * org.ratpackframework.util.Action;
 *
 * class ExampleHandler implements Handler { void handle(Context exchange) { // … } }
 *
 * class ChainBuilder implements Action&lt;Chain&gt; { void execute(Chain chain) { chain.assets("public"); chain.get("info", new ExampleHandler()); chain.prefix("api", new Action&lt;Chain&gt;() { void
 * execute(Chain apiChain) { apiChain.get("version", new ExampleHandler()); apiChain.get("log", new ExampleHandler()); } }); } } </pre>
 */
public abstract class Handlers {

  private Handlers() {
  }

  private static <T> ImmutableList<T> singleton(T thing) {
    return ImmutableList.of(thing);
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given service addition. <p> The service object will be available by its concrete type. To make it available by a
   * different type (perhaps one of its interfaces) use {@link #register(Class, Object, org.ratpackframework.util.Action)}.
   *
   * @param object The object to add to the service, only for the handlers defined by {@code builder}
   * @param builder The definition of the handler chain to insert with the service
   * @return A handler
   */
  public static Handler register(Object object, Action<? super Chain> builder) {
    return register(object, chainList(builder));
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given service addition.
   *
   * @param type The type by which to make the service addition available
   * @param object The object to add to the service, only for the handlers defined by {@code builder}
   * @param builder The definition of the handler chain to insert with the service
   * @param <T> The concrete type of the service addition
   * @return A handler
   */
  public static <T> Handler register(Class<? super T> type, T object, Action<? super Chain> builder) {
    return register(type, object, chainList(builder));
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given service addition. <p> The service object will be available by its concrete type. To make it available by a
   * different type (perhaps one of its interfaces) use {@link #register(Class, Object, List)}.
   *
   * @param object The object to add to the service, only for the handlers defined by {@code builder}
   * @param handlers The handler to
   * @param <T> The concrete type of the service addition
   * @return A handler
   */
  public static <T> Handler register(T object, List<Handler> handlers) {
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
  public static <T> Handler register(Class<? super T> type, T object, List<Handler> handlers) {
    return new RegisteringHandler(type, object, ImmutableList.copyOf(handlers));
  }

  /**
   * Builds a handler chain.
   *
   * @param action The chain definition
   * @return A handler
   */
  public static Handler chain(Action<? super Chain> action) {
    return chain(null, action);
  }

  public static Handler chain(Registry<Object> registry, Action<? super Chain> action) {
    return ChainBuilder.INSTANCE.buildHandler(new ChainActionTransformer(registry), action);
  }

  private static ImmutableList<Handler> chainList(Action<? super Chain> action) {
    return ChainBuilder.INSTANCE.buildList(new ChainActionTransformer(null), action);
  }

  /**
   * Creates a handler chain from the given handlers.
   *
   * @param handlers The handlers to connect into a chain
   * @return A new handler that is the given handlers connected into a chain
   */
  public static Handler chain(List<Handler> handlers) {
    if (handlers.size() == 0) {
      return next();
    } else if (handlers.size() == 1) {
      return handlers.get(0);
    } else {
      return new ChainHandler(ImmutableList.copyOf(handlers));
    }
  }

  public static Handler chain(Handler... handlers) {
    return chain(ImmutableList.copyOf(handlers));
  }

  /**
   * A handlers that changes the {@link org.ratpackframework.file.FileSystemBinding} for the given handlers. <p> The new file system binding will be created by the {@link
   * org.ratpackframework.file.FileSystemBinding#binding(String)} method of the contextual binding.
   *
   * @param path The relative path to the new file system binding point
   * @param handlers The handlers to execute with the new file system binding
   * @return A handler
   */
  public static Handler fileSystem(String path, List<Handler> handlers) {
    return new FileSystemBindingHandler(new File(path), ImmutableList.copyOf(handlers));
  }

  /**
   * A handler that changes the {@link org.ratpackframework.file.FileSystemBinding} for the given handler chain. <p> The new file system binding will be created by the {@link
   * org.ratpackframework.file.FileSystemBinding#binding(String)} method of the contextual binding.
   *
   * @param path The relative path to the new file system binding point
   * @param builder The definition of the handler chain
   * @return A handler
   */
  public static Handler fileSystem(String path, Action<? super Chain> builder) {
    return fileSystem(path, chainList(builder));
  }

  /**
   * A handler that serves static assets at the given file system path, relative to the contextual file system binding. <p> See {@link #assets(String, String[], Handler)} for the definition of how
   * what to serve is calculated. <p> No "index files" will be used.
   *
   * @param path The relative path to the location of the assets to serve
   * @param notFound The handler to delegate to if no file matches the request
   * @return A handler
   */
  public static Handler assets(String path, Handler notFound) {
    return assets(path, new String[0], notFound);
  }

  /**
   * A handler that serves static assets at the given file system path, relative to the contextual file system binding. <p> See {@link #assets(String, String[], Handler)} for the definition of how
   * what to serve is calculated. <p> If no file can be found to serve, the exchange will be delegated to the next handler in the chain.
   *
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   * @return A handler
   */
  public static Handler assets(String path, String... indexFiles) {
    return assets(path, indexFiles, next());
  }

  /**
   * A handler that serves static assets at the given file system path, relative to the contextual file system binding. <p> The file to serve is calculated based on the contextual {@link
   * org.ratpackframework.file.FileSystemBinding} and the contextual {@link org.ratpackframework.path.PathBinding}. The {@link org.ratpackframework.path.PathBinding#getPastBinding()} of the contextual
   * path binding is used to find a file/directory relative to the contextual file system binding. <p> If the request matches a directory, an index file may be served. The {@code indexFiles} array
   * specifies the names of files to look for in order to serve. <p> If no file can be found to serve, the exchange will be delegated to the given handler.
   *
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   * @param notFound The handler to delegate to if no file could be found to serve
   * @return A handler
   */
  public static Handler assets(String path, String[] indexFiles, final Handler notFound) {
    Handler fileHandler = FileStaticAssetRequestHandler.INSTANCE;
    Handler directoryHandler = new DirectoryStaticAssetRequestHandler(ImmutableList.<String>builder().add(indexFiles).build(), fileHandler);
    Handler contextSetter = new TargetFileStaticAssetRequestHandler(directoryHandler);

    return fileSystem(path, ImmutableList.<Handler>of(contextSetter, notFound));
  }

  /**
   * A handler that simply delegates to the next handler. <p> Effectively a noop.
   *
   * @return A handler
   */
  public static Handler next() {
    return NextHandler.INSTANCE;
  }

  /**
   * A handler that delegates to the given handler if the request is GET and matches the given path. <p> If the request is not a GET or does not match the path, the next handler in the chain will be
   * invoked. <p> If the request does match the given path but is not a GET, a 405 will be sent to the exchange's {@linkplain Context#clientError(int) client error handler}. <p> See {@link
   * #path(String, java.util.List)} for details on how the path argument is interpreted.
   *
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a GET
   * @return A handler
   */
  public static Handler get(String path, Handler handler) {
    return path(path, ImmutableList.<Handler>of(get(), handler));
  }

  /**
   * A handler that delegates to the given handler if the request is GET and the path is at the current root. <p> This is shorthand for calling {@link #get(String, Handler)} with a path of {@code
   * ""}.
   *
   * @param handler The handler to delegate to if the path matches and the request is a GET
   * @return A handler
   */
  public static Handler get(Handler handler) {
    return path("", ImmutableList.<Handler>of(get(), handler));
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
   * A handler that delegates to the given handler if the request is POST and matches the given path. <p> If the request is not a POST or does not match the path, the next handler in the chain will be
   * invoked. <p> If the request does match the given path but is not a POST, a 405 will be sent to the exchange's {@linkplain Context#clientError(int) client error handler}. <p> See {@link
   * #path(String, java.util.List)} for details on how the path argument is interpreted.
   *
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a POST
   * @return A handler
   */
  public static Handler post(String path, Handler handler) {
    return path(path, ImmutableList.<Handler>of(post(), handler));
  }

  /**
   * A handler that delegates to the given handler if the request is POST and the path is at the current root. <p> This is shorthand for calling {@link #post(String, Handler)} with a path of {@code
   * ""}.
   *
   * @param handler The handler to delegate to if the path matches and the request is a POST
   * @return A handler
   */
  public static Handler post(Handler handler) {
    return path("", ImmutableList.<Handler>of(post(), handler));
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
   * Creates a handler that delegates to the given handlers if the request path starts with the given prefix. <p> See {@link #prefix(String, List)} for the details on how {@code prefix} is
   * interpreted.
   *
   * @param prefix The path prefix to match
   * @param builder The definition of the chain to delegate to
   * @return A handler
   */
  public static Handler prefix(String prefix, Action<? super Chain> builder) {
    return prefix(prefix, chainList(builder));
  }

  /**
   * Creates a handler that delegates to the given handler if the request path starts with the given prefix. <p> See {@link #prefix(String, List)} for the details on how {@code prefix} is
   * interpreted.
   *
   * @param prefix The path prefix to match
   * @param handler The handler to delegate to
   * @return A handler
   */
  public static Handler prefix(String prefix, Handler handler) {
    return prefix(prefix, singleton(handler));
  }

  /**
   * Creates a handler that delegates to the given handlers if the request path starts with the given prefix. <p> The {@code prefix} is relative to the contextual {@link
   * org.ratpackframework.path.PathBinding} of the exchange. <p> A new contextual {@link org.ratpackframework.path.PathBinding} will be established for the given handlers, using the given prefix as
   * the bind point.
   *
   * @param prefix The path prefix to match
   * @param handlers The handlers to delegate to
   * @return A handler
   */
  public static Handler prefix(String prefix, List<Handler> handlers) {
    return path(new TokenPathBinder(prefix, false), handlers);
  }

  public static Handler prefix(String prefix, Handler... handlers) {
    return prefix(prefix, ImmutableList.copyOf(handlers));
  }

  /**
   * Creates a handler that delegates to the given handler if the request matches the given path exactly. <p> See {@link #path(String, List)} for the details on how {@code prefix} is interpreted.
   *
   * @param path The exact path to match to
   * @param handler The handler to delegate to if the path matches
   * @return A handler
   */
  public static Handler path(String path, Handler handler) {
    return path(path, singleton(handler));
  }

  /**
   * Creates a handler that delegates to the given handlers if the request matches the given path exactly. <p> The {@code path} is relative to the contextual {@link
   * org.ratpackframework.path.PathBinding} of the exchange. <p> A new contextual {@link org.ratpackframework.path.PathBinding} will be established for the given handlers, using the given path as the
   * bind point.
   *
   * @param path The exact path to match to
   * @param handlers The handlers to delegate to if the path matches
   * @return A handler
   */
  public static Handler path(String path, List<Handler> handlers) {
    return path(new TokenPathBinder(path, true), handlers);
  }

  /**
   * Creates a handler that delegates to the given handler if the request can be bound by the given path binder.
   *
   * @param pathBinder The path binder that may bind to the request path
   * @param handler The handler to delegate to if path binder does bind to the path
   * @return A handler
   */
  public static Handler path(PathBinder pathBinder, Handler handler) {
    return path(pathBinder, singleton(handler));
  }

  /**
   * Creates a handler that delegates to the given handlers if the request can be bound by the given path binder.
   *
   * @param pathBinder The path binder that may bind to the request path
   * @param handlers The handlers to delegate to if path binder does bind to the path
   * @return A handler
   */
  public static Handler path(PathBinder pathBinder, List<Handler> handlers) {
    return new PathHandler(pathBinder, ImmutableList.copyOf(handlers));
  }

}
