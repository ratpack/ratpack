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

package ratpack.exec.internal;

import ratpack.exec.ExecController;
import ratpack.exec.ExecControllerBuilder;
import ratpack.exec.ExecInitializer;
import ratpack.exec.ExecInterceptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DefaultExecControllerBuilder implements ExecControllerBuilder {

  private int numThreads = Runtime.getRuntime().availableProcessors() * 2;
  private Duration blockingThreadIdleTimeout = Duration.ofSeconds(60);
  private ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
  private final List<ExecInitializer> execInitializers = new ArrayList<>();
  private final List<ExecInterceptor> execInterceptors = new ArrayList<>();

  @Override
  public ExecControllerBuilder numThreads(int n) {
    this.numThreads = n;
    return this;
  }

  @Override
  public ExecControllerBuilder contextClassLoader(ClassLoader classLoader) {
    this.contextClassLoader = classLoader;
    return this;
  }

  @Override
  public ExecControllerBuilder blockingThreadIdleTimeout(Duration idleTimeout) {
    this.blockingThreadIdleTimeout = idleTimeout;
    return this;

  }

  @Override
  public ExecControllerBuilder execInitializers(Iterable<? extends ExecInitializer> initializers) {
    initializers.forEach(this.execInitializers::add);
    return this;
  }

  @Override
  public ExecControllerBuilder execInterceptors(Iterable<? extends ExecInterceptor> interceptors) {
    interceptors.forEach(this.execInterceptors::add);
    return this;
  }

  @Override
  public ExecController build() {
    return new DefaultExecController(
      numThreads,
      blockingThreadIdleTimeout,
      contextClassLoader,
      execInitializers,
      execInterceptors
    );
  }

}
