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

import io.netty.channel.EventLoopGroup;
import ratpack.exec.internal.DefaultExecControllerBuilder;
import ratpack.exec.internal.ExecThreadBinding;
import ratpack.func.Block;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The exec controller manages the execution of operations.
 * <p>
 * The instance for an application can be obtained via the server registry.
 */
public interface ExecController extends AutoCloseable {

  /**
   * Returns the execution controller bound to the current thread, if this is a Ratpack managed compute thread.
   * <p>
   * If called on a non Ratpack compute thread, the returned optional will be empty.
   *
   * @return the execution controller for the current thread
   */
  static Optional<ExecController> current() {
    return ExecThreadBinding.maybeGet().map(ExecThreadBinding::getExecController);
  }

  /**
   * Returns the execution controller bound to the current thread, or throws an exception if called on a non Ratpack managed compute thread.
   * <p>
   * If called on a non Ratpack compute thread, the returned optional will be empty.
   *
   * @return the execution controller for the current thread
   * @throws UnmanagedThreadException when called from a non Ratpack managed thread
   */
  static ExecController require() throws UnmanagedThreadException {
    return current().orElseThrow(UnmanagedThreadException::new);
  }

  /**
   * Creates a new builder.
   *
   * @return a new builder
   */
  static ExecControllerBuilder builder() {
    return new DefaultExecControllerBuilder();
  }

  /**
   * Create a new {@link Execution} from this controller that is bound to the computation threads.
   * @return a builder for the execution
   */
  ExecStarter fork();

  /**
   * The event loop (i.e. computation) executor.
   * <p>
   * This executor wraps Netty's event loop executor to provide callback features by way of Guava's executor extensions.
   * <p>
   * It is generally preferable to use {@link #fork()} to submit computation work rather than this method,
   * which properly initialises Ratpack's execution infrastructure.
   *
   * @return the executor that performs computation
   */
  ScheduledExecutorService getExecutor();

  /**
   * The blocking (i.e. I/O) executor service.
   * <p>
   * This executor service provides a thread pool which can be used to schedule work such that it does not block the
   * computation threads.
   * <p>
   * The result of work executed on this thread should typically be returned to a continuation on a compute thread
   *
   * @see Blocking#get
   * @return the default blocking executor service
   */
  ExecutorService getBlockingExecutor();

  /**
   * The event loop group used by Netty for this application.
   * <p>
   * Generally there is no need to access this unless you are doing something directly with Netty.
   *
   * @return the event loop group
   */
  EventLoopGroup getEventLoopGroup();

  /**
   * The number of threads that will be used for computation.
   * <p>
   * This is determined by the {@link ServerConfig#getThreads()} value of the launch config that created this controller.
   *
   * @return the number of threads that will be used for computation
   */
  int getNumThreads();

  /**
   * Adds a callback to run when stopping this exec controller.
   *
   * @param block the callback to execute when stopping this controller
   * @return false if the controller is stopped or stopping and the block will not be executed, otherwise true
   * @since 1.10
   */
  boolean onClose(Block block);

  /**
   * The registered interceptors for this controller.
   * <p>
   * The returned list is immutable and will not contain any future additions.
   *
   * @return the registered interceptors for this controller
   * @since 1.10
   */
  List<? extends ExecInterceptor> getInterceptors();

  /**
   * The registered initializers for this controller.
   * <p>
   * The returned list is immutable and will not contain any future additions.
   *
   * @return the registered initializers for this controller
   * @since 1.10
   */
  List<? extends ExecInitializer> getInitializers();

  /**
   * Adds the given interceptors to this controller.
   *
   * @param interceptors the given interceptors to this controller
   * @since 1.10
   */
  void addInterceptors(Iterable<? extends ExecInterceptor> interceptors);

  /**
   * Adds the given initializers to this controller.
   *
   * @param initializers the given interceptors to this controller
   * @since 1.10
   */
  void addInitializers(Iterable<? extends ExecInitializer> initializers);

  /**
   * Shuts down this controller, terminating the event loop and blocking threads.
   * <p>
   * This method returns immediately, not waiting for the actual shutdown to occur.
   * <p>
   * Generally, the only time it is necessary to call this method is when using an exec controller directly during testing.
   */
  @Override
  void close();
}
