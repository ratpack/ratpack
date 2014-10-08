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

package ratpack.test.exec;

import ratpack.exec.ExecControl;
import ratpack.exec.ExecStarter;
import ratpack.exec.Promise;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.test.exec.internal.DefaultExecHarness;

/**
 * A utility for testing asynchronous support/service code.
 * <p>
 * An execution harness is backed by a thread pool.
 * It is important to call {@link #close()} when the object is no longer needed to shutdown this thread pool.
 */
public interface ExecHarness extends AutoCloseable {

  /**
   * Creates a new execution harness.
   * <pre class="java">{@code
   * import ratpack.exec.ExecControl;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * public class Example {
   *
   *   // An async callback based API
   *   static class AsyncApi {
   *
   *     static interface Callback<T> {
   *       void receive(T value);
   *     }
   *
   *     public <T> void returnAsync(T value, Callback<? super T> callback) {
   *       new Thread(() -> callback.receive(value)).run();
   *     }
   *   }
   *
   *   // Our service class that wraps the raw async API
   *   // In the real app this is created by the DI container (e.g. Guice)
   *   static class AsyncService {
   *     private final ExecControl execControl;
   *     private final AsyncApi asyncApi = new AsyncApi();
   *
   *     public AsyncService(ExecControl execControl) {
   *       this.execControl = execControl;
   *     }
   *
   *     // Our method under test
   *     public <T> Promise<T> promise(final T value) {
   *       return execControl.promise(fulfiller -> asyncApi.returnAsync(value, fulfiller::success));
   *     }
   *   }
   *
   *   public static void main(String[] args) throws Throwable {
   *     // the harness must be close()'d when finished with to free resources
   *     try (ExecHarness harness = ExecHarness.harness()) {
   *
   *       // set up the code under test using the exec control from the harness
   *       final AsyncService service = new AsyncService(harness.getControl());
   *
   *       // exercise the async code using the harness, blocking until the promised value is available
   *       ExecResult<String> result = harness.yield(execution -> service.promise("foo"));
   *
   *       assert result.getValue().equals("foo");
   *     }
   *   }
   * }
   * }</pre>
   *
   * @return a new execution harness
   */
  public static ExecHarness harness() {
    return new DefaultExecHarness(new DefaultExecController());
  }

  /**
   * Synchronously returns a promised value.
   * <p>
   * The given function will execute in a separate thread.
   * The calling thread will block, waiting for the promised value to be provided.
   *
   * @param func a function that exercises some code that returns a promise
   * @param <T> the type of promised value
   * @return the result of the execution
   * @throws Exception any thrown by the function, or the promise failure exception
   */
  public <T> ExecResult<T> yield(Function<ExecControl, Promise<T>> func) throws Exception;

  /**
   * Creates an exec harness, {@link #yield(Function) executes} the given function with it before closing it, then returning execution result.
   *
   * @param func a function that exercises some code that returns a promise
   * @param <T> the type of promised value
   * @return the result of the execution
   * @throws Exception any thrown by the function, or the promise failure exception
   */
  static public <T> ExecResult<T> yieldSingle(Function<ExecControl, Promise<T>> func) throws Exception {
    try (ExecHarness harness = harness()) {
      return harness.yield(func);
    }
  }

  public void run(Action<? super ExecControl> action) throws Exception;

  static public void runSingle(Action<? super ExecControl> action) throws Exception {
    try (ExecHarness harness = harness()) {
      harness.run(action);
    }
  }

  /**
   * The execution control for the harness.
   * <p>
   * This is typically given to the code under test to perform the async ops.
   *
   * @return an execution control.
   */
  public ExecControl getControl();

  default ExecStarter exec() {
    return getControl().exec();
  }

  /**
   * Shuts down the thread pool backing this harness.
   */
  @Override
  void close();

}
