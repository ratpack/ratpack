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
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

public class RatpackBaseRegistryModule extends AbstractModule {

  private final Registry baseRegistry;

  public RatpackBaseRegistryModule(Registry baseRegistry) {
    this.baseRegistry = baseRegistry;
  }

  @Override
  protected void configure() {
  }

  @Provides
  ByteBufAllocator bufferAllocator() {
    return baseRegistry.get(ByteBufAllocator.class);
  }

  @Provides
  ExecController execController() {
    return baseRegistry.get(ExecController.class);
  }

  @Provides
  ExecControl execControl(ExecController execController) {
    return execController.getControl();
  }

  @Provides
  ServerConfig serverConfig() {
    return baseRegistry.get(ServerConfig.class);
  }

  @Provides
  HttpClient httpClient(ServerConfig serverConfig) {
    return HttpClients.httpClient(serverConfig, baseRegistry);
  }

  @Provides
  Execution execution(ExecControl execControl) {
    try {
      return execControl.getExecution();
    } catch (ExecutionException e) {
      throw new OutOfScopeException("Cannot provide an instance of " + Execution.class.getName() + " as none is bound to the current thread (are you outside of a managed thread?)");
    }
  }

}
