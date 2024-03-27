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
import io.netty.handler.timeout.IdleStateHandler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class NoopFixedChannelPoolHandler extends ProxyChannelPoolHandler implements InstrumentedChannelPoolHandler {

  private static final String IDLE_STATE_HANDLER_NAME = "idleState";

  private final String host;
  private final Duration idleTimeout;

  public NoopFixedChannelPoolHandler(HttpChannelKey channelKey, Duration idleTimeout) {
    super(channelKey);
    this.host = channelKey.host;
    this.idleTimeout = idleTimeout;
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
