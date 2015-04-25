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

import ratpack.func.Block;

/**
 * Intercepts execution, primarily for traceability and recording metrics.
 * <p>
 * The interception methods <i>wrap</i> the rest of the execution.
 * They receive a <i>continuation</i> (as a {@link Runnable}) that <b>must</b> be called in order for processing to proceed.
 * <p>
 * Request handling execution can be intercepted by the {@link ExecControl#addInterceptor(ExecInterceptor, ratpack.func.Block)} method.
 * <pre class="java">{@code
 * import ratpack.exec.ExecInterceptor;
 * import ratpack.exec.Execution;
 * import ratpack.exec.ExecResult;
 * import ratpack.func.Block;
 * import ratpack.test.exec.ExecHarness;
 *
 * import static java.lang.Thread.sleep;
 * import static org.junit.Assert.assertEquals;
 * import static org.junit.Assert.assertTrue;
 *
 * public class Example {
 *
 *   public static class Timer {
 *     private long totalCompute;
 *     private long totalBlocking;
 *     private boolean blocking;
 *
 *     private long startedAt;
 *
 *     public void start(boolean blocking) {
 *       this.blocking = blocking;
 *       startedAt = System.currentTimeMillis();
 *     }
 *
 *     public void stop() {
 *       long duration = System.currentTimeMillis() - startedAt;
 *       if (blocking) {
 *         totalBlocking += duration;
 *       } else {
 *         totalCompute += duration;
 *       }
 *     }
 *
 *     public long getBlockingTime() {
 *       return totalBlocking;
 *     }
 *
 *     public long getComputeTime() {
 *       return totalCompute;
 *     }
 *   }
 *
 *   public static class ProcessingTimingInterceptor implements ExecInterceptor {
 *     public void intercept(Execution execution, ExecInterceptor.ExecType type, Block continuation) throws Exception {
 *       Timer timer = execution.maybeGet(Timer.class).orElse(null);
 *       if (timer == null) { // this is the first execution segment
 *         timer = new Timer();
 *         execution.add(Timer.class, timer);
 *       }
 *
 *       timer.start(type.equals(ExecInterceptor.ExecType.BLOCKING));
 *       try {
 *         continuation.execute();
 *       } finally {
 *         timer.stop();
 *       }
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     ExecResult<String> result = ExecHarness.yieldSingle(
 *       r -> r.add(new ProcessingTimingInterceptor()), // add the interceptor to the registry
 *       e -> {
 *         Thread.sleep(100);
 *         return e.blocking(() -> {
 *           Thread.sleep(100);
 *           return "foo";
 *         })
 *         .map(s -> {
 *           Thread.sleep(100);
 *           return s.toUpperCase();
 *         });
 *       }
 *     );
 *
 *     assertEquals("FOO", result.getValue());
 *
 *     Timer timer = result.getRegistry().get(Timer.class);
 *     assertTrue(timer.getBlockingTime() >= 100);
 *     assertTrue(timer.getComputeTime() >= 200);
 *   }
 * }
 * }</pre>
 * For other types of executions (e.g. background jobs), the interceptor can be registered via {@link ExecControl#addInterceptor(ExecInterceptor, ratpack.func.Block)}.
 *
 * @see Execution
 * @see ExecControl#addInterceptor(ExecInterceptor, ratpack.func.Block)
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
   * The given action argument represents the rest of the execution to occur on this thread.
   * This does not necessarily mean the rest of the execution until the work (e.g. responding to a request) is complete.
   * Execution may involve multiple parallel (but not concurrent) threads of execution because of blocking IO or asynchronous APIs.
   *
   * @param execution the execution who's segment is being intercepted
   * @param execType indicates whether this is a compute (e.g. request handling) segment or blocking segment
   * @param continuation the “rest” of the execution
   * @throws Exception any
   */
  void intercept(Execution execution, ExecType execType, Block continuation) throws Exception;

}
