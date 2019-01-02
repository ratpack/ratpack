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
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import org.reactivestreams.Subscriber
import org.reactivestreams.tck.SubscriberBlackboxVerification
import org.reactivestreams.tck.TestEnvironment
import ratpack.server.internal.DefaultResponseTransmitter

import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

class DefaultResponseTransmitterBlackboxVerification extends SubscriberBlackboxVerification<ByteBuf> {

  public DefaultResponseTransmitterBlackboxVerification() {
    super(new TestEnvironment(3000L))
  }

  @Override
  Subscriber<ByteBuf> createSubscriber() {
    ChannelFuture channelFuture = mock(ChannelFuture)

    Channel channel = mock(Channel, RETURNS_DEEP_STUBS)
    when(channel.isOpen()).thenReturn(true)
    when(channel.writeAndFlush(any()).addListener(any())).thenReturn(channelFuture)
    when(channel.isWritable()).thenReturn(true)

    FullHttpRequest nettyRequest = mock(FullHttpRequest, RETURNS_DEEP_STUBS)
    when(nettyRequest.retain()).thenReturn(nettyRequest)
    when(nettyRequest.headers().get(any())).thenReturn(null)
    when(nettyRequest.protocolVersion().isKeepAliveDefault()).thenReturn(false)

    HttpHeaders responseHeaders = mock(HttpHeaders)


    new DefaultResponseTransmitter(new AtomicBoolean(), channel, Clock.systemDefaultZone(), nettyRequest, null, responseHeaders, null).transmitter(HttpResponseStatus.OK)
  }

  @Override
  ByteBuf createElement(int element) {
    Unpooled.wrappedBuffer([element.byteValue()] as byte[])
  }

}
