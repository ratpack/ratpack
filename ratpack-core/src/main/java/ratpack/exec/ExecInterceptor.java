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
 * Request handling execution can be intercepted by the {@link Execution#addInterceptor(ExecInterceptor, ratpack.func.Block)} method.
 * <pre class="java">{@code
 * import ratpack.exec.ExecInterceptor;
 * import ratpack.exec.Execution;
 * import ratpack.exec.Blocking;
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
 *         return Blocking.get(() -> {
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
 * For other types of executions (e.g. background jobs), the interceptor can be registered via {@link Execution#addInterceptor(ExecInterceptor, ratpack.func.Block)}.
 *
 * @see Execution#addInterceptor(ExecInterceptor, ratpack.func.Block)
 */
@FunctionalInterface
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
   * Intercepts the execution of an execution segment.
   * <p>
   * The execution segment argument represents a unit of work of the execution.
   * <p>
   * Implementations <b>MUST</b> invoke {@code execute()} on the given execution segment block.
   *
   * @param execution the execution that this segment belongs to
   * @param execType indicates whether this segment is execution on a compute or blocking thread
   * @param executionSegment the execution segment that is to be executed
   * @throws Exception any
   */
  void intercept(Execution execution, ExecType execType, Block executionSegment) throws Exception;

}
