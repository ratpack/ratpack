/*
 * Copyright 2022 the original author or authors.
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

package ratpack.exec.stream.internal

import ratpack.test.internal.BaseRatpackSpec

import java.util.concurrent.atomic.AtomicInteger

class TakeWhilePublisherSpec extends BaseRatpackSpec {

  def "returning false will interrupt processing"() {
    given:
    def counter = new AtomicInteger(0)
    def p = (1..100).publish().map {
      counter.incrementAndGet()
      it
    }.takeWhile { counter.get() <= 50 }
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(80)

    then:
    counter.get() == 51
    s.received == (1..50).toList()
    s.complete
  }

  def "returning true for all elements, results in all elements received"() {
    given:
    def p = (1..40).publish().take(50)
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(Integer.MAX_VALUE)

    then:
    s.received == (1..40).toList()
    s.complete
  }
}
