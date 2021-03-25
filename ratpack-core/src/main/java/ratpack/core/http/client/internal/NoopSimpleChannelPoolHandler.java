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

package ratpack.core.http.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import ratpack.func.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NoopSimpleChannelPoolHandler extends AbstractChannelPoolHandler implements InstrumentedChannelPoolHandler {

  private final String host;

  @Nullable
  private final ProxyInternal proxy;

  public NoopSimpleChannelPoolHandler(HttpChannelKey channelKey, @Nullable ProxyInternal proxy) {
    this.host = channelKey.host;
    this.proxy = proxy;
  }

  @Override
  public void channelCreated(Channel ch) {
    if (proxy != null && proxy.shouldProxy(host)) {
      SocketAddress proxyAddress = new InetSocketAddress(proxy.getHost(), proxy.getPort());
      ch.pipeline().addLast(new HttpProxyHandler(proxyAddress));
    }
  }

  @Override
  public void channelReleased(Channel ch) throws Exception {
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getActiveConnectionCount() {
    return 0;
  }

  @Override
  public int getIdleConnectionCount() {
    return 0;
  }
}
