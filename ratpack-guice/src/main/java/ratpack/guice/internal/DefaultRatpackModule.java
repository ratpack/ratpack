/*
 * Copyright 2013 the original author or authors.
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

package ratpack.guice.internal;

import com.google.inject.AbstractModule;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provides;
import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.ExecutionException;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClients;
import ratpack.launch.LaunchConfig;

public class DefaultRatpackModule extends AbstractModule {

  private final LaunchConfig launchConfig;

  public DefaultRatpackModule(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig;
  }

  @Override
  protected void configure() {
    bind(LaunchConfig.class).toInstance(launchConfig);
  }

  @Provides
  ByteBufAllocator bufferAllocator(LaunchConfig launchConfig) {
    return launchConfig.getBufferAllocator();
  }

  @Provides
  ExecController execController(LaunchConfig launchConfig) {
    return launchConfig.getExecController();
  }

  @Provides
  ExecControl execControlr(ExecController execController) {
    return execController.getControl();
  }

  @Provides
  HttpClient httpClient(LaunchConfig launchConfig) {
    return HttpClients.httpClient(launchConfig);
  }

  @Provides
  Execution execution(ExecController execController) {
    try {
      return execController.getExecution();
    } catch (ExecutionException e) {
      throw new OutOfScopeException("Cannot provide an instance of " + Execution.class.getName() + " as none is bound to the current thread (are you outside of a managed thread?)");
    }
  }

}
