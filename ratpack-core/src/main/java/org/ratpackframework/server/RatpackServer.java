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

/**
 * A Ratpack server.
 */
public interface RatpackServer {

  /**
   * The (read only) settings of this server.
   *
   * @return the (read only) settings of this server
   */
  RatpackServerSettings getSettings();

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
   * Returns {@code true} if the server is running.
   *
   * @return {@code true} if the server is running.
   */
  boolean isRunning();

  /**
   * Starts the server, returning as soon as the server is up and ready to receive requests.
   * <p>
   * This will create new threads that are not daemonized.
   *
   * @throws Exception if the server could not be started
   */
  void start() throws Exception;

  /**
   * Stops the server, returning as soon as the server has stopped receiving requests.
   *
   * @throws Exception if the server could not be stopped cleanly
   */
  void stop() throws Exception;

}
