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

package ratpack.impose;

import ratpack.server.ServerConfigBuilder;

/**
 * Forces the port that the server should use to listen for requests.
 * <p>
 * This imposition is used by {@link ServerConfigBuilder}.
 * If present, the imposed value will be used regardless of any previously specified setting on the builder.
 *
 * @see ServerConfigBuilder#port(int)
 * @see Impositions
 * @since 1.2
 */
public final class ForceServerListenPortImposition implements Imposition {

  private final int port;

  private ForceServerListenPortImposition(int port) {
    this.port = port;
  }

  /**
   * Creates an imposition that forces the use of an ephemeral port.
   *
   * @return an imposition for an ephemeral port
   */
  public static ForceServerListenPortImposition ephemeral() {
    return of(0);
  }

  /**
   * Creates an imposition that forces the use of the specified port.
   *
   * @param port the port to force
   * @return an imposition for the specified port
   */
  public static ForceServerListenPortImposition of(int port) {
    if (port < 0) {
      throw new IllegalArgumentException("port must be >= 0");
    }
    return new ForceServerListenPortImposition(port);
  }

  /**
   * The port to be forced.
   *
   * @return the port to be forced
   */
  public int getPort() {
    return port;
  }

}
