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

package org.ratpackframework.server.internal;

import org.ratpackframework.server.RatpackServer;
import org.ratpackframework.server.RatpackServerSettings;

public class ServiceBackedServer implements RatpackServer {

  private final RatpackService ratpackService;
  private final RatpackServerSettings settings;

  public ServiceBackedServer(RatpackService ratpackService, RatpackServerSettings settings) {
    this.ratpackService = ratpackService;
    this.settings = settings;
  }

  public RatpackServerSettings getSettings() {
    return settings;
  }

  public boolean isRunning() {
    return ratpackService.isRunning();
  }

  public void start() throws Exception {
    ratpackService.start().get();
  }

  public void stop() throws Exception {
    ratpackService.stop().get();
  }

  public int getBindPort() {
    return ratpackService.getBindPort();
  }

  public String getBindHost() {
    return ratpackService.getBindHost();
  }

}
