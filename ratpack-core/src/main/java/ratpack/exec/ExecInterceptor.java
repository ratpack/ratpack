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
 * Request handling execution can be intercepted by the {@link ExecControl#addInterceptor(ExecInterceptor, ratpack.func.NoArgAction)} method.
 * <pre class="java">{@code
 * import ratpack.exec.ExecInterceptor;
 * import ratpack.http.Request;
 * import ratpack.test.UnitTest;
 * import ratpack.test.handling.HandlingResult;
 *
 * import java.util.concurrent.atomic.AtomicLong;
 *
 * import static java.lang.Thread.sleep;
 *
 * public class Example {
 *
 *   public static class Timer {
 *     private final AtomicLong totalCompute = new AtomicLong();
 *     private final AtomicLong totalBlocking = new AtomicLong();
 *     private boolean blocking;
 *
 *     private final ThreadLocal<Long> startedAt = ThreadLocal.withInitial(() -> 0l);
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
 *       counter.addAndGet(startedAtTime > 0 ? System.currentTimeMillis() - startedAtTime : 0);
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
 *       request.add(new Timer());
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
 *     HandlingResult result = UnitTest.requestFixture().handleChain(chain -> chain
 *         .handler(context ->
 *             context.addInterceptor(new ProcessingTimingInterceptor(context.getRequest()), () -> context.next())
 *         )
 *         .handler(context -> {
 *           sleep(100);
 *           context.blocking(() -> {
 *             sleep(100);
 *             return "foo";
 *           }).then(string -> {
 *             sleep(100);
 *             context.render(string);
 *           });
 *         })
 *     );
 *
 *     assert result.rendered(String.class).equals("foo");
 *
 *     Timer timer = result.getRequestRegistry().get(Timer.class);
 *     assert timer.getBlockingTime() >= 100;
 *     assert timer.getComputeTime() >= 200;
 *   }
 * }
 * }</pre>
 * For other types of executions (e.g. background jobs), the interceptor can be registered via {@link ExecControl#addInterceptor(ExecInterceptor, ratpack.func.NoArgAction)}.
 *
 * @see Execution
 * @see ExecControl#addInterceptor(ExecInterceptor, ratpack.func.NoArgAction)
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
