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
import ratpack.func.Supplier;

import java.util.List;

/**
 * The foreground represents the request processing, and computation handling, aspect of an application.
 */
public interface Foreground {

  /**
   * Provides the current context on the current thread.
   * <p>
   * This method is primarily provided for integration with dependency injection frameworks.
   *
   * @return the current context on the current thread
   * @throws NoBoundContextException if this method is called from a thread that is not performing request processing
   */
  ExecContext getContext() throws NoBoundContextException;

  /**
   * The executor that manages foreground work.
   * <p>
   * Can be used for scheduling or performing computation.
   * This is the same executor that is managing request handling threads.
   * Blocking operations should not be performed by tasks of this executor.
   *
   * @return the executor that manages foreground work.
   */
  ListeningScheduledExecutorService getExecutor();

  EventLoopGroup getEventLoopGroup();

  void exec(Supplier<? extends ExecContext> execContextFactory, List<ExecInterceptor> interceptors, ExecInterceptor.ExecType execType, Action<? super ExecContext> action);

  void onExecFinish(Runnable runnable);

}
