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

import ratpack.stream.Streams
import spock.lang.Specification

class FilteringPublisherSpec extends Specification {

  def "demand is balanced when items are filtered"() {
    when:
    def p = Streams.publish(1..10).filter { it > 5 }
    def s = CollectingSubscriber.subscribe(p)
    s.subscription.request(5)

    then:
    s.received == (6..10).toList()
  }

}
