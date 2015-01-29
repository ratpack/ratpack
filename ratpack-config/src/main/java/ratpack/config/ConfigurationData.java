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

package ratpack.config;

import ratpack.server.ReloadInformant;

/**
 * Provides access to configuration data bound to arbitrary objects.
 */
public interface ConfigurationData {
  /**
   * Binds a segment of the configuration data to the specified type.
   *
   * @param pointer a <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a> specifying
   * the point in the configuration data to bind from
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return an instance of the specified type with bound configuration data
   */
  <O> O get(String pointer, Class<O> type);

  /**
   * Binds the root of the configuration data to the specified type.
   *
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return an instance of the specified type with bound configuration data
   */
  default <O> O get(Class<O> type) {
    return get(null, type);
  }

  /**
   * Returns a reload informant that can be used to reload the server when the configuration data changes.
   *
   * @return a reload informant backed by this configuration data
   */
  ReloadInformant getReloadInformant();
}
