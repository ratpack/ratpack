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
import ratpack.registry.RegistrySpec;

/**
 * Starts a new {@link Execution}.
 *
 * @see Execution#fork()
 */
public interface ExecStarter extends ExecSpec {

  /**
   * Starts the execution, with the given action as the initial segment.
   *
   * @param initialExecutionSegment the initial execution segment of the execution
   */
  @NonBlocking
  void start(Action<? super Execution> initialExecutionSegment);

  /**
   * Starts the execution, and executes the given operation.
   *
   * @param operation the operation to execute
   * @since 1.4
   */
  @NonBlocking
  default void start(Operation operation) {
    start(e -> operation.then());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  ExecStarter onError(Action<? super Throwable> onError);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecStarter onComplete(Action<? super Execution> onComplete);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecStarter onStart(Action<? super Execution> onStart);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecStarter register(Action<? super RegistrySpec> action);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecStarter eventLoop(EventLoop eventLoop);

}
