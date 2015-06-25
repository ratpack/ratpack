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

import org.reactivestreams.Publisher;
import ratpack.exec.internal.DefaultOperation;
import ratpack.exec.internal.JustInTimeExecControl;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.func.Factory;
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.Callable;

/**
 * Provides methods for controlling execution(i.e. blocking, forking and calling async API), independent of the current execution.
 *
 * <pre class="java">{@code
 * import ratpack.exec.ExecControl;
 * import ratpack.exec.ExecController;
 * import ratpack.exec.Promise;
 *
 * import ratpack.test.handling.RequestFixture;
 * import ratpack.test.handling.HandlingResult;
 *
 * import static org.junit.Assert.assertEquals;
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
 *     public Promise<String> toUpper(final String lower) {
 *       return control.promise(f -> f.success(lower.toUpperCase()));
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     HandlingResult result = RequestFixture.requestFixture().handleChain(chain -> {
 *       ExecControl control = chain.getRegistry().get(ExecController.class).getControl();
 *       AsyncUpperCaseService service = new AsyncUpperCaseService(control);
 *       chain.get(ctx -> service.toUpper("foo").then(ctx::render));
 *     });
 *
 *     assertEquals("FOO", result.rendered(String.class));
 *   }
 * }
 * }</pre>
 * <p>
 * <b>Note:</b> when using the Guice integration, the exec control is made available for injection.
 */
public interface ExecControl {

  /**
   * Provides the execution control bound to the current thread.
   * <p>
   * This method will fail when called outside of a Ratpack compute thread as it relies on {@link ExecController#require()}.
   *
   * @return the execution control bound to the current thread
   */
  static ExecControl current() throws UnmanagedThreadException {
    return ExecController.require().getControl();
  }

  /**
   * An exec control that binds to the thread's execution on demand.
   * <p>
   * Unlike the {@link #current()} method, this method can be called outside of a Ratpack managed thread.
   * However, the methods of the returned exec control can only be called on managed threads.
   * If a method is called while not on a managed thread, a {@link UnmanagedThreadException} will be thrown.
   *
   * <pre class="java">{@code
   * import ratpack.exec.ExecControl;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   // Get an exec control on a non managed thread (i.e. the JVM main thread)
   *   public static ExecControl control = ExecControl.execControl();
   *
   *   public static void main(String... args) throws Exception {
   *     String value = ExecHarness.yieldSingle(e ->
   *       control.blocking(() -> "foo")
   *     ).getValue();
   *     assertEquals("foo", value);
   *   }
   * }
   * }</pre>
   *
   * @return an exec control
   */
  static ExecControl execControl() {
    return JustInTimeExecControl.INSTANCE;
  }

  Execution getExecution() throws UnmanagedThreadException;

  ExecController getController();

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
  void addInterceptor(ExecInterceptor execInterceptor, Block continuation) throws Exception;

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
   *
   * @param blockingOperation the operation that blocks
   * @param <T> the type of value created by the operation
   * @return a promise for the return value of the given blocking operation
   */
  <T> Promise<T> blocking(Callable<T> blockingOperation);

  default Operation blockingOperation(Block block) {
    return blocking(() -> {
      block.execute();
      return null;
    }).operation();
  }

  /**
   * Creates a promise for an asynchronously created value.
   * <p>
   * This method can be used to integrate with APIs that produce values asynchronously.
   * The asynchronous API should be invoked during the execute method of the action given to this method.
   * The result of the asynchronous call is then given to the {@link Fulfiller} that the action is given.
   *
   * @param action an action that invokes an asynchronous API, forwarding the result to the given fulfiller
   * @param <T> the type of promised value
   * @return a promise for the asynchronously created value
   * @see Fulfiller
   */
  <T> Promise<T> promise(Action<? super Fulfiller<T>> action);

  /**
   * Creates a promise for the given value.
   * <p>
   * This method can be used when a promise is called for, but the value is immediately available.
   *
   * @param item the promised value
   * @param <T> the type of promised value
   * @return a promise for the given item
   */
  default <T> Promise<T> promiseOf(T item) {
    return promise(f -> f.success(item));
  }

  default <T> Promise<T> promiseFrom(Factory<? extends T> factory) {
    return promise(f -> f.success(factory.create()));
  }

  /**
   * Creates a failed promise with the given error.
   * <p>
   * This method can be used when a promise is called for, but the failure is immediately available.
   *
   * @param error the promise failure
   * @param <T> the type of promised value
   * @return a failed promise
   */
  default <T> Promise<T> failedPromise(Throwable error) {
    return promise(f -> f.error(error));
  }

  /**
   * Executes the given promise producing factory, converting any thrown exception into a failed promise.
   * <p>
   * Can be used to wrap execution of promise returning functions that may themselves throw errors.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.exec.ExecControl;
   * import ratpack.exec.ExecResult;
   * import ratpack.test.exec.ExecHarness;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static Promise<String> someMethod() throws Exception {
   *     throw new Exception("bang!");
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(e ->
   *       e.wrap(() -> someMethod())
   *     );
   *
   *     assertEquals("bang!", result.getThrowable().getMessage());
   *   }
   * }
   * }</pre>
   *
   * @param factory the promise factory
   * @param <T> the type of promised value
   * @return the promise returned by the factory, or a promise for the exception it threw
   */
  default <T> Promise<T> wrap(Factory<? extends Promise<T>> factory) {
    try {
      return factory.create();
    } catch (Exception e) {
      return failedPromise(e);
    }
  }

  default Operation operation(Block operation) {
    return new DefaultOperation(this.<Void>promise(f -> {
      operation.execute();
      f.success(null);
    }));
  }

  default void nest(Block nested, Block then) {
    operation(nested).then(then);
  }

  default void nest(Block nested, Block then, Action<? super Throwable> onError) {
    operation(nested).onError(onError).then(then);
  }

  /**
   * Initiates a new {@link Execution execution}.
   * <p>
   *
   *
   * @return a new execution builder
   */
  ExecBuilder fork();

  /**
   * Process streams of data asynchronously with non-blocking back pressure.
   * <p>
   * This method allows the processing of elements (onNext) or termination signals (onError, onComplete) to happen outside of the execution stack of the Publisher.
   * In other words these "events" are executed asynchronously, on a Ratpack managed thread, without blocking the Publisher.
   *
   * @param publisher the provider of a potentially unbounded number of sequenced elements, publishing them according to the demand
   * received from its Subscriber(s)
   * @return effectively the given publisher, emitting each “event” as an execution segment of the current execution
   * @param <T> the type of streamed elements
   */
  <T> TransformablePublisher<T> stream(Publisher<T> publisher);

}
