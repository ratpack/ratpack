/*
 * Copyright 2015 the original author or authors.
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

package ratpack.sse.internal

import io.netty.buffer.Unpooled
import io.netty.util.ByteProcessor
import ratpack.sse.Event
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.TestByteBufAllocators

class ServerSentEventStreamDecoderSpec extends RatpackGroovyDslSpec {

  List<Event<?>> events = []
  def decoder = new ServerSentEventDecoder(TestByteBufAllocators.LEAKING_UNPOOLED_HEAP, events.&add)

  def "can decode valid server sent event"() {
    given:
    def bytebuf = Unpooled.copiedBuffer(sseStream.bytes)

    when:
    decoder.decode(bytebuf)
    def event = events.first()

    then:
    event.id == expectedEventId
    event.event == expectedEventType
    event.data == expectedEventData

    where:
    sseStream                                       | expectedEventId | expectedEventType | expectedEventData
    "event: fooType\ndata: fooData\nid: fooId\n\n"  | "fooId"         | "fooType"         | "fooData"
    "event: fooType\ndata: fooData\n\n"             | null            | "fooType"         | "fooData"
    "data: fooData\n\n"                             | null            | null              | "fooData"
    "data: fooData\nid: fooId\n\n"                  | "fooId"         | null              | "fooData"
    "id: fooId\n\n"                                 | "fooId"         | null              | null
    "event: fooType\nid: fooId\n\n"                 | "fooId"         | "fooType"         | null
    "event: fooType\n\n"                            | null            | "fooType"         | null
    "data: fooData1\nid: fooId\ndata: fooData2\n\n" | "fooId"         | null              | "fooData1\nfooData2"
    ":ignore this comment\ndata: fooData\n\n"       | null            | null              | "fooData"
    "data: fooData\nfoo: ignore this\n\n"           | null            | null              | "fooData"
    "data: fooData\nwhole-line-field-name\n\n"      | null            | null              | "fooData"
    "data\n\n"                                      | null            | null              | ""
    "data:\nevent:\n\n"                             | null            | ""                | ""
    "data: foo:data\n\n"                            | null            | null              | "foo:data"
    "data:foo\nbar\n\n"                             | null            | null              | "foo"
    "data:foo\ndata:\n\n"                           | null            | null              | "foo\n"
  }

  def "can decode multiple events"() {
    given:
    def bytebuf = Unpooled.copiedBuffer("data: fooData1\ndata: fooData2\n\nevent:fooType\ndata: fooData3\nid: fooId\n\ndata:fooData4\nid: fooId\n\n".bytes)

    when:
    decoder.decode(bytebuf)

    then:
    events.size() == 3

    events[0].id == null
    events[0].event == null
    events[0].data == "fooData1\nfooData2"

    events[1].id == "fooId"
    events[1].event == "fooType"
    events[1].data == "fooData3"

    events[2].id == "fooId"
    events[2].event == null
    events[2].data == "fooData4"

    and:
    bytebuf.refCnt() == 0
  }

  def "can handle when no events"() {
    given:
    def bytebuf = Unpooled.copiedBuffer(sseStream.bytes)

    when:
    decoder.decode(bytebuf)

    then:
    events.empty

    where:
    sseStream << ["data: fooData1\ndata: fooData2\n", "just a line with no EOL"]
  }

  def "can decode data split across bytebufs"() {
    given:
    def bytebuf = Unpooled.copiedBuffer("""id: 1
event: foo1
data: bar1

id: 2
event: foo2
data: bar2

""".bytes)

    when:
    bytebuf.forEachByte({
      def b = Unpooled.wrappedBuffer([it] as byte[])
      decoder.decode(b)
      true
    } as ByteProcessor)

    then:
    events[0].id == "1"
    events[0].event == "foo1"
    events[0].data == "bar1"
    events[1].id == "2"
    events[1].event == "foo2"
    events[1].data == "bar2"
  }

}
