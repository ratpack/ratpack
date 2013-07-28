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

package org.ratpackframework.server;

public interface RatpackServerSettings {

  /**
   * The actual port that the application is bound to.
   *
   * @return The actual port that the application is bound to, or -1 if the server is not running.
   */
  int getBindPort();

  /**
   * The actual host/ip that the application is bound to.
   *
   * @return The actual host/ip that the application is bound to, or null if this server is not running.
   */
  String getBindHost();

  /**
   * Whether or not the server is in "reloadable" (i.e. development) mode.
   * <p>
   * Different parts of the application may respond to this as they see fit.
   *
   * @return {@code true} if the server is in "reloadable" mode
   */
  boolean isReloadable();

}
