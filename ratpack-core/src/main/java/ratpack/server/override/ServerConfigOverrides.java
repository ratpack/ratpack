/*
 * Copyright 2015 the original author or authors.
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

package ratpack.server.override;

import ratpack.server.ServerConfigBuilder;

import java.util.function.Consumer;

public final class ServerConfigOverrides {

  private final Consumer<? super ServerConfigBuilder> action;

  private ServerConfigOverrides(Consumer<? super ServerConfigBuilder> action) {
    this.action = action;
  }

  public static ServerConfigOverrides of(Consumer<? super ServerConfigBuilder> overrides) {
    return new ServerConfigOverrides(overrides);
  }

  public void apply(ServerConfigBuilder builder) {
    action.accept(builder);
  }
}
