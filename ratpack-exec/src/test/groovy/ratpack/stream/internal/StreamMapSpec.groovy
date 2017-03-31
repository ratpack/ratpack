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

import org.reactivestreams.Subscription
import ratpack.func.Action
import ratpack.func.Function
import ratpack.stream.StreamMapper
import spock.lang.Specification

class StreamMapSpec extends Specification {

  def "upstream not subscribed if mapper fails to establish"() {
    when:
    def established = false
    def error = new RuntimeException()
    def p = new BufferingPublisher(Action.noop(), {
      established = true
      return new Subscription() {
        @Override
        void request(long n) {

        }

        @Override
        void cancel() {
        }
      }
    } as Function).streamMap({ s, down ->
      throw error
    } as StreamMapper)

    then:
    !established

    when:
    def s = CollectingSubscriber.subscribe(p)
    s.subscription.request(1)

    then:
    !established
    s.error == error
  }

  def "upstream is cancelled if item map fails"() {
    when:
    def cancelled = false
    def error = new RuntimeException()
    def p = new BufferingPublisher(Action.noop(), { down ->
      return new Subscription() {
        @Override
        void request(long n) {
          n.times { down.item(it) }
        }

        @Override
        void cancel() {
          cancelled = true
        }
      }
    } as Function).streamMap({ s, down ->
      down.itemMap(s) { throw error }
    } as StreamMapper)

    then:
    !cancelled

    when:
    def s = CollectingSubscriber.subscribe(p)
    s.subscription.request(1)

    then:
    s.error == error
    cancelled
  }
}
