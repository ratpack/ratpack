/*
 * Copyright 2014 the original author or authors.
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

import ratpack.api.NonBlocking;
import ratpack.func.Action;
import ratpack.http.Request;
import ratpack.path.PathTokens;
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.server.BindAddress;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * A context that does not provide access to the {@link ratpack.http.Response} or flow control methods.
 */
public interface ReadOnlyContext extends Registry {

  /**
   * Returns this.
   *
   * @return this.
   */
  ReadOnlyContext getContext();

  /**
   * The HTTP request.
   *
   * @return The HTTP request.
   */
  Request getRequest();

  /**
   * Forwards the exception to the {@link ratpack.error.ServerErrorHandler} in this service.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.error.ServerErrorHandler} in all contexts.
   * A {@link NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param exception The exception that occurred
   * @throws NotInRegistryException if no {@link ratpack.error.ServerErrorHandler} can be found in the service
   */
  @NonBlocking
  void error(Exception exception) throws NotInRegistryException;

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
   * Gets the file relative to the contextual {@link ratpack.file.FileSystemBinding}.
   * <p>
   * Shorthand for {@code get(FileSystemBinding.class).file(path)}.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.file.FileSystemBinding} in all contexts.
   * A {@link NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   *
   * @param path The path to pass to the {@link ratpack.file.FileSystemBinding#file(String)} method.
   * @return The file relative to the contextual {@link ratpack.file.FileSystemBinding}
   * @throws NotInRegistryException if there is no {@link ratpack.file.FileSystemBinding} in the current service
   */
  Path file(String path) throws NotInRegistryException;

  /**
   * An object to be used when executing blocking IO, or long operations.
   *
   * @return An object to be used when executing blocking IO, or long operations.
   * @see #background(java.util.concurrent.Callable)
   */
  Background getBackground();

  /**
   * The application foreground.
   *
   * @return the application foreground
   * @see ratpack.handling.Foreground
   */
  Foreground getForeground();

  /**
   * Perform a blocking operation, off the request thread.
   * <p>
   * Ratpack apps typically do not use a large thread pool for handling requests. By default there is about one thread per core.
   * This means that blocking IO operations cannot be done on the thread invokes a handler. Background IO operations must be
   * offloaded in order to free the request handling thread to handle other requests while the IO operation is performed.
   * The {@code Background} object makes it easy to do this.
   * <p>
   * A callable is submitted to the {@link ratpack.handling.Background#exec(java.util.concurrent.Callable)} method. The implementation of this callable <b>can</b> background
   * as it will be executed on a non request handling thread. It should do not much more than initiate a blocking IO operation and return the result.
   * <p>
   * However, the callable is not executed immediately. The return value of {@link ratpack.handling.Background#exec(java.util.concurrent.Callable)} must be used to specify
   * how to proceed after the blocking operation. The {@code then()} method must be called for the work to be performed.
   * </p>
   * Example usage (Java):
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.func.Action;
   * import java.util.concurrent.Callable;
   *
   * class MyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.background(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "foo";
   *        }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.getResponse().send(result);
   *       }
   *     });
   *   }
   * }
   * </pre>
   *
   * <h4>Error Handling</h4>
   * <p>
   * Unless otherwise specified, any exceptions that are raised during the blocking operation callable are forwarded
   * to the {@link Context#error(Exception)} method of the current context.
   * Similarly, errors that occur during the result handler are forwarded.
   * </p>
   * <p>
   * To use a custom error handling strategy, use the {@link ratpack.promise.SuccessOrErrorPromise#onError(ratpack.func.Action)} method
   * of the return of {@link ratpack.handling.Background#exec(java.util.concurrent.Callable)}.
   * </p>
   * <p>
   * Example usage:
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.func.Action;
   * import java.util.concurrent.Callable;
   *
   * class MyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.background(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "foo";
   *        }
   *     }).onError(new Action&lt;Exception&gt;() {
   *       public void execute(Exception exception) {
   *         // do something with the exception
   *       }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.getResponse().send(result);
   *       }
   *     });
   *   }
   * }
   * </pre>
   *
   * @param backgroundOperation The blocking operation to perform off of the request thread
   * @param <T> The type of object returned by the background operation
   * @return A builder for specifying the result handling strategy for a blocking operation.
   * @see #getBackground()
   */
  <T> SuccessOrErrorPromise<T> background(Callable<T> backgroundOperation);

  /**
   * The address that this request was received on.
   *
   * @return The address that this request was received on.
   */
  BindAddress getBindAddress();

  /**
   * Registers a callback to be notified when the request for this context is “closed” (i.e. responded to).
   *
   * @param onClose A notification callback
   */
  void onClose(Action<? super RequestOutcome> onClose);
}
