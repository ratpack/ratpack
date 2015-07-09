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


package ratpack.stream.tck

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.reactivestreams.Subscriber
import org.reactivestreams.tck.SubscriberBlackboxVerification
import org.reactivestreams.tck.TestEnvironment
import ratpack.websocket.WebSocket
import ratpack.websocket.internal.WebsocketBroadcastSubscriber

import static org.mockito.Mockito.mock

class WebsocketBroadcastSubscriberBlackboxVerification extends SubscriberBlackboxVerification<ByteBuf> {

  public WebsocketBroadcastSubscriberBlackboxVerification() {
    super(new TestEnvironment(10000L))
  }

  @Override
  Subscriber<ByteBuf> createSubscriber() {
    WebSocket ws = mock(WebSocket)
    new WebsocketBroadcastSubscriber(ws)
  }

  @Override
  ByteBuf createElement(int element) {
    Unpooled.copiedBuffer(element.toString(), CharsetUtil.UTF_8)
  }
}
