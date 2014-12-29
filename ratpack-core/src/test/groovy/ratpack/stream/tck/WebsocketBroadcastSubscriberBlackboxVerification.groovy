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
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.tck.SubscriberBlackboxVerification
import org.reactivestreams.tck.TestEnvironment
import ratpack.websocket.WebSocket
import ratpack.websocket.internal.WebsocketBroadcastSubscriber

import static org.mockito.Mockito.mock
import static ratpack.stream.Streams.constant
import static ratpack.stream.Streams.publish

class WebsocketBroadcastSubscriberBlackboxVerification extends SubscriberBlackboxVerification<ByteBuf> {

  public static final long DEFAULT_TIMEOUT_MILLIS = 3000L

  public WebsocketBroadcastSubscriberBlackboxVerification() {
    super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS))
  }

  @Override
  Subscriber<ByteBuf> createSubscriber() {
    WebSocket ws = mock(WebSocket)
    new WebsocketBroadcastSubscriber(ws)
  }

  @Override
  Publisher<ByteBuf> createHelperPublisher(long elements) {
    if (elements == Long.MAX_VALUE) {
      constant(Unpooled.copiedBuffer("foo", CharsetUtil.UTF_8))
    } else if (elements > 0) {
      publish(0..<elements).map { Unpooled.copiedBuffer(it.toString(), CharsetUtil.UTF_8) }
    } else {
      publish([])
    }
  }
}
