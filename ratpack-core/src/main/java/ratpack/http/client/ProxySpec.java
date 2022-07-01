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

package ratpack.http.client;

import ratpack.api.Nullable;

import java.util.Collection;

/**
 * Class for specifying configuration for an HTTP proxy to utilize for outgoing requests using {@link HttpClient}.
 *
 * @since 1.8.0
 */
public interface ProxySpec {

  /**
   * Configure the host that will proxy outbound HTTP requests.
   * @param host the host name for the HTTP proxy
   * @return {@code this}
   */
  ProxySpec host(String host);

  /**
   * Configure the port on the proxy to will outbound HTTP requests will be sent.
   * @param port the port for the HTTP proxy
   * @return {@code this}
   */
  ProxySpec port(int port);

  /**
   * Configure the username to use when connecting to the proxy.
   *
   * @param username the username to use when connecting to the HTTP proxy
   * @return {@code this}
   */
  ProxySpec username(@Nullable String username);

  /**
   * Configure the password to use when connecting to the proxy.
   *
   * @param password the password to use when connecting to the HTTP proxy
   * @return {@code this}
   */
  ProxySpec password(@Nullable String password);

  /**
   * Configure a collection of patterns for which if any are matched, the outbound request will bypass the HTTP proxy.
   * <p>
   * The format of the values in this list follow the standard
   * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html">Java Networking and Proxies standards</a>
   *
   * @param nonProxyHosts a list of patterns to match the destination host
   * @return {@code this}
   */
  ProxySpec nonProxyHosts(Collection<String> nonProxyHosts);
}
