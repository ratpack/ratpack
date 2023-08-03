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

package ratpack.server;

import io.netty.channel.ChannelOption;

/**
 * Extension mechanism used to set Netty channel options not configurable via {@link ServerConfig}.
 * <p>
 * To use, create your own types that implement this interface that will be deserialized from server config data and
 * specify as {@link ServerConfigBuilder#require(String, Class) required config}.
 * <p>
 * Any {@link ServerConfig#getRequiredConfig()} that implements this interface will automatically have its
 * {@link #setOptions(OptionSetter)} and {@link #setChildOptions(OptionSetter)} methods invoked when building
 * the server.
 * <p>
 * These methods are invoked <b>after</b> setting options based on {@link ServerConfig} values, so they may override
 * any such values already set.
 *
 * @since 1.10
 */
public interface ServerChannelOptions {

  interface OptionSetter {
    <T> OptionSetter set(ChannelOption<T> option, T value);
  }

  /**
   * Sets the channel options for the server channel.
   *
   * @param setter the option setter
   */
  default void setOptions(OptionSetter setter) {
  }

  /**
   * Sets the channel options for child channels.
   *
   * @param setter the option setter
   */
  default void setChildOptions(OptionSetter setter) {
  }

}
