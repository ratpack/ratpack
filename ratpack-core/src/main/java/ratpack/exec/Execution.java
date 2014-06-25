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
import ratpack.registry.MutableRegistry;

/**
 * A <em>logical</em> stream of execution, which is potentially serialized over many threads.
 * <p>
 * Ratpack is non blocking.
 * This requires that IO and other blocking operations are performed asynchronously.
 * In completely synchronous execution, the thread and the call stack serve as the representation of a stream of execution,
 * with the execution being bound to a single thread exclusively for its entire duration.
 * The {@code Execution} concept in Ratpack brings some of the characteristics of the traditional single-thread exclusive model
 * to the asynchronous, non-exclusive, environment.
 * <p>
 * A well understood example of a logical <em>stream of execution</em> in the web application environment is the handling of a request.
 * This can be thought of as a single logical operation; the request comes in and processing happens until the response is sent back.
 * Many web application frameworks exclusively assign a thread to such a stream of execution, from a large thread pool.
 * If a blocking operation is performed during the execution, the thread sits <em>waiting</em> until it can continue (e.g. the IO completes, or the contended resource becomes available).
 * Thereby the <em>segments</em> of the execution are <em>serialized</em> and the call stack provides execution context.
 * Ratpack supports the <em>non-blocking</em> model, where threads do not wait.
 * Instead of threads waiting for IO or some future event, they are returned to the “pool” to be used by other executions (and therefore the pool can be smaller).
 * When the IO completes or the contended resource becomes available, execution continues with a new call stack and possibly on a different thread.
 * <p>
 * <b>The execution object underpins an entire logical operation, even when that operation may be performed by multiple threads.</b>
 * <p>
 * The “execution” in Ratpack simulates aspects of the traditional execution context by allowing {@link #setErrorHandler(ratpack.func.Action) registration of an error handler} that is carried
 * across execution segments (and therefore threads), and {@link #onComplete(Runnable) completion notifications}.
 * <p>
 * Importantly, it also <em>serializes</em> execution segments by way of the {@link #promise(ratpack.func.Action)} method.
 * These methods are fundamentally asynchronous in that they facilitate performing operations where the result will arrive later without waiting for the result,
 * but are synchronous in the operations the perform are serialized and not executed concurrently or in parallel.
 * <em>Crucially</em>, this means that state that is local to an execution does not need to be thread safe.
 * <h4>Executions and request handling</h4>
 * <p>
 * The execution object actually underpins the {@link ratpack.handling.Context} objects that are used when handling requests.
 * It is rarely used directly when request handling, except when concurrency or parallelism is required to process data via the {@link ratpack.handling.Context#fork(ratpack.func.Action)} method.
 * Moreover, it provides its own error handling and completion mechanisms.
 * </p>
 */
public interface Execution extends MutableRegistry, ExecControl {

