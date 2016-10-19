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
import ratpack.func.Block;

/**
 * Intercepts execution segments of an execution, primarily for traceability and recording metrics.
 * <p>
 * Interceptors present in the base registry will be implicitly applied to all executions.
 * Execution specific interceptors can be registered via the {@link ExecStarter#register(Action)} method when starting the execution.
 * The {@link Execution#addInterceptor(ExecInterceptor, Block)} method allows interceptors to be registered during an execution, for the rest of the execution.
 *
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
 *     ExecResult<Timer> result = ExecHarness.yieldSingle(
 *       r -> r.add(new ProcessingTimingInterceptor()), // add the interceptor to the registry
 *       e -> {
 *         Thread.sleep(100);
 *         return Blocking.get(() -> {
 *           Thread.sleep(100);
 *           return Execution.current().get(Timer.class);
 *         })
 *         .map(s -> {
 *           Thread.sleep(100);
 *           return s;
 *         });
 *       }
 *     );
 *
 *     Timer timer = result.getValue();
 *     assertTrue(timer.getBlockingTime() >= 100);
 *     assertTrue(timer.getComputeTime() >= 200);
 *   }
 * }
 * }</pre>
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
     * The execution segment is executing on a blocking thread.
     */
    BLOCKING,

    /**
     * The execution segment is executing on a compute thread.
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
