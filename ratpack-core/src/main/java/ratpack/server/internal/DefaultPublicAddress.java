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

package ratpack.server.internal;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import ratpack.handling.Context;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.server.BindAddress;
import ratpack.server.PublicAddress;
import ratpack.util.ExceptionUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A best-effort attempt at providing meaningful default behaviors for determining the appropriate advertised address for a request.
 * This implementation supports a number of strategies, depending on what information is available:
 *
 * <ul>
 *   <li>An optional configured public address URI</li>
 *   <li>An absolute request URI</li>
 *   <li>A Host header included in the request</li>
 *   <li>The bind address of the service</li>
 * </ul>
 *
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.23">Header Field Definitions: Host</a>
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.2">Request: The Resource Identified by a Request</a>
 */
public class DefaultPublicAddress implements PublicAddress {

  private final URI publicAddress;
  private final String scheme;

  public DefaultPublicAddress(URI publicAddress, String scheme) {
    this.publicAddress = publicAddress;
    this.scheme = scheme;
  }

  public URI getAddress(Context context) {
    URI currentUrl;
    if (this.publicAddress != null) {
      currentUrl = this.publicAddress;
    } else {
      String scheme = this.scheme;
      String host;
      int port;
      URI absoluteRequestURI = getAbsoluteRequestUri(context);
      if (absoluteRequestURI != null) {
        scheme = absoluteRequestURI.getScheme();
        host = absoluteRequestURI.getHost();
        port = absoluteRequestURI.getPort();
      } else {
        HostAndPort hostData = getHostData(context);
        if (hostData != null) {
          host = hostData.getHostText();
          port = hostData.getPortOrDefault(-1);
        } else {
          BindAddress bindAddress = context.getBindAddress();
          host = bindAddress.getHost();
          port = bindAddress.getPort();
        }
      }
      try {
        currentUrl = new URI(scheme, null, host, port, null, null, null);
      } catch (URISyntaxException ex) {
        throw ExceptionUtils.uncheck(ex);
      }
    }
    return currentUrl;
  }

  private URI getAbsoluteRequestUri(Context context) {
    String rawUri = Strings.nullToEmpty(context.getRequest().getRawUri());
    if (rawUri.isEmpty() || rawUri.startsWith("/")) {
      return null;
    }
    return URI.create(rawUri);
  }

  private HostAndPort getHostData(Context context) {
    HostAndPort hostData = null;
    String hostHeader = Strings.nullToEmpty(context.getRequest().getHeaders().get(HttpHeaderConstants.HOST.toString()));
    if (!hostHeader.isEmpty()) {
      hostData = HostAndPort.fromString(hostHeader);
    }
    return hostData;
  }

}
