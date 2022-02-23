/*
 * Copyright 2022 the original author or authors.
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

import io.netty.channel.EventLoopGroup;
import ratpack.func.Action;
import ratpack.func.BiFunction;
import ratpack.func.Function;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * An additive specification of an execution controller.
 *
 * @see ExecController#of(Action)
 * @since 2.0
 */
public interface ExecControllerSpec {
  /**
   * Registers an interceptor for execution segments managed by this controller.
   *
   * @param interceptor the interceptor to execute on new execution segments
   * @return {@code this}
   */
  ExecControllerSpec interceptor(ExecInterceptor interceptor);

  /**
   * Registers an initializer for executions created by this controller.
   *
   * @param initializer the initializer to execute on new executions
   * @return {@code this}
   */
  ExecControllerSpec initializer(ExecInitializer initializer);

  /**
   * Configures the default executor for computation (i.e. event loop) work for this controller.
   *
   * @param definition the specification for the computation executor
   * @return {@code this}
   * @see ExecController#getEventLoopGroup()
   * @see ExecController#getExecutor()
   */
  ExecControllerSpec compute(Action<? super ComputeSpec> definition);

  /**
   * Configures the default executor for blocking (i.e. I/O) work for this controller.
   *
   * @param definition the specification for the blocking executor
   * @return {@code this}
   * @see ExecController#getBlockingExecutor()
   */
  ExecControllerSpec blocking(Action<? super BlockingSpec> definition);

  /**
   * A specification for building Netty event loop groups.
   */
  interface EventLoopSpec {

    /**
     * Specify the number of threads for this group.
     *
     * @param threads the number of threads
     * @return {@code this}
     */
    EventLoopSpec threads(int threads);

    /**
     * Specify the thread prefix to utilize when constructing new threads from this group.
     *
     * @param prefix the thread name prefix
     * @return {@code this}
     */
    EventLoopSpec prefix(String prefix);

    /**
     * Specify the thread priority for threads created by this group.
     *
     * @param priority the thread priority
     * @return {@code this}
     * @see Thread#MAX_PRIORITY
     * @see Thread#MIN_PRIORITY
     * @see Thread#NORM_PRIORITY
     */
    EventLoopSpec priority(int priority);

    /**
     * Specify a factory that generates Netty {@link EventLoopGroup} instances with the arguments.
     * <p>
     * The default value for this factory is {@link ratpack.exec.util.internal.TransportDetector#eventLoopGroup(int, ThreadFactory)}
     * which will automatically configure the most optimization implementation available on the current system in the following order:
     * <li>
     *   <ul>{@code Epoll}</ul>
     *   <ul>{@code KQueue}</ul>
     *   <ul>{@code NIO}</ul>
     * </li>
     *
     * @param eventLoopGroupFactory a factory that creates event loop group instances
     * @return {@code this}
     */
    EventLoopSpec eventLoopGroup(BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory);
  }

  /**
   * A specification for building executor services
   */
  interface ExecutorServiceSpec {

    /**
     * Specify the thread name prefix to use for threads created by this executor service.
     *
     * @param prefix the thread name prefix
     * @return {@code this}
     */
    ExecutorServiceSpec prefix(String prefix);

    /**
     * Specify the thread priority for threads created by this executor service.
     *
     * @param priority the thread priority.
     * @return {@code this}
     * @see Thread#MAX_PRIORITY
     * @see Thread#MIN_PRIORITY
     * @see Thread#NORM_PRIORITY
     */
    ExecutorServiceSpec priority(int priority);

    /**
     * Specify a factory for creating an executor service with the provider arguments.
     * <p>
     * The default value for this factory is {@link Executors ::newCachedThreadPool}
     *
     * @param executorFactory the executor service factory
     * @return {@code this}
     */
    ExecutorServiceSpec executor(Function<ThreadFactory, ExecutorService> executorFactory);
  }

  /**
   * A specification for defining the default computation executor.
   * <p>
   * All parameters for this specification have defaults that can be overwritten.
   *
   * @see EventLoopSpec
   */
  interface ComputeSpec extends EventLoopSpec {

    /**
     * {@inheritDoc}
     * <p>
     * The default value for this parameter is 2 * {@link Runtime#availableProcessors()}
     *
     * @param threads the number of threads
     * @return {@code this}
     */
    @Override
    ComputeSpec threads(int threads);

    /**
     * {@inheritDoc}
     * <p>
     * The default value for this parameter is {@code "ratpack-compute"}
     *
     * @param prefix the thread name prefix
     * @return {@code this}
     */
    @Override
    ComputeSpec prefix(String prefix);

    /**
     * {@inheritDoc}
     * <p>
     * The default value for this parameter is {@link Thread#MAX_PRIORITY}
     *
     * @param priority the thread priority
     * @return {@code this}
     */
    @Override
    ComputeSpec priority(int priority);

    /**
     * {@inheritDoc}
     *
     * @param eventLoopGroupFactory a factory that creates event loop group instances
     * @return {@code this}
     */
    @Override
    ComputeSpec eventLoopGroup(BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory);

  }

  /**
   * A specification for defining the default blocking executor.
   * <p>
   * All parameters for this specification have defaults that can be overwritten.
   *
   * @see ExecutorServiceSpec
   */
  interface BlockingSpec extends ExecutorServiceSpec {

    /**
     * {@inheritDoc}
     * <p>
     * The default value for this parameter is {@code "ratpack-blocking"}
     *
     * @param prefix the thread name prefix
     * @return {@code this}
     */
    @Override
    BlockingSpec prefix(String prefix);

    /**
     * {@inheritDoc}
     * <p>
     * The default value for this parameter is {@link Thread#MIN_PRIORITY}
     *
     * @param priority the thread priority.
     * @return {@code this}
     */
    @Override
    BlockingSpec priority(int priority);

    /**
     * {@inheritDoc}
     *
     * @param executorFactory the executor service factory
     * @return {@code this}
     */
    @Override
    BlockingSpec executor(Function<ThreadFactory, ExecutorService> executorFactory);
  }
}
