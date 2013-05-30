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

import org.ratpackframework.file.internal.DirectoryStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileSystemContextHandler;
import org.ratpackframework.file.internal.TargetFileStaticAssetRequestHandler;
import org.ratpackframework.handling.internal.*;
import org.ratpackframework.http.internal.MethodHandler;
import org.ratpackframework.path.PathBinder;
import org.ratpackframework.path.internal.PathHandler;
import org.ratpackframework.path.internal.TokenPathBinder;
import org.ratpackframework.util.Action;

import java.io.File;
import java.util.Arrays;

/**
 * Factory methods for certain types of handlers.
 * <p>
 * Typically used by {@link Chain} implementations to build a handler chain.
 * <pre class="groovyTestCase">
 * import static org.ratpackframework.handling.Handlers.*;
 * import org.ratpackframework.handling.Handler;
 * import org.ratpackframework.handling.Chain;
 * import org.ratpackframework.handling.Exchange;
 * import org.ratpackframework.util.Action;
 *
 * class ExampleHandler implements Handler {
 *   void handle(Exchange exchange) {
 *     // …
 *   }
 * }
 *
 * class ChainBuilder implements Action&lt;Chain&gt; {
 *   void execute(Chain chain) {
 *     chain.add(assets("public"));
 *     chain.add(get("info", new ExampleHandler()));
 *     chain.add(path("api", new Action&lt;Chain&gt;() {
 *       void execute(Chain apiChain) {
 *         apiChain.add(get("version", new ExampleHandler()));
 *         apiChain.add(get("log", new ExampleHandler()));
 *       }
 *     }));
 *   }
 * }
 * 0;
 * </pre>
 */
public abstract class Handlers {

  private Handlers() {}

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given context addition.
   * <p>
   * The context object will be available by its concrete type.
   * To make it available by a different type (perhaps one of its interfaces) use {@link #context(Class, Object, org.ratpackframework.util.Action)}.
   *
   * @param object The object to add to the context, only for the handlers defined by {@code builder}
   * @param builder The definition of the handler chain to insert with the context
   * @return A handler
   */
  public static Handler context(Object object, Action<? super Chain> builder) {
    return context(object, chain(builder));
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given context addition.
   *
   * @param type The type by which to make the context addition available
   * @param object The object to add to the context, only for the handlers defined by {@code builder}
   * @param builder The definition of the handler chain to insert with the context
   * @return A handler
   */
  public static <T> Handler context(Class<? super T> type, T object, Action<? super Chain> builder) {
    return context(type, object, chain(builder));
  }

  /**
   * Creates a handler that inserts the given handler with the given context addition.
   * <p>
   * The context object will be available by its concrete type.
   * To make it available by a different type (perhaps one of its interfaces) use {@link #context(Class, Object, Handler)}.
   *
   * @param object The object to add to the context for the handler
   * @param handler The handler to make the context addition available for
   * @return A handler
   */
  public static Handler context(Object object, Handler handler) {
    return new ContextInsertingHandler(object, handler);
  }

  /**
   * Creates a handler that inserts the handler chain defined by the builder, with the given context addition.
   *
   * @param type The type by which to make the context addition available
   * @param object The object to add to the context, only for the handlers defined by {@code builder}
   * @param handler The handler to
   * @return A handler
   */
  public static <T> Handler context(Class<? super T> type, T object, Handler handler) {
    return new ContextInsertingHandler(type, object, handler);
  }

  /**
   * Builds a handler chain.
   *
   * @param action The chain definition
   * @return A handler
   */
  public static Handler chain(Action<? super Chain> action) {
    return ChainBuilder.INSTANCE.build(ChainActionTransformer.INSTANCE, action);
  }

  public static Handler chain(final Handler... handlers) {
    return new ChainHandler(Arrays.asList(handlers));
  }

  /**
   * A handler that changes the {@link org.ratpackframework.file.FileSystemBinding} for the given handler.
   * <p>
   * The new file system binding will be created by the {@link org.ratpackframework.file.FileSystemBinding#binding(String)} method of the contextual binding.
   *
   * @param path The relative path to the new file system binding point
   * @param handler The handler to execute with the new file system binding
   * @return A handler
   */
  public static Handler fileSystem(String path, Handler handler) {
    return new FileSystemContextHandler(new File(path), handler);
  }

  /**
   * A handler that changes the {@link org.ratpackframework.file.FileSystemBinding} for the given handler chain.
   * <p>
   * The new file system binding will be created by the {@link org.ratpackframework.file.FileSystemBinding#binding(String)} method of the contextual binding.
   *
   * @param path The relative path to the new file system binding point
   * @param builder The definition of the handler chain
   * @return A handler
   */
  public static Handler fileSystem(String path, Action<? super Chain> builder) {
    return fileSystem(path, chain(builder));
  }

  /**
   * A handler that serves static assets at the given file system path, relative to the contextual file system binding.
   * <p>
   * See {@link #assets(String, String[], Handler)} for the definition of how what to serve is calculated.
   * <p>
   * No "index files" will be used.
   *
   * @param path The relative path to the location of the assets to serve
   * @param notFound The handler to delegate to if no file matches the request
   * @return A handler
   */
  public static Handler assets(String path, Handler notFound) {
    return assets(path, new String[0], notFound);
  }

  /**
   * A handler that serves static assets at the given file system path, relative to the contextual file system binding.
   * <p>
   * See {@link #assets(String, String[], Handler)} for the definition of how what to serve is calculated.
   * <p>
   * If no file can be found to serve, the exchange will be delegated to the next handler in the chain.
   *
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   * @return A handler
   */
  public static Handler assets(String path, String... indexFiles) {
    return assets(path, indexFiles, next());
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
   * If no file can be found to serve, the exchange will be delegated to the given handler.
   *
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   * @param notFound The handler to delegate to if no file could be found to serve
   * @return A handler
   */
  public static Handler assets(String path, String[] indexFiles, final Handler notFound) {
    Handler fileHandler = FileStaticAssetRequestHandler.INSTANCE;
    Handler directoryHandler = new DirectoryStaticAssetRequestHandler(Arrays.asList(indexFiles), fileHandler);
    Handler contextSetter = new TargetFileStaticAssetRequestHandler(directoryHandler);

    return fileSystem(path, chain(contextSetter, notFound));
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

  public static Handler get(String path, Handler handler) {
    return path(path, chain(MethodHandler.GET, handler));
  }

  public static Handler get(Handler handler) {
    return path("", chain(MethodHandler.GET, handler));
  }

  public static Handler post(String path, Handler handler) {
    return path(path, chain(MethodHandler.POST, handler));
  }

  public static Handler path(String path, Action<? super Chain> builder) {
    return path(path, chain(builder));
  }

  public static Handler path(String path, Handler handler) {
    return pathBinding(new TokenPathBinder(path, false), handler);
  }

  public static Handler handler(String path, Handler handler) {
    return pathBinding(new TokenPathBinder(path, true), handler);
  }

  public static Handler pathBinding(PathBinder pathBinder, Handler handler) {
    return new PathHandler(pathBinder, handler);
  }

}
