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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import io.netty.channel.EventLoopGroup;

/**
 * The exec controller manages the execution of operations.
 * <p>
 * The instance for an application can be obtained via the launch config's {@link ratpack.launch.LaunchConfig#getExecController()} method.
 */
public interface ExecController extends AutoCloseable {

  /**
   * Indicates whether the current thread is managed by <b>this</b> execution controller.
   * <p>
   * This will return {@code true} if the current thread is either part of the event loop thread pool or blocking thread pool of the
   * application that is backed by this execution controller.
   *
   * @return true if the current thread is managed by this execution controller
   */
  boolean isManagedThread();

  /**
   * A <strong>singleton</strong> that can be used from any managed thread to perform asynchronous or blocking operations.
   * <p>
   * The control is typically used by services that are not inherently tied to any specific execution to perform execution operations
   * such as blocking and forking.
   * <p>
   * If you are using the Guice integration, an instance of this type can be injected.
   *
   * @return the execution control
   */
  ExecControl getControl();

  /**
   * The event loop (i.e. computation) executor.
   * <p>
   * This executor wraps Netty's event loop executor to provide callback features by way of Guava's executor extensions.
   * <p>
   * It is generally preferable to use {@link ExecControl#fork(ratpack.func.Action)} to submit computation work rather than this method,
   * which properly initialises Ratpack's execution infrastructure.
   *
   * @return the executor that performs computation
   */
  ListeningScheduledExecutorService getExecutor();

  ListeningExecutorService getBlockingExecutor();

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
   * This is determined by the {@link ratpack.launch.LaunchConfig#getThreads()} value of the launch config that created this controller.
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
