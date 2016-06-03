/*
 * Copyright 2014 the original author or authors.
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

package ratpack.http.client.internal

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelPipeline
import io.netty.channel.pool.ChannelPool
import io.netty.channel.pool.ChannelPoolMap
import ratpack.exec.Downstream
import ratpack.exec.ExecController
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.http.client.HttpClientSpec
import ratpack.http.client.RequestSpec
import ratpack.http.client.StreamedResponse
import spock.lang.Unroll

import java.util.concurrent.CountDownLatch

@Unroll
class ContentStreamingRequestActionSpec extends HttpClientSpec implements PooledHttpClientFactory {

  def "client channel is closed when response is not subscribed to"(pooled, keepalive) {
    def requestAction
    def latch = new CountDownLatch(1)

    given:
    otherApp { get("foo") { response.headers.set('connection', keepalive) ; render "bar" } }

    and:
    handlers {
      get { ExecController execController, ByteBufAllocator byteBufAllocator ->
        def map = createClient(context, pooled).channelPoolMap
        requestAction = new ChannelSpyRequestAction({}, map, pooled, otherAppUrl("foo"), byteBufAllocator, execution)
        Promise.async(requestAction).then {
          execution.onComplete {
            latch.countDown()
          }
          render 'foo'
        }
      }
    }

    expect:
    text == 'foo'
    latch.await()
    assert !requestAction.channel.open

    where:
    pooled << [new PooledHttpConfig(pooled: true), new PooledHttpConfig(pooled: false)]
    keepalive << ['keep-alive','close']
  }

  static class ChannelSpyRequestAction extends ContentStreamingRequestAction {
    private Channel channel

    ChannelSpyRequestAction(Action<? super RequestSpec> requestConfigurer, ChannelPoolMap<URI, ChannelPool> map, PooledHttpConfig config, URI uri, ByteBufAllocator byteBufAllocator, Execution execution) {
      super(requestConfigurer, map, config, uri, byteBufAllocator, execution, 0)
    }

    @Override
    protected void addResponseHandlers(ChannelPipeline p, Downstream<? super StreamedResponse> downstream) {
      channel = p.channel()
      super.addResponseHandlers(p, downstream)
    }
  }
}
