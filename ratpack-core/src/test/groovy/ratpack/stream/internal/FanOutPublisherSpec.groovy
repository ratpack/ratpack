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

package ratpack.stream.internal

import ratpack.stream.Streams
import spock.lang.Specification

class FanOutPublisherSpec extends Specification {

  def "only sends requested amount"() {
    when:
    def p = Streams.fanOut(Streams.publish([[1, 2], [3, 4]]))
    List r
    def s = new CollectingSubscriber({ r = it.value }, { it.request(3) })
    p.subscribe(s)

    then:
    s.received == [1, 2, 3]
  }

  def "empty collections don't count towards subscriber demand"() {
    when:
    def p = Streams.fanOut(Streams.publish([[1, 2], [], [], [3, 4]]))
    List r
    def s = new CollectingSubscriber({ r = it.value }, { it.request(3) })
    p.subscribe(s)

    then:
    s.received == [1, 2, 3]
  }

}
