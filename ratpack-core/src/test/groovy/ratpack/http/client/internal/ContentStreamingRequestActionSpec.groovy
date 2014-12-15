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
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.channel.SimpleChannelInboundHandler
import ratpack.exec.ExecController
import ratpack.exec.Execution
import ratpack.exec.Fulfiller
import ratpack.func.Action
import ratpack.http.client.HttpClientSpec
import ratpack.http.client.RequestSpec
import ratpack.http.client.StreamedResponse

class ContentStreamingRequestActionSpec extends HttpClientSpec {

  def "client channel is closed when response is not subscribed to"() {
    def requestAction

    given:
    otherApp { get("foo") { render "bar" } }

    and:
    handlers {
      get {
        ExecController execController = launchConfig.execController
        ByteBufAllocator byteBufAllocator = launchConfig.bufferAllocator

        requestAction = new ChannelSpyRequestAction({}, otherAppUrl("foo"), execution, byteBufAllocator)
        execController.control.promise(requestAction).then {
          render 'foo'
        }
      }
    }

    expect:
    text == 'foo'
    !requestAction.channel.open
  }

  static class ChannelSpyRequestAction extends ContentStreamingRequestAction {
    private Channel channel

    ChannelSpyRequestAction(Action<? super RequestSpec> requestConfigurer, URI uri, Execution execution, ByteBufAllocator byteBufAllocator) {
      super(requestConfigurer, uri, execution, byteBufAllocator)
    }

    @Override
    protected void addResponseHandlers(ChannelPipeline p, Fulfiller<? super StreamedResponse> fulfiller) {
      super.addResponseHandlers(p, fulfiller)
      p.addFirst(new SimpleChannelInboundHandler(false) {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
          channel = ctx.channel()
          ctx.fireChannelRead(msg)
        }
      })
    }
  }
}
