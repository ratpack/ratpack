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

package ratpack.core.sse.internal

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.util.ByteProcessor
import ratpack.core.sse.ServerSentEvent
import ratpack.test.internal.RatpackGroovyDslSpec

class ServerSentEventStreamDecoderSpec extends RatpackGroovyDslSpec {

  List<ServerSentEvent> events = []
  def decoder = new ServerSentEventDecoder(ByteBufAllocator.DEFAULT, events.&add)

  def cleanup() {
    events*.close()
  }

  def "can decode valid server sent event"() {
    given:
    def bytebuf = Unpooled.copiedBuffer(sseStream.bytes)

    when:
    decoder.decode(bytebuf)
    def event = events.first()

    then:
    event.idAsString == expectedEventId
    event.eventAsString == expectedEventType
    event.dataAsString == expectedEventData

    where:
    sseStream                                       | expectedEventId | expectedEventType | expectedEventData
    "event: fooType\ndata: fooData\nid: fooId\n\n"  | "fooId"         | "fooType"         | "fooData"
    "event: fooType\ndata: fooData\n\n"             | ""              | "fooType"         | "fooData"
    "data: fooData\n\n"                             | ""              | ""                | "fooData"
    "data: fooData\nid: fooId\n\n"                  | "fooId"         | ""                | "fooData"
    "id: fooId\n\n"                                 | "fooId"         | ""                | ""
    "event: fooType\nid: fooId\n\n"                 | "fooId"         | "fooType"         | ""
    "event: fooType\n\n"                            | ""              | "fooType"         | ""
    "data: fooData1\nid: fooId\ndata: fooData2\n\n" | "fooId"         | ""                | "fooData1\nfooData2"
    ":ignore this comment\ndata: fooData\n\n"       | ""              | ""                | "fooData"
    "data: fooData\nfoo: ignore this\n\n"           | ""              | ""                | "fooData"
    "data: fooData\nwhole-line-field-name\n\n"      | ""              | ""                | "fooData"
    "data\n\n"                                      | ""              | ""                | ""
    "data:\nevent:\n\n"                             | ""              | ""                | ""
    "data: foo:data\n\n"                            | ""              | ""                | "foo:data"
    "data:foo\nbar\n\n"                             | ""              | ""                | "foo"
    "data:foo\ndata:\n\n"                           | ""              | ""                | "foo\n"
  }

  def "can decode multiple events"() {
    given:
    def bytebuf = Unpooled.copiedBuffer("data: fooData1\ndata: fooData2\n\nevent:fooType\ndata: fooData3\nid: fooId\n\ndata:fooData4\nid: fooId\n\n".bytes)

    when:
    decoder.decode(bytebuf)

    then:
    events.size() == 3

    events[0].idAsString == ""
    events[0].eventAsString == ""
    events[0].dataAsString == "fooData1\nfooData2"

    events[1].idAsString == "fooId"
    events[1].eventAsString == "fooType"
    events[1].dataAsString == "fooData3"

    events[2].idAsString == "fooId"
    events[2].eventAsString == ""
    events[2].dataAsString == "fooData4"

    and:
    bytebuf.refCnt() == 0
  }

  def "can handle when no events"() {
    given:
    def bytebuf = Unpooled.copiedBuffer(sseStream.bytes)

    when:
    decoder.decode(bytebuf)
    decoder.close()

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
    events[0].idAsString == "1"
    events[0].eventAsString == "foo1"
    events[0].dataAsString == "bar1"
    events[1].idAsString == "2"
    events[1].eventAsString == "foo2"
    events[1].dataAsString == "bar2"
  }

}
