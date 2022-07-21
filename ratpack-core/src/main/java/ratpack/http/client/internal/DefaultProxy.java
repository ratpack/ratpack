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

package ratpack.http.client.internal;

import ratpack.api.Nullable;
import ratpack.http.client.ProxyCredentials;
import ratpack.http.client.ProxySpec;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class DefaultProxy implements ProxyInternal {

  private final String host;
  private final int port;
  private final Collection<String> nonProxyHosts;

  private final ProxyCredentials credentials;

  public DefaultProxy(String host, int port, Collection<String> nonProxyHosts, @Nullable ProxyCredentials credentials) {
    this.host = host;
    this.port = port;
    this.nonProxyHosts = nonProxyHosts;
    this.credentials = credentials;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public Collection<String> getNonProxyHosts() {
    return nonProxyHosts;
  }

  @Override
  public ProxyCredentials getCredentials() {
    return credentials;
  }

  @Override
  public boolean shouldProxy(String host) {
    return !isIgnoredForHost(host);
  }

  // Captured from https://github.com/AsyncHttpClient/async-http-client/blob/758dcf214bf0ec08142ba234a3967d98a3dc60ef/client/src/main/java/org/asynchttpclient/proxy/ProxyServer.java

  /**
   * Checks whether proxy should be used according to nonProxyHosts settings of
   * it, or we want to go directly to target host. If <code>null</code> proxy is
   * passed in, this method returns true -- since there is NO proxy, we should
   * avoid to use it. Simple hostname pattern matching using "*" are supported,
   * but only as prefixes.
   *
   * @param hostname the hostname
   * @return true if we have to ignore proxy use (obeying non-proxy hosts
   * settings), false otherwise.
   * @see <a href=
   * "https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html">Networking
   * Properties</a>
   */
  public boolean isIgnoredForHost(String hostname) {
    if (nonProxyHosts != null && nonProxyHosts.size() > 0) {
      for (String nonProxyHost : nonProxyHosts) {
        if (matchNonProxyHost(hostname, nonProxyHost)) {
          return true;
        }
      }
    }

    return false;
  }

  // Captured from https://github.com/AsyncHttpClient/async-http-client/blob/758dcf214bf0ec08142ba234a3967d98a3dc60ef/client/src/main/java/org/asynchttpclient/proxy/ProxyServer.java
  private boolean matchNonProxyHost(String targetHost, String nonProxyHost) {

    if (nonProxyHost.length() > 1) {
      if (nonProxyHost.charAt(0) == '*') {
        return targetHost.regionMatches(true, targetHost.length() - nonProxyHost.length() + 1, nonProxyHost, 1,
          nonProxyHost.length() - 1);
      } else if (nonProxyHost.charAt(nonProxyHost.length() - 1) == '*') {
        return targetHost.regionMatches(true, 0, nonProxyHost, 0, nonProxyHost.length() - 1);
      }
    }

    return nonProxyHost.equalsIgnoreCase(targetHost);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DefaultProxy that = (DefaultProxy) o;

    return host.equals(that.host)
      && port == that.port
      && nonProxyHosts.equals(that.nonProxyHosts)
      && Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, nonProxyHosts, credentials);
  }

  public static class Builder implements ProxySpec {

    private String host;
    private int port;
    private Collection<String> nonProxyHosts = Collections.emptyList();

    private DefaultProxyCredentials credentials;

    @Override
    public ProxySpec host(String host) {
      this.host = host;
      return this;
    }

    @Override
    public ProxySpec port(int port) {
      this.port = port;
      return this;
    }

    @Override
    public ProxySpec nonProxyHosts(Collection<String> nonProxyHosts) {
      this.nonProxyHosts = nonProxyHosts;
      return this;
    }

    @Override
    public ProxySpec credentials(String username, String password) {
      this.credentials = new DefaultProxyCredentials(username, password);
      return this;
    }

    ProxyInternal build() {
      return new DefaultProxy(host, port, nonProxyHosts, credentials);
    }
  }
}
