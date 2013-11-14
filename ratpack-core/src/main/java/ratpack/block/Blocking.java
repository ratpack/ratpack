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

package ratpack.block;

import ratpack.api.NonBlocking;
import ratpack.util.Action;

import java.util.concurrent.Callable;

/**
 * Allows blocking operations to be executed off of a main request handling thread.
 * <p>
 * Instances of {@code Blocking} can be obtained via {@link ratpack.handling.Context#getBlocking()}.
 * <p>
 * Ratpack apps typically do not use a large thread pool for handling requests. By default there is one thread per core.
 * This means that blocking IO operations cannot be done on the thread invokes a handler. Blocking IO operations must be
 * offloaded in order to free the request handling thread to handle other requests while the IO operation is performed.
 * The {@code Blocking} type makes it easy to do this.
 * <p>
 * A callable is submitted to the {@link #exec(Callable)} method. The implementation of this callable CAN block.
 * It will be executed on a non request handling thread. It should do not much more than initiate a blocking IO operation.
 * However, the callable is not executed immediately. The return value of {@link #exec(Callable)} must be used to specify
 * how to proceed after the blocking operation. The {@code then()} method must be called for the work to be performed.
 * </p>
 * Example usage (Java):
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.util.Action;
 * import java.util.concurrent.Callable;
 *
 * class MyHandler implements Handler {
 *   void handle(final Context context) {
 *     context.getBlocking().exec(new Callable&lt;String&gt;() {
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
 * to the {@link ratpack.handling.Context#error(Exception)} method of the current context.
 * Similarly, errors that occur during the result handler are forwarded.
 * </p>
 * <p>
 * To use a custom error handling strategy, use the {@link ratpack.block.Blocking.SuccessOrError#onError(ratpack.util.Action)} method
 * of the return of {@link #exec(java.util.concurrent.Callable)}.
 * </p>
 * <p>
 * Example usage (Java):
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.util.Action;
 * import java.util.concurrent.Callable;
 *
 * class MyHandler implements Handler {
 *   void handle(final Context context) {
 *     context.getBlocking().exec(new Callable&lt;String&gt;() {
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
 */
public interface Blocking {

  /**
   * Execute the given operation in a background thread pool, avoiding blocking on a request handling thread.
   *
   * @param operation The operation to perform
   * @param <T> The type of result object that the operation produces
   * @return A fluent style builder for specifying how to process the result and optionally how to deal with errors
   */
  <T> SuccessOrError<T> exec(Callable<T> operation);

  /**
   * A builder for specifying the result handling strategy for a blocking operation.
   *
   * @param <T> The type of result object that the operation produces
   */
  interface SuccessOrError<T> extends Success<T> {

    /**
     * Specifies the action to take should an error occur during the blocking operation.
     *
     * @param errorHandler The action to take
     * @return A builder for specifying the action to take when the blocking operation succeeds
     */
    Success<T> onError(Action<? super Throwable> errorHandler);
  }

  /**
   * A builder for specifying the result handling strategy for a blocking operation that succeeds.
   *
   * @param <T> The type of result object that the operation produces
   */
  interface Success<T> {

    /**
     * Specifies the success handler, and actually starts the process of having the blocking operation execute.
     *
     * @param then The action to process the result of the blocking operation.
     */
    @NonBlocking
    void then(Action<? super T> then);
  }

}
