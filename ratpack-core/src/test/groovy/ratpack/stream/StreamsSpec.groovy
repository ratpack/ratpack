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


package ratpack.stream

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Specification

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

import static ratpack.stream.Streams.*

class StreamsSpec extends Specification {

  def "can buffer publisher"() {
    given:
    def sent = []
    def received = []
    boolean complete
    Subscription subscription

    def stream = publish(1..10)
    stream = wiretap(stream) {
      if (it.data) {
        sent << it.item
      }
    }
    stream = throttle(stream)

    stream.subscribe(new Subscriber<Integer>() {
      @Override
      void onSubscribe(Subscription s) {
        subscription = s
      }

      @Override
      void onNext(Integer integer) {
        received << integer
      }

      @Override
      void onError(Throwable t) {
        received << t
      }

      @Override
      void onComplete() {
        complete = true
      }
    })

    expect:
    sent.size() == 0
    received.isEmpty()
    subscription.request(2)
    sent.size() == 10
    received.size() == 2
    subscription.request(2)
    received.size() == 4
    subscription.request(10)
    received.size() == 10
    complete
  }

  def "can firehose with buffering publisher"() {
    when:
    def p = publish(1..100)
    def s = CollectingSubscriber.subscribe(p)
    s.subscription.request(Long.MAX_VALUE)

    then:
    s.received == (1..100).toList()
  }


  def "can periodically produce"() {
    given:
    Runnable runnable = null
    def future = Mock(ScheduledFuture)
    def executor = Mock(ScheduledExecutorService) {
      scheduleWithFixedDelay(_, 0, 5, TimeUnit.SECONDS) >> {
        runnable = it[0]
        future
      }
    }

    when:
    def queue = new LinkedBlockingDeque()
    boolean complete
    Subscription subscription

    def stream = periodically(executor, 5, TimeUnit.SECONDS) { it < 5 ? it : null }

    stream.subscribe(new Subscriber<Integer>() {
      @Override
      void onSubscribe(Subscription s) {
        subscription = s
      }

      @Override
      void onNext(Integer integer) {
        queue.put(integer)
      }

      @Override
      void onError(Throwable t) {
        queue.put(t)
      }

      @Override
      void onComplete() {
        complete = true
      }
    })

    then:
    !future.isCancelled()
    runnable == null
    subscription.request(1)
    runnable.run()
    runnable.run()
    runnable.run()
    queue.toList() == [0]
    subscription.request(1)
    queue.toList() == [0, 1]
    subscription.request(2)
    queue.toList() == [0, 1, 2]
    runnable.run()
    runnable.run()
    runnable.run()
    queue.toList() == [0, 1, 2, 3]
    subscription.request(10)
    queue.toList() == [0, 1, 2, 3, 4]
    complete

    then:
    1 * future.cancel(_)
  }

  def "yielding publisher"() {
    when:
    def p = yield { it.requestNum < 4 ? it.requestNum.toString() + "-" + it.subscriberNum.toString() : null }
    def s1 = CollectingSubscriber.subscribe(p)
    def s2 = CollectingSubscriber.subscribe(p)

    then:
    s1.received.isEmpty()
    s2.received.isEmpty()

    when:
    s1.subscription.request(1)
    s2.subscription.request(1)

    then:
    s1.received == ["0-0"]
    s2.received == ["0-1"]

    when:
    s1.subscription.request(3)
    s2.subscription.request(3)

    then:
    s1.received == ["0-0", "1-0", "2-0", "3-0"]
    s2.received == ["0-1", "1-1", "2-1", "3-1"]
  }

}
