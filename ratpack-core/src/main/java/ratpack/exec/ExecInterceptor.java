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

/**
 * Intercepts execution, primarily for traceability and recording metrics.
 * <p>
 * The interception methods <i>wrap</i> the rest of the execution.
 * They receive a <i>continuation</i> (as a {@link Runnable}) that <b>must</b> be called in order for processing to proceed.
 * <p>
 * Request handling execution can be intercepted by the {@link ratpack.handling.Context#addInterceptor(ExecInterceptor, ratpack.func.Action)} method.
 * <pre class="java">
 * import ratpack.launch.LaunchConfig;
 * import ratpack.launch.LaunchConfigBuilder;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.handling.Chain;
 * import ratpack.handling.ChainAction;
 * import ratpack.http.Request;
 * import ratpack.exec.Execution;
 * import ratpack.exec.ExecInterceptor;
 * import ratpack.func.Action;
 * import ratpack.func.Actions;
 *
 * import ratpack.test.UnitTest;
 * import ratpack.test.handling.HandlingResult;
 *
 * import java.util.concurrent.Callable;
 * import java.util.concurrent.atomic.AtomicLong;
 *
 * public class Example {
 *
 *   public static class Timer {
 *     private final AtomicLong totalCompute = new AtomicLong();
 *     private final AtomicLong totalBlocking = new AtomicLong();
 *     private boolean blocking;
 *
 *     private final ThreadLocal&lt;Long&gt; startedAt = new ThreadLocal&lt;Long&gt;() {
 *       protected Long initialValue() {
 *         return 0l;
 *       }
 *     };
 *
 *     public void start(boolean blocking) {
 *       this.blocking = blocking;
 *       startedAt.set(System.currentTimeMillis());
 *     }
 *
 *     public void stop() {
 *       long startedAtTime = startedAt.get();
 *       startedAt.remove();
 *       AtomicLong counter = blocking ? totalBlocking : totalCompute;
 *       counter.addAndGet(startedAtTime &gt; 0 ? System.currentTimeMillis() - startedAtTime : 0);
 *     }
 *
 *     public long getBlockingTime() {
 *       return totalBlocking.get();
 *     }
 *
 *     public long getComputeTime() {
 *       return totalCompute.get();
 *     }
 *   }
 *
 *   public static class ProcessingTimingInterceptor implements ExecInterceptor {
 *     private final Request request;
 *
 *     public ProcessingTimingInterceptor(Request request) {
 *       this.request = request;
 *       request.register(new Timer());
 *     }
 *
 *     public void intercept(ExecInterceptor.ExecType type, Runnable continuation) {
 *       Timer timer = request.get(Timer.class);
 *       timer.start(type.equals(ExecInterceptor.ExecType.BLOCKING));
 *       continuation.run();
 *       timer.stop();
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     Action&lt;Chain&gt; handlers = new ChainAction() {
 *       protected void execute() {
 *         handler(new Handler() {
 *           public void handle(final Context context) throws Exception {
 *             context.addInterceptor(new ProcessingTimingInterceptor(context.getRequest()), new Action&lt;Execution&gt;() {
 *               public void execute(Execution execution) {
 *                 context.next();
 *               }
 *             });
 *           }
 *         });
 *
 *         handler(new Handler() {
 *           public void handle(final Context context) throws Exception {
 *             Thread.currentThread().sleep(100);
 *             context
 *               .blocking(new Callable&lt;String&gt;() {
 *                 public String call() throws Exception {
 *                   Thread.currentThread().sleep(100);
 *                   return "foo";
 *                 }
 *               })
 *               .then(new Action&lt;String&gt;() {
 *                 public void execute(String string)  throws Exception {
 *                   Thread.currentThread().sleep(100);
 *                   context.render(string);
 *                 }
 *               });
 *           }
 *         });
 *       }
 *     };
 *
 *     HandlingResult result = UnitTest.handle(handlers, Actions.noop());
 *
 *     assert result.rendered(String.class).equals("foo");
 *
 *     Timer timer = result.getRequestRegistry().get(Timer.class);
 *     assert timer.getBlockingTime() &gt;= 100;
 *     assert timer.getComputeTime() &gt;= 200;
 *   }
 * }
 * </pre>
 * For other types of executions (e.g. background jobs), the interceptor can be registered via {@link ratpack.handling.Context#addInterceptor(ExecInterceptor, ratpack.func.Action)}.
 *
 * @see Execution
 * @see ratpack.handling.Context#addInterceptor(ExecInterceptor, ratpack.func.Action)
 */
public interface ExecInterceptor {

  /**
   * The execution type (i.e. type of thread).
   */
  enum ExecType {

    /**
     * The execution is performing blocking IO.
     */
    BLOCKING,

    /**
     * The execution is performing computation (i.e. it is not blocking)
     */
    COMPUTE
  }

  /**
   * Intercepts the “rest” of the execution on the current thread.
   * <p>
   * The given {@code Runnable} argument represents the rest of the execution to occur on this thread.
   * This does not necessarily mean the rest of the execution until the work (e.g. responding to a request) is complete.
   * Execution may involve multiple parallel (but not concurrent) threads of execution because of blocking IO or asynchronous APIs.
   * <p>
   * All exceptions thrown by this method will be <b>ignored</b>.
   *  @param execType indicates whether this is a compute (e.g. request handling) or blocking IO thread
   * @param continuation the “rest” of the execution
   */
  void intercept(ExecType execType, Runnable continuation);

}
