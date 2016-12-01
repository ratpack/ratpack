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

import spock.lang.Specification

import static ratpack.stream.Streams.concat

class ConcatPublisherSpec extends Specification {

  def "can concatenate publishers into a single stream"() {
    given:
    def p1 = (1..10).publish()
    def p2 = (11..15).publish()
    def p3 = (16..100).publish()
    def p = concat(p1, p2, p3)
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(8)

    then:
    s.received == (1..8)
    !s.complete

    when:
    s.subscription.request(7)

    then:
    s.received == (1..15)
    !s.complete

    when:
    s.subscription.request(1000)

    then:
    s.received == (1..100)
    s.complete
  }

}
