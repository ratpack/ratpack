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
 * Provides methods for controlling execution(i.e. blocking, forking and calling async API), independent of the current execution.
 *
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
   * <p>
   * This method should be used to perform blocking IO, or to perform any operation that synchronously waits for something to happen.
   * The given {@code blockingOperation} will be performed on a thread from a special thread pool for such operations
   * (i.e. not a thread from the main compute event loop).
   * <p>
   * The operation should do as little computation as possible.
   * It should just perform the blocking operation and immediately return the result.
   * Performing computation during the operation will degrade performance.
   * <p>
   * This method is just a specialization of {@link #promise}, and shares all of the same semantics with regard to
   * execution binding and execution-on-promise-subscription.
   * <pre class="java">
   * import ratpack.launch.LaunchConfigBuilder;
   * import ratpack.func.Action;
   * import ratpack.exec.Execution;
   * import ratpack.exec.ExecController;
   *
   * import java.util.concurrent.Callable;
   * import java.util.concurrent.CountDownLatch;
   *
   * public class Example {
   *
   *   public static void main(String[] args) throws InterruptedException {
   *     ExecController controller = LaunchConfigBuilder.noBaseDir().build().getExecController();
   *
   *     final CountDownLatch latch = new CountDownLatch(1);
   *
   *     controller.start(new Action&lt;Execution&gt;() {
   *       public void execute(Execution execution) {
   *         execution
   *           .blocking(new Callable&lt;String&gt;() {
   *             public String call() {
   *               // perform a blocking op, e.g. a database call, filesystem read etc.
   *               return "foo";
   *             }
   *           })
   *           .then(new Action&lt;String&gt;() {
   *             public void execute(String string) {
   *               // do something with the value that was obtained from a blocking operation
   *               latch.countDown();
   *             }
   *           });
   *       }
   *     });
   *
   *     latch.await();
   *   }
   * }
   * </pre>
   *
   * @param blockingOperation the operation that blocks
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

  /**
   * Forks a new execution on a separate thread.
   * <p>
   * This is similar to using {@code new Thread().run()} except that the action will be executed
   * on a Ratpack managed thread, and will use Ratpack's execution semantics.
   * <p>
   * This is functionally equivalent to {@link ExecController#start(ratpack.func.Action)}.
   *
   * @param action the initial execution segment
   */
  void fork(Action<? super Execution> action);

}
