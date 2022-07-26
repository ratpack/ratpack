/*
 * Copyright 2022 the original author or authors.
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

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ProxyChannelPoolHandler extends AbstractChannelPoolHandler {

  private final String host;
  private final ProxyInternal proxy;

  public ProxyChannelPoolHandler(HttpChannelKey channelKey) {
    this.host = channelKey.host;
    this.proxy = channelKey.proxy;
  }

  @Override
  public void channelCreated(Channel ch) throws Exception {
    if (proxy != null && proxy.shouldProxy(host)) {
      SocketAddress proxyAddress = new InetSocketAddress(proxy.getHost(), proxy.getPort());

      if (proxy.useSsl()) {
        ch.pipeline().addLast(createSslHandler(ch));
      }

      if (proxy.getCredentials() != null) {
        // HttpProxyHandler throws a NPE passed a null username or a null password.
        ch.pipeline().addLast(new HttpProxyHandler(proxyAddress, proxy.getCredentials().getUsername(), proxy.getCredentials().getPassword()));
      } else {
        ch.pipeline().addLast(new HttpProxyHandler(proxyAddress));
      }
    }
  }

  private SslHandler createSslHandler(Channel ch) throws SSLException {
    SslContext sslCtx = proxy.getSslContext();
    if (sslCtx == null) {
      sslCtx = SslContextBuilder.forClient().build();
    }

    return sslCtx.newHandler(ch.alloc(), proxy.getHost(), proxy.getPort());
  }

}
