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
import ratpack.http.Request;
import ratpack.registry.MutableRegistry;

import java.util.function.Supplier;

/**
 * A <em>logical</em> stream of execution, which is potentially serialized over many threads.
 * <p>
 * Ratpack is non blocking.
 * This requires that IO and other blocking operations are performed asynchronously.
 * In completely synchronous execution, the thread and the call stack serve as the representation of a stream of execution,
 * with the execution being bound to a single thread exclusively for its entire duration.
 * The {@code Execution} concept in Ratpack brings some of the characteristics of the traditional single-thread exclusive model
 * to the asynchronous, non-exclusive, environment.
 * <p>
 * A well understood example of a logical <em>stream of execution</em> in the web application environment is the handling of a request.
 * This can be thought of as a single logical operation; the request comes in and processing happens until the response is sent back.
 * Many web application frameworks exclusively assign a thread to such a stream of execution, from a large thread pool.
 * If a blocking operation is performed during the execution, the thread sits <em>waiting</em> until it can continue (e.g. the IO completes, or the contended resource becomes available).
 * Thereby the <em>segments</em> of the execution are <em>serialized</em> and the call stack provides execution context.
 * Ratpack supports the <em>non-blocking</em> model, where threads do not wait.
 * Instead of threads waiting for IO or some future event, they are returned to the “pool” to be used by other executions (and therefore the pool can be smaller).
 * When the IO completes or the contended resource becomes available, execution continues with a new call stack and possibly on a different thread.
 * <p>
 * <b>The execution object underpins an entire logical operation, even when that operation may be performed by multiple threads.</b>
 * <p>
 * Importantly, it also <em>serializes</em> execution segments by way of the {@link ratpack.exec.ExecControl#promise(ratpack.func.Action)} method.
 * These methods are fundamentally asynchronous in that they facilitate performing operations where the result will arrive later without waiting for the result,
 * but are synchronous in the operations the perform are serialized and not executed concurrently or in parallel.
 * <em>Crucially</em>, this means that state that is local to an execution does not need to be thread safe.
 * <h3>Executions and request handling</h3>
 * <p>
 * The execution object actually underpins the {@link ratpack.handling.Context} objects that are used when handling requests.
 * It is rarely used directly when request handling, except when concurrency or parallelism is required to process data via the {@link ratpack.handling.Context#fork()} method.
 * Moreover, it provides its own error handling and completion mechanisms.
 * </p>
 */
public interface Execution extends MutableRegistry, ExecControl {

  /**
   * Provides the currently execution execution.
   * <p>
   * This method will fail when called outside of a Ratpack compute thread as it relies on {@link ExecController#require()}.
   *
   * @return the currently execution execution
   */
  static Execution execution() throws UnmanagedThreadException {
    return ExecControl.current().getExecution();
  }

  /**
   * The execution controller that this execution is associated with.
   *
   * @return the execution controller that this execution is associated with
   */
  ExecController getController();

  ExecControl getControl();

  EventLoop getEventLoop();

  // TODO: this is not the right name.
  void onCleanup(AutoCloseable autoCloseable);

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Execution add(Class<? super O> type, O object) {
    MutableRegistry.super.add(type, object);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default <O> Execution add(TypeToken<? super O> type, O object) {
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

  void setRequest(Request request);
}
