/*
 * Copyright 2016 the original author or authors.
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
import spock.lang.Specification

class BatchPublisherSpec extends Specification {

  def "can batch requests to publisher"() {
    given:
    def disposed = []
    List<StreamEvent<Integer>> events = []
    def p = (1..10).publish().wiretap(events.&add).batch(3, disposed.&add)
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(1)

    then:
    s.received == [1]
    events[0].requestAmount == 3
    events[1].item == 1
    events[2].item == 2
    events[3].item == 3

    when:
    s.subscription.request(1)

    then:
    events.size() == 4 // no extra upstream events, pull from buffer
    s.received == (1..2)

    when:
    s.subscription.request(1)

    then:
    events.size() == 4 // no extra upstream events, pull from buffer
    s.received == (1..3)

    when:
    s.subscription.request(4)

    then:
    events[4].requestAmount == 3
    events[5].item == 4
    events[6].item == 5
    events[7].item == 6
    events[8].requestAmount == 3
    events[9].item == 7
    events[10].item == 8
    events[11].item == 9
    s.received == (1..7)

    when:
    s.subscription.cancel()

    then:
    disposed == (8..9)
  }

}
