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

import groovy.util.logging.Slf4j
import org.reactivestreams.Subscription
import ratpack.exec.BaseExecutionSpec
import ratpack.exec.Execution
import ratpack.func.Action
import ratpack.func.Function
import ratpack.stream.Streams

import java.time.Duration

@Slf4j
class FanOutPublisherSpec extends BaseExecutionSpec {


  def "only sends requested amount"() {
    when:
    def p = Streams.fanOut(Streams.publish([[1, 2], [3, 4]]))

    def s = new CollectingSubscriber({ log.info it.value }, { it.request(3) })
    p.subscribe(s)

    then:
    !s.complete
    s.received == [1, 2, 3]
  }

  def "empty collections don't count towards subscriber demand"() {
    when:
    def p = Streams.fanOut(Streams.publish([[1, 2], [], [], [3, 4]]))

    def s = new CollectingSubscriber({ log.info it.value }, { it.request(3) })
    p.subscribe(s)

    then:
    !s.complete
    s.received == [1, 2, 3]
  }

  def "empty publisher can be consumed"() {
    given:
    def p = Streams.fanOut(Streams.publish([]))
    def s = CollectingSubscriber.subscribe(p)

    when:
    s.subscription.request(10)

    then:
    s.complete
    s.received == []
  }

  def "handles downstream request outside of onNext"() {
    /*
      This tests the scenario where there is async downstream, so demand requests may overlap.
      It tests that the publisher knows when it has made requests of the upstream and never
      requests when there is already a pending request.
    */
    when:
    def integers = 1..10
    def v = execHarness.yield {
      Streams.fanOut(
        Streams.publish(integers.collect { [it] })
          .flatMap { i -> Execution.sleep(Duration.ofMillis(1)).map { i } }
      )
        .batch(3, Action.noop())
        .toList()
    }.valueOrThrow

    then:
    v == integers.toList()
  }

  def "disposes extra items"() {
    when:
    BufferedWriteStream w
    def p = new BufferingPublisher<List<Integer>>(Action.noop(), { write ->
      w = write
      new Subscription() {
        @Override
        void request(long n) {
          n.times { write.item([1, 2, 3]) }
        }

        @Override
        void cancel() {

        }
      }
    } as Function)

    def disposed = []
    def f = Streams.fanOut(p, disposed.&add)
    def s = new CollectingSubscriber()
    f.subscribe(s)

    then:
    s.subscription.request(1)
    s.received == [1]
    w.error(new Exception("!"))
    s.error.message == "!"
    disposed == [2, 3]
  }
}

