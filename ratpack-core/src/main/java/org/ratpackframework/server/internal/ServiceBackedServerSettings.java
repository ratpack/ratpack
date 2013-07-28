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

import org.ratpackframework.server.RatpackServerSettings;

public class ServiceBackedServerSettings implements RatpackServerSettings {

  private final RatpackService ratpackService;
  private final boolean reloadable;

  public ServiceBackedServerSettings(RatpackService ratpackService, boolean reloadable) {
    this.ratpackService = ratpackService;
    this.reloadable = reloadable;
  }

  public int getBindPort() {
    return ratpackService.getBindPort();
  }

  public String getBindHost() {
    return ratpackService.getBindHost();
  }

  public boolean isReloadable() {
    return reloadable;
  }
}
