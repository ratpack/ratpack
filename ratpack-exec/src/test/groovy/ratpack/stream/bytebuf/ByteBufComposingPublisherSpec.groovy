/*
 * Copyright 2017 the original author or authors.
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

package ratpack.stream.bytebuf

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import ratpack.stream.StreamEvent
import ratpack.stream.Streams
import ratpack.stream.internal.CollectingSubscriber
import spock.lang.Specification

class ByteBufComposingPublisherSpec extends Specification {

  def "can batch requests to publisher"() {
    given:
    List<StreamEvent<ByteBuf>> events = []
    def upstream = Streams.yield { it.requestNum < 4 ? Unpooled.copyLong(it.requestNum) : null }.wiretap(events.&add)
    def p = ByteBufStreams.buffer(upstream, 20, 5, UnpooledByteBufAllocator.DEFAULT)
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(1)

    then:
    events[0].requestAmount == 1
    events[1].data
    events[2].requestAmount == 1
    events[3].data
    events[4].requestAmount == 1
    events[5].data
    events.size() == 6

    s.received[0].readableBytes() == 24

    when:
    s.received[0].release()

    then:
    events[1].item.refCnt() == 0
    events[3].item.refCnt() == 0
    events[5].item.refCnt() == 0

    when:
    s.subscription.request(1)

    then:
    events[6].requestAmount == 1
    events[7].data
    events[8].requestAmount == 1
    events[9].complete

    and:
    s.received[1].readableBytes() == 8
    s.received.size() == 2

    cleanup:
    s.received.each {
      if (it.refCnt() > 0) {
        it.release(it.refCnt())
      }
    }
  }

}
