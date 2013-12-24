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

package ratpack.server.internal;

import ratpack.launch.LaunchConfig;
import ratpack.server.RatpackServer;

public class ServiceBackedServer implements RatpackServer {

  private final RatpackService ratpackService;
  private final LaunchConfig launchConfig;

  public ServiceBackedServer(RatpackService ratpackService, LaunchConfig launchConfig) {
    this.ratpackService = ratpackService;
    this.launchConfig = launchConfig;
  }

  public LaunchConfig getLaunchConfig() {
    return launchConfig;
  }

  public boolean isRunning() {
    return ratpackService.isRunning();
  }

  public void start() throws Exception {
    ratpackService.start();
  }

  public void stop() throws Exception {
    ratpackService.stop();
  }

  @Override
  public String getScheme() {
    return ratpackService.getScheme();
  }

  public int getBindPort() {
    return ratpackService.getBindPort();
  }

  public String getBindHost() {
    return ratpackService.getBindHost();
  }

}
