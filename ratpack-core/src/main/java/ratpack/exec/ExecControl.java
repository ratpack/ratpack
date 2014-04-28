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

import ratpack.func.Action;

import java.util.concurrent.Callable;

/**
 * The execution control can be used by supporting services to perform asynchronous or blocking operations.
 * <p>
 * There is a single instance for an entire application.
 * It can therefore be constructor injected into services that scope several requests (i.e. execution contexts).
 * <pre class="java">
 * import ratpack.exec.ExecControl;
 * import ratpack.exec.Promise;
 * import ratpack.exec.Fulfillment;
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.handling.ChainAction;
 * import ratpack.func.Action;
 * import ratpack.func.Actions;
 *
 * import ratpack.test.UnitTest;
 * import ratpack.test.handling.HandlingResult;
 *
 * public class Example {
 *
 *   public static class AsyncUpperCaseService {
 *     private final ExecControl control;
 *
 *     public AsyncUpperCaseService(ExecControl control) {
 *       this.control = control;
 *     }
 *
 *     public Promise&lt;String&gt; toUpper(final String lower) {
 *       return control.promise(new Fulfillment&lt;String&gt;() {
 *         protected void execute() {
 *           success(lower.toUpperCase());
 *         }
 *       });
 *     }
 *   }
 *
 *   public static class ServiceUsingHandler implements Handler {
 *     private final AsyncUpperCaseService service;
 *
 *     public ServiceUsingHandler(AsyncUpperCaseService service) {
 *       this.service = service;
 *     }
 *
 *     public void handle(final Context context) {
 *       service.toUpper("foo").then(new Action&lt;String&gt;() {
 *         public void execute(String string) {
 *           context.render(string);
 *         }
 *       });
 *     }
 *   }
 *
 *   public static void main(String[] args) {
 *     HandlingResult result = UnitTest.handle(
 *       new ChainAction() {
 *         protected void execute() {
 *           ExecControl control = getLaunchConfig().getExecController().getControl();
 *           AsyncUpperCaseService service = new AsyncUpperCaseService(control);
 *           Handler handler = new ServiceUsingHandler(service);
 *
 *           get(handler);
 *         }
 *       },
 *       Actions.noop()
 *     );
 *
 *     assert result.rendered(String.class).equals("FOO");
 *   }
 * }
 * </pre>
 * <p>
 * <b>Note:</b> when using the Guice integration, the exec control is made available for injection.
 */
public interface ExecControl {

  /**
   * Performs a blocking operation on a separate thread, returning a promise for its value.
   *
   * @param blockingOperation the operation to perform that performs blocking IO
   * @param <T> the type of value created by the operation
   * @return a promise for the return value of the given blocking operation
   */
  <T> Promise<T> blocking(Callable<T> blockingOperation);

  /**
   * Creates a promise for an asynchronously created value.
   * <p>
   * This method can be used to integrate with APIs that produce values asynchronously.
   * The asynchronous API should be invoked during the execute method of the action given to this method.
   * The result of the asynchronous call is then given to the {@link Fulfiller} that the action is given.
   *
   * @param action an action that invokes an asynchronous API, forwarding the result to the given fulfiller.
   * @param <T> the type of value
   * @return a promise for the asynchronously created value
   * @see Fulfiller
   * @see Fulfillment
   */
  <T> Promise<T> promise(Action<? super Fulfiller<T>> action);

}
