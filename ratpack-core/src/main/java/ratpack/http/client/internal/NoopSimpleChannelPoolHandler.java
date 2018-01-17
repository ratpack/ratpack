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

public class NoopSimpleChannelPoolHandler extends AbstractChannelPoolHandler implements InstrumentedChannelPoolHandler {

  private final String host;

  public NoopSimpleChannelPoolHandler(HttpChannelKey channelKey) {
    this.host = channelKey.host;
  }

  @Override
  public void channelCreated(Channel ch) throws Exception {
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
