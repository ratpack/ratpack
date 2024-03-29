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

import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;

public class InstrumentedFixedChannelPoolHandler extends NoopFixedChannelPoolHandler {

  private final LongAdder activeConnectionCount;
  private final int maxConnectionCount;

  public InstrumentedFixedChannelPoolHandler(HttpChannelKey channelKey, int poolSize, Duration idleTimeout) {
    super(channelKey, idleTimeout);
    this.activeConnectionCount = new LongAdder();
    this.maxConnectionCount = poolSize;
  }

  @Override
  public void channelReleased(Channel ch) throws Exception {
    super.channelReleased(ch);
    activeConnectionCount.decrement();
  }

  @Override
  public void channelAcquired(Channel ch) throws Exception {
    super.channelAcquired(ch);
    activeConnectionCount.increment();
  }

  @Override
  public int getActiveConnectionCount() {
    return activeConnectionCount.intValue();
  }

  @Override
  public int getIdleConnectionCount() {
    return maxConnectionCount - getActiveConnectionCount();
  }

}
