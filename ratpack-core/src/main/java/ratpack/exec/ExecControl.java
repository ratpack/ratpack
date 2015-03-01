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
import ratpack.func.Action;
import ratpack.func.NoArgAction;
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

  Execution getExecution();

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
  void addInterceptor(ExecInterceptor execInterceptor, NoArgAction continuation) throws Exception;

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
   * @see Fulfillment
   */
  <T> Promise<T> promise(Action<? super Fulfiller<T>> action);

  default <T> Promise<T> promiseOf(T item) {
    return promise(f -> f.success(item));
  }

  /**
   * Creates a new execution starter that can be used to initiate a new execution.
   *
   * @return an execution starter that can be used to configure and start a new execution.
   */
  ExecStarter exec();

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
