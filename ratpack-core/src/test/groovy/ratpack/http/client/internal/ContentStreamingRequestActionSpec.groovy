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

import groovy.transform.InheritConstructors
import io.netty.channel.Channel
import io.netty.channel.ChannelPipeline
import ratpack.exec.Downstream
import ratpack.exec.Promise
import ratpack.http.client.BaseHttpClientSpec
import ratpack.http.client.HttpClient
import ratpack.http.client.StreamedResponse

import java.util.concurrent.CountDownLatch

class ContentStreamingRequestActionSpec extends BaseHttpClientSpec {

  def "client channel is closed when response is not subscribed to"(pooled, keepalive) {
    given:
    ChannelSpyRequestAction requestAction = null
    def latch = new CountDownLatch(1)
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }

    otherApp { get("foo") { response.headers.set('connection', keepalive); render "bar" } }

    and:
    handlers {
      get { HttpClient httpClient ->
        HttpClientInternal httpClientInternal = httpClient as HttpClientInternal
        requestAction = new ChannelSpyRequestAction(otherAppUrl("foo"), httpClientInternal, 0, execution, {})
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
    requestAction.channel.closeFuture().get() == null

    where:
    pooled << [true, false]
    keepalive << ['keep-alive', 'close']
  }

  @InheritConstructors
  static class ChannelSpyRequestAction extends ContentStreamingRequestAction {
    private Channel channel

    @Override
    protected void addResponseHandlers(ChannelPipeline p, Downstream<? super StreamedResponse> downstream) {
      channel = p.channel()
      super.addResponseHandlers(p, downstream)
    }
  }
}
