/*
 * Copyright 2019 the original author or authors.
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

package ratpack.core.http.client;

import java.util.Collection;

/**
 * Configuration data for an HTTP proxy to utilize for outbound requests using {@link HttpClient}.
 *
 * @since 1.8.0
 */
public interface Proxy {

  /**
   * List of valid proxy types.
   */
  enum Type {
    /**
     * HTTP proxy
     */
    HTTP,
    /**
     * SOCKS4 proxy
     */
    SOCKS4,
    /**
     * SOCKS5 proxy
     */
    SOCKS5
  }

  /**
   * The host that proxied requests will be sent.
   *
   * @return The host that proxied requests will be sent.
   */
  String getHost();

  /**
   * The port on the proxy where proxied requests will be sent.
   *
   * @return The port on the proxy where proxied requests will be sent.
   */
  int getPort();

  /**
   * A collection of patterns which if any or matched, the outgoing request will bypass the HTTP proxy.
   * <p>
   * The format of the values in this list follow the standard
   * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html">Java Networking and Proxies standards</a>.
   *
   * @return A collection of patterns which if any or matched, the outgoing request will bypass the HTTP proxy.
   */
  Collection<String> getNonProxyHosts();

  /**
   * The type of the proxy where proxied requests will be sent.
   * @return The type of the proxy where proxied requests will be sent.
   */
  Type getType();
}
