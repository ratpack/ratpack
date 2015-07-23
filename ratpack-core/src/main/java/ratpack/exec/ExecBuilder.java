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

import io.netty.channel.EventLoop;
import ratpack.api.NonBlocking;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.registry.RegistrySpec;

/**
 * Builds, and initiates, a new {@link Execution execution}.
 *
 * @see Execution#fork()
 */
public interface ExecBuilder {

  /**
   * Specify the top level error handler for the execution.
   *
   * @see #onError(BiAction)
   * @param onError the top level error handler for the execution
   * @return {@code this}
   */
  ExecBuilder onError(Action<? super Throwable> onError);

  /**
   * Specify the top level error handler for the execution.
   * <p>
   * The given action will be called if an exception is raised during execution that is not caught.
   * <p>
   * This method is not additive.
   * That is, any subsequent calls replace the previous value.
   *
   * @see #onError(BiAction)
   * @param onError the top level error handler for the execution
   * @return {@code this}
   */
  ExecBuilder onError(BiAction<? super Execution, ? super Throwable> onError);

  /**
   * Specifies the completion callback for the execution.
   * <p>
   * The given action will effectively execute <b>outside</b> of the execution.
   * The action is expected to be synchronous and cannot perform async operations.
   * During its execution, there will be no thread bound execution or execution control.
   * Any exceptions raised will be logged.
   * <p>
   * This method should be used as a last resort.
   * <p>
   * The action will be invoked regardless of whether the execution completed with an error or not.
   * If the execution did complete with an error, the given action will be invoked <b>after</b> the error handler.
   * <p>
   * This method is not additive.
   * That is, any subsequent calls replace the previous value.

   * @param onComplete the action to invoke when the execution completes.
   * @return {@code this}
   */
  ExecBuilder onComplete(Action<? super Execution> onComplete);

  /**
   * Specifies an action to be taken just before the execution starts.
   * <p>
   * The action will be invoked after the execution registry has been populated.
   * <p>
   * This method is not additive.
   * That is, any subsequent calls replace the previous value.

   * @param onStart the action to invoke just before the execution starts
   * @return {@code this}
   */
  ExecBuilder onStart(Action<? super Execution> onStart);

  /**
   * Populates the execution's registry.
   * <p>
   * This method is not additive.
   * That is, any subsequent calls replace the previous value.
   *
   * @param action the initial contents of the execution's registry.
   * @return {@code this}
   */
  ExecBuilder register(Action<? super RegistrySpec> action);

  /**
   * Specifies that the execution must run on the given event loop.
   * <p>
   * If this method is not called, an event loop will be automatically assigned from the {@link ExecController#getEventLoopGroup() exec controller's event loop group}.
   * It is generally not required, or desirable, to call this method.
   *
   * @param eventLoop the event loop to use for the execution
   * @return {@code this}
   */
  ExecBuilder eventLoop(EventLoop eventLoop);

  /**
   * Initiate the new execution.
   * <p>
   * This method effectively returns immediately, with the forked execution occurring on a separate thread.
   *
   * @param action the initial execution segment of the execution
   */
  @NonBlocking
  void start(Action<? super Execution> action);

}
