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

package ratpack.exec;

import ratpack.api.NonBlocking;

/**
 * A fulfiller of an asynchronous promise.
 * <p>
 * This type is used to integrate with asynchronous APIs, via the {@link ExecControl#promise(ratpack.func.Action)} method.
 * The following example shows usage during request processing.
 * <pre class="tested">
 * import ratpack.handling.InjectionHandler;
 * import ratpack.handling.Context;
 * import ratpack.exec.Promise;
 * import ratpack.exec.Fulfiller;
 * import ratpack.func.Action;
 *
 * // Some 3rd party asynchronous API
 * public interface SomeAsyncApi {
 *   interface AsyncCallback {
 *     void onSuccess(String result);
 *     void onError(IOException e);
 *   }
 *
 *   void doSomeAsyncOperation(AsyncCallback callback);
 * }
 *
 * public class AsyncApiUsingHandler extends InjectionHandler {
 *   void handle(final Context context, final SomeAsyncApi asyncApi) {
 *     Promise&lt;String&gt; promise = context.promise(new Action&lt;Fulfiller&lt;String&gt;&gt;() {
 *       public void execute(final Fulfiller&lt;String&gt; fulfiller) {
 *         asyncApi.doSomeAsyncOperation(new SomeAsyncApi.AsyncCallback() {
 *           public void onSuccess(String result) {
 *             fulfiller.success(result);
 *           }
 *           public void onError(IOException e) {
 *             fulfiller.error(e);
 *           }
 *         });
 *       }
 *     });
 *
 *     promise.then(new Action&lt;String&gt;() {
 *       public void execute(String string) {
 *         context.render(string);
 *       }
 *     });
 *   }
 * }
 * </pre>
 * <p>
 * The methods of this method may be executed asynchronously.
 * That is, the promise subscription my be invoked in a separate thread.
 *
 * @param <T> the type of value that was promised.
 * @see ExecControl#promise(ratpack.func.Action)
 * @see Promise
 * @see Fulfillment
 */
public interface Fulfiller<T> {

  /**
   * Fulfills the promise with an error result.
   *
   * @param throwable the error result
   */
  @NonBlocking
  void error(Throwable throwable);

  /**
   * Fulfills the promise with the given value.
   *
   * @param value the value to provide to the promise subscriber
   */
  @NonBlocking
  void success(T value);

}
