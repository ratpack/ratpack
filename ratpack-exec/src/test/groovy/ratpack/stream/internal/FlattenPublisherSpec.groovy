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

package ratpack.stream.internal

import ratpack.stream.StreamEvent
import ratpack.stream.Streams
import spock.lang.Specification

import static ratpack.stream.Streams.flatten

class FlattenPublisherSpec extends Specification {

  def "can flatten publishers into a single stream"() {
    given:
    def p1 = [1, 2].publish()
    def p2 = [3, 4].publish()
    def p3 = [5, 6].publish()
    def p = flatten(Streams.publish([p1, p2, p3]), {})
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(4)

    then:
    s.received == [1, 2, 3, 4]

    when:
    s.subscription.request(1)

    then:
    s.received == [1, 2, 3, 4, 5]

    when:
    s.subscription.request(1)

    then:
    s.received == [1, 2, 3, 4, 5, 6]

    when:
    s.subscription.request(1)

    then:
    s.complete
  }

  def "can flatten single element"() {
    given:
    def p1 = [1, 2].publish()
    def p2 = [3].publish()
    def p = flatten(Streams.publish([p1, p2]), {})
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(4)

    then:
    s.received == [1, 2, 3]
    s.complete
  }

  def "transfers unmet demand to next publisher"() {
    given:
    def p1 = [1, 2].publish()
    def p2 = [3, 4].publish()
    def p = flatten(Streams.publish([p1, p2]), {})
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(3)

    then:
    s.received == [1, 2, 3]

    when:
    s.subscription.request(1)

    then:
    s.received == [1, 2, 3, 4]
  }

  def "can be firehosed"() {
    given:
    def p1 = [1, 2].publish()
    def p2 = [3, 4].publish()
    def p = flatten(Streams.publish([p1, p2]), {})
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(Long.MAX_VALUE)

    then:
    s.received == [1, 2, 3, 4]
  }

  def "emits error and cancels outer when inner fails"() {
    given:
    List<StreamEvent<?>> events = []
    def p1 = [1, 2].publish()
    def p2 = Streams.yield { throw new RuntimeException("!") }
    def p = flatten(Streams.publish([p1, p2]).wiretap(events.&add), {})
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(3)

    then:
    s.received == [1, 2]
    s.error instanceof RuntimeException
    s.error.message == "!"

    events[0].requestAmount == 1
    events[1].item == p1
    events[2].requestAmount == 1
    events[3].item == p2
    events[4].cancel
  }

}
