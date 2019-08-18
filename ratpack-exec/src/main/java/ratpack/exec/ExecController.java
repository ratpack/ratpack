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
import ratpack.exec.internal.ExecThreadBinding;

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
   * This is determined by the {@link ratpack.server.ServerConfig#getThreads()} value of the launch config that created this controller.
   *
   * @return the number of threads that will be used for computation
   */
  int getNumThreads();

  /**
   * Shuts down this controller, terminating the event loop and blocking threads.
   * <p>
   * This method returns immediately, not waiting for the actual shutdown to occur.
   * <p>
   * Generally, the only time it is necessary to call this method is when using an exec controller directly during testing.
   * Calling {@link ratpack.server.RatpackServer#stop()} will inherently call this method.
   */
  @Override
  void close();

}
