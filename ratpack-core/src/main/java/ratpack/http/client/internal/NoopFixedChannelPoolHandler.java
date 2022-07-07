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

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class NoopFixedChannelPoolHandler extends AbstractChannelPoolHandler implements InstrumentedChannelPoolHandler {

  private static final String IDLE_STATE_HANDLER_NAME = "idleState";

  private final String host;
  private final Duration idleTimeout;
  private final ProxyInternal proxy;

  public NoopFixedChannelPoolHandler(HttpChannelKey channelKey, Duration idleTimeout, ProxyInternal proxy) {
    this.host = channelKey.host;
    this.idleTimeout = idleTimeout;
    this.proxy = proxy;
  }

  @Override
  public void channelCreated(Channel ch) throws Exception {
    if  (proxy != null && proxy.shouldProxy(host)) {
      SocketAddress proxyAddress = new InetSocketAddress(proxy.getHost(), proxy.getPort());
      if (proxy.getCredentials() != null) {
        // HttpProxyHandler throws a NPE passed a null username or a null password.
        ch.pipeline().addLast(new HttpProxyHandler(proxyAddress, proxy.getCredentials().getUsername(), proxy.getCredentials().getPassword()));
      } else {
        ch.pipeline().addLast(new HttpProxyHandler(proxyAddress));
      }
    }
  }

  @Override
  public void channelReleased(Channel ch) throws Exception {
    if (ch.isOpen()) {
      ch.config().setAutoRead(true);
      ch.pipeline().addLast(IdlingConnectionHandler.INSTANCE);
      if (idleTimeout.toNanos() > 0) {
        ch.pipeline().addLast(IDLE_STATE_HANDLER_NAME, new IdleStateHandler(idleTimeout.toNanos(), idleTimeout.toNanos(), 0, TimeUnit.NANOSECONDS));
        ch.pipeline().addLast(IdleTimeoutHandler.INSTANCE);
      }
    }
  }

  @Override
  public void channelAcquired(Channel ch) throws Exception {
    if (ch.pipeline().context(IdlingConnectionHandler.INSTANCE) != null) {
      ch.pipeline().remove(IdlingConnectionHandler.INSTANCE);
    }
    if (ch.pipeline().context(IDLE_STATE_HANDLER_NAME) != null) {
      ch.pipeline().remove(IDLE_STATE_HANDLER_NAME);
    }
    if (ch.pipeline().context(IdleTimeoutHandler.INSTANCE) != null) {
      ch.pipeline().remove(IdleTimeoutHandler.INSTANCE);
    }
  }

  @Override
  public String getHost() {
    return this.host;
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
