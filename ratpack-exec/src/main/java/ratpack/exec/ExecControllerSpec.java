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
 * A mutable specification of an exec controller.
 *
 * @see ExecControllerBuilder
 * @since 1.10
 */
public interface ExecControllerSpec {

  /**
   * Sets the number of compute threads to use.
   * <p>
   * Defaults to {@code Runtime.getRuntime().availableProcessors() * 2}.
   *
   * @param n the number of compute threads to use
   * @return {@code this}
   */
  ExecControllerSpec numThreads(int n);

  /**
   * Sets the number of persistent blocking threads to use.
   * <p>
   * This number of threads will be permanently reserved for blocking work.
   * The thread pool will grow on demand before shrinking back to this size as threads idle out.
   * <p>
   * Defaults to {@code Runtime.getRuntime().availableProcessors() * 2}.
   *
   * @param n the number of persistent blocking threads to use
   * @return {@code this}
   * @see #blockingThreadIdleTimeout(Duration)
   */
  ExecControllerSpec numCoreBlockingThreads(int n);

  /**
   * The duration to allow a non-core blocking thread to remain in the pool before terminating it.
   * <p>
   * Defaults to 3 seconds.
   *
   * @param idleTimeout the duration to allow a non-core blocking thread to remain in the pool before terminating it
   * @return {@code this}
   */
  ExecControllerSpec blockingThreadIdleTimeout(Duration idleTimeout);

  /**
   * The exec initializers to use for initializing executions.
   *
   * @param initializers the exec initializers to use for initializing executions
   * @return {@code this}
   */
  ExecControllerSpec execInitializers(Iterable<ExecInitializer> initializers);

  /**
   * The exec interceptors to use for intercepting executions.
   *
   * @param interceptors exec interceptors to use for intercepting executions
   * @return {@code this}
   */
  ExecControllerSpec execInterceptors(Iterable<ExecInterceptor> interceptors);

  /**
   * The context classloader to initialize threads with.
   * <p>
   * Defaults to the current context classloader of the thread that created {@code this}.
   *
   * @param classLoader the context classloader to initialize threads with
   * @return {@code this}
   */
  ExecControllerSpec contextClassLoader(ClassLoader classLoader);

}
