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

package ratpack.http.client;

import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.ExecController;
import ratpack.http.client.internal.DefaultHttpClient;
import ratpack.server.ServerConfig;
import ratpack.registry.Registry;

public abstract class HttpClients {

  private HttpClients() {
  }

  public static HttpClient httpClient(ServerConfig serverConfig, Registry registry) {
    return new DefaultHttpClient(registry.get(ExecController.class), registry.get(ByteBufAllocator.class), serverConfig.getMaxContentLength());
  }

  public static HttpClient httpClient(ExecController execController, ByteBufAllocator byteBufAllocator, int maxContentLengthBytes) {
    return new DefaultHttpClient(execController, byteBufAllocator, maxContentLengthBytes);
  }

}
