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

import com.google.common.reflect.TypeToken;
import io.netty.channel.EventLoop;
import ratpack.exec.internal.DefaultExecution;
import ratpack.exec.internal.ThreadBinding;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.registry.MutableRegistry;

import java.util.function.Supplier;

/**
 * A logical operation, such as servicing a request, that may be comprised of non contiguous units of work.
 * <p>
 * In a synchronous environment, a logical operation is typically given exclusive access to a thread for its duration.
 * The use of thread local variables for operation global state and try/catch as a global error handling strategy rely on this.
 * The execution construct provides mechanisms to emulate constructs of synchronous programming that cannot be achieved the same way in asynchronous programming.
 * <p>
 * Almost all work that occurs as part of a running Ratpack application happens during an execution.
 * Ratpack APIs such as {@link Promise}, {@link Blocking} etc. can only be used within an execution.
 * Request processing is always within an execution.
 * When initiating other work (e.g. background processing), an execution can be created via {@link Execution#fork()}
 * <p>
 * The term “execution segment” (sometimes just “segment”) is used to refer to a unit of work within an execution.
 * An execution segment has exclusive access to a thread.
 * All executions start with the segment given to the {@link ExecStarter#start(Action)} method.
 * If the initial execution segment does not use any asynchronous APIs, the execution will be comprised of that single segment.
 * When an asynchronous API is used, via {@link Promise#async(Upstream)}, the resumption of work when the result becomes available is within a new execution segment.
 * During any execution segment, the {@link Execution#current()} method will return the current execution, giving global access to the execution object.
 * <p>
 * Segments of an execution are never executed concurrently.
 *
 * <h3>Execution state (i.e. simulating thread locals)</h3>
 * <p>
 * Each execution is a {@link MutableRegistry}.
 * Objects can be added to this registry and then later retrieved at any time during the execution.
 * The registry storage can be leveraged via an {@link ExecInterceptor} to manage thread local storage for execution segments.
 *
 * <h3>Error handling</h3>
 * <p>
 * When starting an execution, a global error handler can be specified via {@link ExecStarter#onError(Action)}.
 * The default error handler simply logs the error to a logger named {@code ratpack.exec.Execution}.
 * <p>
 * The error handler for request processing executions forwards the exception to {@link ratpack.handling.Context#error(Throwable)}.
 *
 * <h3>Cleanup</h3>
 * <p>
 * The {@link #onComplete(AutoCloseable)} method can be used to register actions to invoke or resources to close when the execution completes.
 *
 * @see ExecInterceptor
 * @see ExecInitializer
 * @see ExecController
 * @see Promise
 */
public interface Execution extends MutableRegistry {

  /**
   * Provides the currently executing execution.
   *
   * @return the currently executing execution
   * @throws UnmanagedThreadException if called from outside of an execution
   */
  static Execution current() throws UnmanagedThreadException {
    return DefaultExecution.require();
  }

  /**
   * Used to create a new execution.
   * <p>
   * This method obtains the thread bound {@link ExecController} and simply calls {@link ExecController#fork()}.
   *
   * @return an execution starter
   * @throws UnmanagedThreadException if there is no thread bound execution controller (i.e. this was called on a thread that is not managed by the Ratpack application)
   */
  static ExecStarter fork() throws UnmanagedThreadException {
    return ExecController.require().fork();
  }

  /**
   * Whether the current thread is a thread that is managed by Ratpack.
   *
   * @return whether the current thread is a thread that is managed by Ratpack
   */
  static boolean isManagedThread() {
    return ThreadBinding.get().isPresent();
  }

  /**
   * Whether the current thread is a Ratpack compute thread.
   *
   * @return whether the current thread is a Ratpack compute thread
   */
  static boolean isComputeThread() {
    return ThreadBinding.get().map(ThreadBinding::isCompute).orElse(false);
  }

  /**
   * Whether the current thread is a Ratpack blocking thread.
   *
   * @return whether the current thread is a Ratpack blocking thread
   */
  static boolean isBlockingThread() {
    return ThreadBinding.get().map(threadBinding -> !threadBinding.isCompute()).orElse(false);
  }

  /**
   * The execution controller that this execution is associated with.
   *
   * @return the execution controller that this execution is associated with
   */
  ExecController getController();

  /**
   * The specific event loop that this execution is using for compute operations.
   * <p>
   * When integrating with asynchronous API that allows an executor to be specified that should be used to
   * schedule the receipt of the value, use this executor.
   *
   * @return the event loop used by this execution
   */
  EventLoop getEventLoop();

  /**
   * Registers a closeable that will be closed when the execution completes.
   * <p>
   * Where possible, care should be taken to have the given closeable not throw exceptions.
   * Any that are thrown will be logged and ignored.
   *
   * @param closeable the resource to close when the execution completes
   */
  void onComplete(AutoCloseable closeable);

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Execution add(Class<O> type, O object) {
    MutableRegistry.super.add(type, object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Execution add(TypeToken<O> type, O object) {
    MutableRegistry.super.add(type, object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default Execution add(Object object) {
    MutableRegistry.super.add(object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Execution addLazy(Class<O> type, Supplier<? extends O> supplier) {
    MutableRegistry.super.addLazy(type, supplier);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Execution addLazy(TypeToken<O> type, Supplier<? extends O> supplier);

  /**
   * Adds an interceptor that wraps the rest of the current execution segment and all future segments of this execution.
   * <p>
   * The given action is executed immediately.
   * Any code executed after a call to this method in the same execution segment <b>WILL NOT</b> be intercepted.
   * Therefore, it is advisable to not execute any code after calling this method in a given execution segment.
   * <p>
   * See {@link ExecInterceptor} for example use of an interceptor.
   * <p>
   * It is generally preferable to register the interceptor in the server registry, or execution registry when starting, than using this method.
   * That way, the interceptor can interceptor all of the execution.
   *
   * @param execInterceptor the execution interceptor to add
   * @param continuation the rest of the code to be executed
   * @throws Exception any thrown by {@code continuation}
   * @see ExecInterceptor
   */
  void addInterceptor(ExecInterceptor execInterceptor, Block continuation) throws Exception;

}
