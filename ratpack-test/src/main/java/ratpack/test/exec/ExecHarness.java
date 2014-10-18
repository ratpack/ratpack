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

import org.reactivestreams.Publisher;
import ratpack.exec.*;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.test.exec.internal.DefaultExecHarness;

import java.util.concurrent.Callable;

/**
 * A utility for testing asynchronous support/service code.
 * <p>
 * An execution harness is backed by a thread pool.
 * It is important to call {@link #close()} when the object is no longer needed to shutdown this thread pool.
 * Alternatively, if you are performing a single operation you can use one of the {@code *single} static methods.
 *
 * @see #yield(Function)
 * @see #yieldSingle(Function)
 * @see #run(Action)
 * @see #runSingle(Action)
 */
public interface ExecHarness extends ExecControl, AutoCloseable {

  /**
   * Creates a new execution harness.
   *
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
   * When using Ratpack's RxJava integration, ExecHarness can be used to test {@code rx.Observable} instances by first converting them to a promise.
   * See the {@code ratpack.rx.RxRatpack.asPromise(Observable)} documentation for an example of testing observables.
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

  /**
   * Initiates an execution and blocks until it completes.
   *
   * If an uncaught exception is thrown during the execution, it will be thrown by this method.
   * <p>
   * This method is useful for testing an execution that has some detectable side effect, as this method does not return the “result” of the execution.
   *
   * @param action the start of the execution
   * @throws Exception any thrown during the execution that is not explicitly caught
   * @see #runSingle(Action)
   * @see #yield(Function)
   */
  public void run(Action<? super ExecControl> action) throws Exception;

  /**
   * Convenient form of {@link #run(Action)} that creates and closes a harness for the run.
   *
   * @param action the start of the execution
   * @throws Exception any thrown during the execution that is not explicitly caught
   * @see #run(Action)
   * @see #yield(Function)
   */
  static public void runSingle(Action<? super ExecControl> action) throws Exception {
    try (ExecHarness harness = harness()) {
      harness.run(action);
    }
  }

  /**
   * The execution control for the harness.
   * <p>
   * Note that the execution harness implements {@link ExecControl} itself, simply delegating calls this the return of this method.
   *
   * @return an execution control
   */
  public ExecControl getControl();

  /**
   * Shuts down the thread pool backing this harness.
   */
  @Override
  void close();

  /**
   * {@inheritDoc}
   */
  default ExecStarter exec() {
    return getControl().exec();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default Execution getExecution() {
    return getControl().getExecution();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default ExecController getController() {
    return getControl().getController();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default void addInterceptor(ExecInterceptor execInterceptor, Action<? super Execution> continuation) throws Exception {
    getControl().addInterceptor(execInterceptor, continuation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <T> Promise<T> blocking(Callable<T> blockingOperation) {
    return getControl().blocking(blockingOperation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return getControl().promise(action);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <T> Publisher<T> stream(Publisher<T> publisher) {
    return getControl().stream(publisher);
  }

}
