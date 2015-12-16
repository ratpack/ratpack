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

import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

import java.util.function.Consumer;

/**
 * An override object for imposing server configuration.
 * <p>
 * This type works with the {@link Overrides} mechanism.
 * It allows arbitrary {@link ServerConfig} to be overridden.
 * <p>
 * Every {@link ServerConfigBuilder} is override aware.
 * The overrides are captured when the builder is created.
 * <p>
 * If a {@code ServerConfigOverrides} is present, its function will be applied to the builder during {@link ServerConfigBuilder#build()}.
 * Note that the {@link ForceDevelopmentOverride} and {@link ForcePortOverride} are applied <b>after</b> this override.
 *
 * @see Overrides
 * @since 1.2
 */
public final class ServerConfigOverrides {

  private final Consumer<? super ServerConfigBuilder> action;

  private ServerConfigOverrides(Consumer<? super ServerConfigBuilder> action) {
    this.action = action;
  }

  /**
   * Creates an override that applies the given function to the server config builder.
   *
   * @param overrides the overrides
   * @return an overrides object, for the overrides registry
   */
  public static ServerConfigOverrides of(Consumer<? super ServerConfigBuilder> overrides) {
    return new ServerConfigOverrides(overrides);
  }

  /**
   * Applies the overrides to the given builder.
   *
   * @param builder a server config builder
   */
  public void apply(ServerConfigBuilder builder) {
    action.accept(builder);
  }
}
