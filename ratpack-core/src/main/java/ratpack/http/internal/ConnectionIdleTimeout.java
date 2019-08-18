/*
 * Copyright 2017 the original author or authors.
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

package ratpack.http.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ConnectionIdleTimeout implements RequestIdleTimeout {

  private final static AttributeKey<ConnectionIdleTimeout> ATTRIBUTE_KEY = AttributeKey.valueOf(ConnectionIdleTimeout.class.getName());
  private static final String HANDLER_NAME = "timeout";

  private final ChannelPipeline channelPipeline;
  private final Duration timeout;
  boolean handlerRegistered;

  public ConnectionIdleTimeout(ChannelPipeline channelPipeline, Duration timeout) {
    this.channelPipeline = channelPipeline;
    this.timeout = timeout;

    channelPipeline.channel().attr(ATTRIBUTE_KEY).set(this);

    if (!timeout.isZero()) {
      init(timeout);
    }
  }

  public static ConnectionIdleTimeout of(Channel channel) {
    return channel.attr(ATTRIBUTE_KEY).get();
  }

  @Override
  public void setRequestIdleTimeout(Duration duration) {
    if (handlerRegistered) {
      channelPipeline.remove(HANDLER_NAME);
    }
    if (duration.isZero()) {
      handlerRegistered = false;
    } else {
      init(duration);
    }
  }

  public void reset() {
    if (handlerRegistered) {
      channelPipeline.remove(HANDLER_NAME);
    }
    if (!timeout.isZero()) {
      init(timeout);
    }
  }

  private void init(Duration duration) {
    long millis = duration.toMillis();
    channelPipeline.addFirst(HANDLER_NAME, new IdleStateHandler(millis, millis, 0, TimeUnit.MILLISECONDS));
    handlerRegistered = true;
  }

}