  /**
   * Registers an action to occur if an exception is raised during an execution segment that is uncaught, or when asynchronous operations with no defined error handler fail.
   * <pre class="java">
   * import ratpack.launch.LaunchConfigBuilder;
   * import ratpack.exec.Execution;
   * import ratpack.exec.ExecController;
   * import ratpack.func.Action;
   *
   * import java.util.concurrent.Callable;
   * import java.util.concurrent.BlockingQueue;
   * import java.util.concurrent.LinkedBlockingQueue;
   *
   * public class Example {
   *
   *   public static void main(String[] args) throws InterruptedException {
   *     final BlockingQueue&lt;String&gt; queue = new LinkedBlockingQueue&lt;&gt;();
   *
   *     ExecController controller = LaunchConfigBuilder.noBaseDir().build().getExecController();
   *
   *     controller.start(new Action&lt;Execution&gt;() {
   *       public void execute(Execution execution) {
   *         execution.setErrorHandler(new Action&lt;Throwable&gt;() {
   *           public void execute(Throwable throwable) {
   *             try {
   *               queue.put("caught be execution error handler: " + throwable.getMessage());
   *             } catch (Exception e) {
   *               // Important to not let the error handler throw an exception that would cause the error handler to be invoked again,
   *               // and throw another exception… resulting in a stack overflow.
   *               e.printStackTrace();
   *             }
   *           }
   *         });
   *
   *         execution
   *           .blocking(new Callable&lt;String&gt;() {
   *             public String call() {
   *               return "foo";
   *             }
   *           })
   *           .then(new Action&lt;String&gt;() {
   *             public void execute(String value) {
   *               throw new RuntimeException(value);
   *             }
   *           });
   *       }
   *     });
   *
   *     assert queue.take().equals("caught be execution error handler: foo");
   *   }
   * }
   * </pre>
   * <p>
   * Before the error handler is invoked, all queued execution segments are discarded.
   * <pre class="java">
   * import ratpack.launch.LaunchConfigBuilder;
   * import ratpack.exec.Execution;
   * import ratpack.exec.ExecController;
   * import ratpack.func.Action;
   *
   * import java.util.concurrent.Callable;
   * import java.util.concurrent.BlockingQueue;
   * import java.util.concurrent.LinkedBlockingQueue;
   *
   * public class Example {
   *
   *   public static void main(String[] args) throws InterruptedException {
   *     final BlockingQueue&lt;String&gt; queue = new LinkedBlockingQueue&lt;&gt;();
   *
   *     ExecController controller = LaunchConfigBuilder.noBaseDir().build().getExecController();
   *
   *     controller.start(new Action&lt;Execution&gt;() {
   *       public void execute(Execution execution) {
   *         execution.setErrorHandler(new Action&lt;Throwable&gt;() {
   *           public void execute(Throwable throwable) {
   *             try {
   *               queue.put("caught be execution error handler: " + throwable.getMessage());
   *             } catch (Exception e) {
   *               // Important to not let the error handler throw an exception that would cause the error handler to be invoked again,
   *               // and throw another exception… resulting in a stack overflow.
   *               e.printStackTrace();
   *             }
   *           }
   *         });
   *
   *         // This blocking call will never be executed as the execution segment that queued it
   *         // will throw an exception that is uncaught
   *         execution
   *           .blocking(new Callable&lt;String&gt;() {
   *             public String call() {
   *               return "foo";
   *             }
   *           })
   *           .then(new Action&lt;String&gt;() {
   *             public void execute(String value) {
   *               throw new RuntimeException("will never be executed");
   *             }
   *           });
   *
   *         throw new RuntimeException("after blocking call");
   *       }
   *     });
   *
   *     assert queue.take().equals("caught be execution error handler: after blocking call");
   *   }
   * }
   * </pre>
   * <p>
   * <b>Note:</b> it is generally not advisable to call this method for a request handling execution,
   * as Ratpack installs an error handler for such executions that delegates to {@link ratpack.handling.Context#error(Exception)}.
   * It is generally used for background and forked executions.
   * </p>
   *
   * @param errorHandler the action that should be invoked when an exception is uncaught during an execution segment.
   */
  void setErrorHandler(Action<? super Throwable> errorHandler);

  /**
   * Adds an interceptor that wraps the rest of the current execution segment and all future segments of this execution.
   * <p>
   * The given action is executed immediately (i.e. as opposed to being queued to be executed as the next execution segment).
   * Any code executed after a call to this method in the same execution segment <b>WILL NOT</b> be intercepted.
   * Therefore, it is advisable to not execute any code after calling this method in a given execution segment.
   * <p>
   * See {@link ExecInterceptor} for example use of an interceptor.
   *
   * @param execInterceptor the execution interceptor to add
   * @param continuation the rest of the code to be executed
   * @throws Exception any thrown by {@code continuation}
   * @see ExecInterceptor
   */
  void addInterceptor(ExecInterceptor execInterceptor, Action<? super Execution> continuation) throws Exception;

  /**
   * The execution controller that this execution is associated with.
   *
   * @return the execution controller that this execution is associated with
   */
  ExecController getController();

  /**
   * Registers code to be executed when the execution completes.
   * <p>
   * An execution completes when an execution segment completes (that did not queue an async operation via ({@link #promise(ratpack.func.Action)}) and there are no further segments.
   * <p>
   * Multiple callbacks can be registered with this method.
   * They will be executed in registration order.
   * <p>
   * <b>Note:</b> for request handling, it is generally more useful to use {@link ratpack.handling.Context#onClose(ratpack.func.Action)} that this method for executing code
   * at the end of the execution as it exposes the request outcome.
   *
   * @param runnable code to execute when this execution completes
   */
  void onComplete(Runnable runnable);

}
