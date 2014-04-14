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
 * The following example (in Groovy) demonstrates using a processing interceptor to time processing.
 * <pre class="tested">
 * import ratpack.launch.LaunchConfig
 * import ratpack.launch.LaunchConfigBuilder
 * import ratpack.handling.Context
 * import ratpack.http.Request
 * import ratpack.exec.ExecInterceptor
 * import ratpack.test.embed.LaunchConfigEmbeddedApplication
 *
 * import static ratpack.groovy.Groovy.chain
 * import static ratpack.groovy.test.TestHttpClients.testHttpClient
 *
 * import java.util.concurrent.atomic.AtomicLong
 *
 * class Timer {
 *   private final AtomicLong totalCompute = new AtomicLong()
 *   private final AtomicLong totalBlocking = new AtomicLong()
 *
 *   private boolean blocking
 *
 *   private final ThreadLocal&lt;Long&gt; startedAt = new ThreadLocal() {
 *     protected Long initialValue() { 0 }
 *   }
 *
 *   void start(boolean blocking) {
 *     this.blocking = blocking
 *     startedAt.set(System.currentTimeMillis())
 *   }
 *
 *   void stop() {
 *     def startedAtTime = startedAt.get()
 *     startedAt.remove()
 *     def counter = blocking ? totalBlocking : totalCompute
 *     counter.addAndGet(startedAtTime > 0 ? System.currentTimeMillis() - startedAtTime : 0)
 *   }
 *
 *   long getBlockingTime() {
 *     totalBlocking.get()
 *   }
 *
 *   long getComputeTime() {
 *     totalCompute.get()
 *   }
 * }
 *
 * class ProcessingTimingInterceptor implements ExecInterceptor {
 *
 *   final Request request
 *
 *   ProcessingTimingInterceptor(Request request) {
 *     this.request = request
 *     request.register(new Timer())
 *   }
 *
 *   void intercept(ExecInterceptor.ExecType type, Runnable continuation) {
 *     request.get(Timer).with {
 *       start(type == ExecInterceptor.ExecType.BLOCKING)
 *       continuation.run()
 *       stop()
 *     }
 *   }
 * }
 *
 * import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
 *
 * def app = embeddedApp {
 *   handlers {
 *     handler {
 *       addExecInterceptor(new ProcessingTimingInterceptor(request)) {
 *         next()
 *       }
 *     }
 *     handler {
 *       sleep 100
 *       next()
 *     }
 *     get {
 *       sleep 100
 *       background {
 *         sleep 100
 *       } then {
 *         def timer = request.get(Timer)
 *         timer.stop()
 *         render "$timer.computeTime:$timer.blockingTime"
 *       }
 *     }
 *   }
 * }
 *
 * def client = testHttpClient(app)
 * try {
 *   def times = client.getText().split(":")*.toInteger()
 *   int computeTime = times[0]
 *   int blockingTime = times[1]
 *
 *   assert computeTime >= 200
 *   assert blockingTime >= 100
 * } finally {
 *   app.close()
 * }
 * </pre>
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
