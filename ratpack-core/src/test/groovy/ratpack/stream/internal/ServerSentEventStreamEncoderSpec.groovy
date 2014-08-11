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

package ratpack.stream.internal

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import ratpack.stream.ServerSentEvent
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.internal.IoUtils

class ServerSentEventStreamEncoderSpec extends RatpackGroovyDslSpec {

  def publisher = Mock(Publisher)
  def subscriber = Mock(Subscriber)
  def encoder = new DefaultServerSentEventStreamEncoder(publisher)

  def setup() {
    encoder.subscribe(subscriber)
  }

  def "can encode valid server sent events"() {
    when:
    encoder.onNext(sse)

    then:
    1 * subscriber.onNext({
      IoUtils.utf8String(it) == expectedEncoding
    })

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
    encoder.onNext(new ServerSentEvent(null, null, null))

    then:
    def ex = thrown(IllegalArgumentException)
    ex.message == 'You must supply at least one of evenId, eventType, eventData'
  }
}
