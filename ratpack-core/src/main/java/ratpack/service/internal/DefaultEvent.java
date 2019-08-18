/*
 * Copyright 2016 the original author or authors.
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

package ratpack.service.internal;

import ratpack.registry.Registry;
import ratpack.service.StartEvent;
import ratpack.service.StopEvent;

@SuppressWarnings("deprecation")
public class DefaultEvent implements StartEvent, StopEvent, ratpack.server.StartEvent, ratpack.server.StopEvent {

  private final Registry registry;
  private final boolean reload;

  public DefaultEvent(Registry registry, boolean reload) {
    this.registry = registry;
    this.reload = reload;
  }

  @Override
  public Registry getRegistry() {
    return registry;
  }

  @Override
  public boolean isReload() {
    return reload;
  }

}
