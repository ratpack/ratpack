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

import io.netty.channel.EventLoop;
import io.netty.handler.ssl.SslContext;
import ratpack.api.Nullable;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

final class HttpChannelKey {

  final boolean ssl;
  final int port;
  final String host;

  final ProxyInternal proxy;

  final ExecController execController;
  final EventLoop eventLoop;

  final Duration connectTimeout;

  final SslContext sslContext;

  HttpChannelKey(URI uri, Duration connectTimeout, Execution execution) {
    this(uri, null, connectTimeout, execution, null);
  }

    HttpChannelKey(URI uri, @Nullable ProxyInternal proxy, Duration connectTimeout, Execution execution, SslContext sslContext) {
      this.sslContext = sslContext;
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
    this.proxy = proxy;
    this.execController = execution.getController();
    this.eventLoop = execution.getEventLoop();
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

    return execController == that.execController
      && ssl == that.ssl
      && port == that.port
      && host.equals(that.host)
      && Objects.equals(proxy, that.proxy);
  }

  @Override
  public int hashCode() {
    int result = ssl ? 1 : 0;
    result = 31 * result + port;
    result = 31 * result + host.hashCode();
    result = 31 * result + execController.hashCode();
    if (proxy != null) {
      result = 31 * result + proxy.hashCode();
    }
    return result;
  }

}
