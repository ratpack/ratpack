/*
 * Copyright 2023 the original author or authors.
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

import java.time.Duration;

/**
 * A builder of an exec controller.
 *
 * @see ExecControllerBuilder
 * @since 1.10
 */
public interface ExecControllerBuilder extends ExecControllerSpec {

  /**
   * {@inheritDoc}
   */
  @Override
  ExecControllerBuilder numThreads(int n);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecControllerBuilder contextClassLoader(ClassLoader classLoader);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecControllerBuilder blockingThreadIdleTimeout(Duration idleTimeout);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecControllerBuilder execInitializers(Iterable<? extends ExecInitializer> initializers);

  /**
   * {@inheritDoc}
   */
  @Override
  ExecControllerBuilder execInterceptors(Iterable<? extends ExecInterceptor> interceptors);

  /**
   * Creates a controller from the current state.
   *
   * @return a controller from the current state
   */
  ExecController build();

}
