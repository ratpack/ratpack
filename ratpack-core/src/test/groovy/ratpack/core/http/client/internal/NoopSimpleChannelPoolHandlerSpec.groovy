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
import io.netty.channel.ChannelPipeline
import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.Socks4ProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import ratpack.core.http.client.Proxy.Type
import ratpack.exec.Execution
import ratpack.test.internal.BaseRatpackSpec

import java.time.Duration

class NoopSimpleChannelPoolHandlerSpec extends BaseRatpackSpec {

  Execution execution = Mock(Execution)
  URI uri = new URI('https://ratpack.io')
  HttpChannelKey channelKey = new HttpChannelKey(uri, Duration.ofSeconds(30), execution)
  Channel channel = Mock(Channel)
  ChannelPipeline pipeline = Mock(ChannelPipeline)

  void 'it should handle construction and listening to ChannelPool changes'() {
    when:
    NoopSimpleChannelPoolHandler handler = new NoopSimpleChannelPoolHandler(channelKey, null)

    then:
    assert handler.host == 'ratpack.io'
    assert handler.getActiveConnectionCount() == 0
    assert handler.getIdleConnectionCount() == 0

    when:
    handler.channelCreated(channel)

    then:
    assert handler.getActiveConnectionCount() == 0
    assert handler.getIdleConnectionCount() == 0

    when:
    handler.channelAcquired(channel)

    then:
    assert handler.getActiveConnectionCount() == 0
    assert handler.getIdleConnectionCount() == 0

    when:
    handler.channelReleased(channel)

    then:
    assert handler.getActiveConnectionCount() == 0
    assert handler.getIdleConnectionCount() == 0
  }

  void '#handlerType.simpleName is used for #proxyType proxy type'() {
    given:
    DefaultProxy proxy = new DefaultProxy("localhost", 7777, Collections.emptySet(), proxyType)
    NoopSimpleChannelPoolHandler handler = new NoopSimpleChannelPoolHandler(channelKey, proxy)

    when:
    handler.channelCreated(channel)

    then:
    channel.pipeline() >> pipeline
    pipeline.addLast(_) >> { h ->
      assert h.size() == 1
      assert handlerType.isAssignableFrom(h[0][0].class)
      pipeline
    }

    where:
    proxyType   | handlerType
    Type.HTTP   | HttpProxyHandler
    Type.SOCKS4 | Socks4ProxyHandler
    Type.SOCKS5 | Socks5ProxyHandler
  }
}
