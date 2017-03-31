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
import spock.lang.Specification

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch

class BufferingPublisherSpec extends Specification {

  BufferedWriteStream<Integer> writeStream = null
  long requested
  boolean cancelled
  def disposed = new ConcurrentLinkedQueue()
  def p = new BufferingPublisher<Integer>(disposed.&add as Action, { w ->
    writeStream = w
    new Subscription() {
      @Override
      void request(long n) {
        requested = n
      }

      @Override
      void cancel() {
        cancelled = true
      }
    }
  } as Function)

  def subscriber = new CollectingSubscriber()
  private RuntimeException exception = new RuntimeException("!")

  def "can write/buffer/request/drain"() {
    when:
    p.subscribe(subscriber)

    then:
    writeStream == null

    when:
    subscriber.subscription.request(1)

    then:
    writeStream.buffered == 0
    writeStream.requested == 1

    when:
    writeStream.item(1)

    then:
    writeStream.buffered == 0
    writeStream.requested == 0

    when:
    writeStream.item(1)

    then:
    writeStream.buffered == 1
    writeStream.requested == 0

    when:
    subscriber.subscription.request(5)

    then:
    subscriber.received.size() == 2
    writeStream.buffered == 0
    writeStream.requested == 4

    when:
    writeStream.item(1)

    then:
    writeStream.buffered == 0
    writeStream.requested == 3

    when:
    writeStream.item(1)
    writeStream.item(1)
    writeStream.item(1)
    writeStream.item(1)

    then:
    writeStream.buffered == 1
    writeStream.requested == 0
  }

  def "buffered are disposed of when cancelled"() {
    when:
    p.subscribe(subscriber)
    subscriber.subscription.request(1)
    writeStream.item(1)
    writeStream.item(2)
    writeStream.item(3)
    subscriber.subscription.cancel()

    then:
    disposed.toList() == [2, 3]
  }

  def "buffered are disposed of on error"() {
    when:
    p.subscribe(subscriber)
    subscriber.subscription.request(1)
    writeStream.item(1)
    writeStream.item(2)
    writeStream.item(3)
    writeStream.error(exception)

    then:
    disposed.toList() == [2, 3]
    subscriber.error == exception
    subscriber.received == [1]
  }

  def "any written after error are disposed of"() {
    when:
    p.subscribe(subscriber)
    subscriber.subscription.request(3)
    writeStream.item(1)
    writeStream.error(exception)
    writeStream.item(2)

    then:
    disposed.toList() == [2]
    subscriber.error == exception
    subscriber.received == [1]
  }

  def "any written after complete are disposed of"() {
    when:
    p.subscribe(subscriber)
    subscriber.subscription.request(3)
    writeStream.item(1)
    writeStream.complete()
    writeStream.item(2)

    then:
    disposed.toList() == [2]
    subscriber.error == null
    subscriber.received == [1]
  }

  def "any written after cancel are disposed of"() {
    when:
    p.subscribe(subscriber)
    subscriber.subscription.request(3)
    writeStream.item(1)
    subscriber.subscription.cancel()
    writeStream.item(2)

    then:
    disposed.toList() == [2]
    subscriber.error == null
    subscriber.received == [1]
  }

  def "can write/buffer/request/drain async"() {
    given:
    def latch = new CountDownLatch(1)
    subscriber = new CollectingSubscriber<Integer>({ latch.countDown() }, {}) {
      @Override
      void onNext(Integer o) {
        super.onNext(o)
        if (received.size() % 5 == 0) {
          Thread.start {
            sleep 10
            subscription.request(5)
          }
        }
      }
    }

    p.subscribe(subscriber)
    subscriber.subscription.request(5)

    when:
    def queue = new ConcurrentLinkedQueue()
    1000.times { queue.add it }

    Thread.start {
      10.times {
        def innerLatch = new CountDownLatch(1)
        Thread.start {
          100.times {
            def v = queue.poll()
            if (v != null) {
              writeStream.item(it)
            }
            if (v == 999) {
              writeStream.complete()
            }
            sleep 1
          }
          innerLatch.countDown()
        }
        innerLatch.await()
      }
    }

    then:
    latch.await()
  }

  def "indicates when cancelled"() {
    when:
    p.subscribe(subscriber)
    subscriber.subscription.request(1)

    then:
    !writeStream.cancelled

    when:
    subscriber.subscription.cancel()

    then:
    writeStream.cancelled
  }


}
