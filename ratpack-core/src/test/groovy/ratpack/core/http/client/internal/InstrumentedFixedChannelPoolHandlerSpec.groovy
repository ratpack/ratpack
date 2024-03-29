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

package ratpack.core.http.client.internal

import io.netty.channel.Channel
import io.netty.channel.ChannelConfig
import io.netty.channel.ChannelPipeline
import ratpack.exec.Execution
import ratpack.test.internal.BaseRatpackSpec

import java.time.Duration

class InstrumentedFixedChannelPoolHandlerSpec extends BaseRatpackSpec {

  int poolSize = 100
  Execution execution = Mock(Execution)
  URI uri = new URI('https://ratpack.io')
    HttpChannelKey channelKey = new HttpChannelKey(uri, Duration.ofSeconds(30), execution)
  ChannelPipeline pipeline = Mock(ChannelPipeline) {
    _ * remove(IdlingConnectionHandler.INSTANCE)
    _ * addLast(IdlingConnectionHandler.INSTANCE)
  }
  ChannelConfig config = Mock(ChannelConfig) {
    _ * setAutoRead(true)
  }
  Channel channel = Mock(Channel) {
    _ * config() >> config
    _ * isOpen() >> true
    _ * pipeline() >> pipeline
  }

  void 'it should handle construction and listening to ChannelPool changes'() {
    when:
    InstrumentedFixedChannelPoolHandler handler = new InstrumentedFixedChannelPoolHandler(channelKey, poolSize, Duration.ofSeconds(15))

    then:
    assert handler.host == 'ratpack.io'
    assert handler.getActiveConnectionCount() == 0
    assert handler.getIdleConnectionCount() == poolSize

    when:
    handler.channelCreated(channel)

    then:
    assert handler.getActiveConnectionCount() == 0
    assert handler.getIdleConnectionCount() == poolSize

    when:
    handler.channelAcquired(channel)

    then:
    assert handler.getActiveConnectionCount() == 1
    assert handler.getIdleConnectionCount() == poolSize - 1

    when:
    handler.channelAcquired(channel)

    then:
    assert handler.getActiveConnectionCount() == 2
    assert handler.getIdleConnectionCount() == poolSize - 2

    when:
    handler.channelReleased(channel)

    then:
    assert handler.getActiveConnectionCount() == 1
    assert handler.getIdleConnectionCount() == poolSize - 1

    when:
    handler.channelReleased(channel)

    then:
    assert handler.getActiveConnectionCount() == 0
    assert handler.getIdleConnectionCount() == poolSize
  }

}
