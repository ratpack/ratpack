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

package ratpack.server.internal

import ratpack.http.ServerSentEvent
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.internal.IoUtils

class ServerSentEventEncoderSpec extends RatpackGroovyDslSpec {

  def encoder = new ServerSentEventEncoder()
  def outList = []

  def "can encode valid server sent events"() {
    when:
    encoder.encode(null, sse, outList)

    then:
    outList.size() == 1
    IoUtils.utf8String(outList[0].content) == expectedEncoding

    where:
    sse                                                 | expectedEncoding
    new ServerSentEvent("fooId", "fooType", "fooData")  | "event: fooType\ndata: fooData\nid: fooId\n\n"
    new ServerSentEvent(null, "fooType", "fooData")     | "event: fooType\ndata: fooData\n\n"
    new ServerSentEvent(null, null, "fooData")          | "data: fooData\n\n"
    new ServerSentEvent("fooId", null, "fooData")       | "data: fooData\nid: fooId\n\n"
    new ServerSentEvent("fooId", null, null)            | "id: fooId\n\n"
    new ServerSentEvent("fooId", "fooType", null)       | "event: fooType\nid: fooId\n\n"
    new ServerSentEvent(null, "fooType", null)          | "event: fooType\n\n"

  }

  def "throws exception if server sent event has no fields set"() {
    when:
    encoder.encode(null, new ServerSentEvent(null, null, null), outList)

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message == 'You must supply at least one of evenId, eventType, eventData'
  }
}
