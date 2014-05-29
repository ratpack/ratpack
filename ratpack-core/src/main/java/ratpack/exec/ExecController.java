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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import io.netty.channel.EventLoopGroup;
import ratpack.func.Action;

/**
 * The exec controller manages the execution of operations.
 */
public interface ExecController {

  void start(Action<? super Execution> action);

  /**
   * Provides the current context on the current thread.
   * <p>
   * This method is primarily provided for integration with dependency injection frameworks.
   *
   * @return the current context on the current thread
   * @throws ExecutionException if this method is called from a thread that is not performing request processing
   */
  Execution getExecution() throws ExecutionException;

  /**
   * A singleton that can be used from any managed thread to perform asynchronous or blocking operations.
   * <p>
   * The control can be used by support services that need to perform such operations, of which they can return the promise.
   *
   * @return the execution control.
   */
  ExecControl getControl();

  /**
   * The executor that performs computation.
   *
   * @return the executor that performs computation.
   */
  ListeningScheduledExecutorService getExecutor();

  /**
   * The event loop group used by Netty for this application.
   * <p>
   * Generally there is no need to access this unless you are doing something directly with Netty.
   *
   * @return the event loop group
   */
  EventLoopGroup getEventLoopGroup();

  /**
   * Indicates whether the current thread is managed by this execution controller.
   *
   * @return true if the current thread is managed by this execution controller
   */
  boolean isManagedThread();

  void shutdown() throws Exception;

}
