/*
 * Copyright 2016 the original author or authors.
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

import java.net.URI;
import java.time.Duration;

class HttpChannelKey {

  final boolean ssl;
  final int port;
  final String host;

  // intentionally not part of the equality check
  final Duration connectTimeout;

  public HttpChannelKey(URI uri, Duration connectTimeout) {
    switch (uri.getScheme()) {
      case "https":
        this.ssl = true;
        break;
      case "http":
        this.ssl = false;
        break;
      default:
        throw new IllegalArgumentException("URI " + uri + " is not HTTP or HTTPS");
    }

    this.port = uri.getPort() < 0 ? ssl ? 443 : 80 : uri.getPort();
    this.host = uri.getHost();
    this.connectTimeout = connectTimeout;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HttpChannelKey that = (HttpChannelKey) o;

    return ssl == that.ssl && port == that.port && host.equals(that.host);
  }

  @Override
  public int hashCode() {
    int result = ssl ? 1 : 0;
    result = 31 * result + port;
    result = 31 * result + host.hashCode();
    return result;
  }

}
