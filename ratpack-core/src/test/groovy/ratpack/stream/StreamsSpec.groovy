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
import ratpack.func.Function
import ratpack.stream.internal.CollectingSubscriber
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

import static ratpack.stream.Streams.*
import static ratpack.test.exec.ExecHarness.yieldSingle

class StreamsSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()

  def "can buffer publisher"() {
    given:
    def sent = []
    def received = []
    boolean complete
    Subscription subscription

    def stream = (1..10).publish().wiretap {
      if (it.data) {
        sent << it.item
      }
    }.buffer()

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
    def p = (1..100).publish()
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
      scheduleWithFixedDelay(_, 0, Duration.ofSeconds(5).toNanos(), TimeUnit.NANOSECONDS) >> {
        runnable = it[0]
        future
      }
    }

    when:
    def queue = new LinkedBlockingDeque()
    boolean complete
    Subscription subscription

    def stream = executor.periodically(Duration.ofSeconds(5)) { it < 5 ? it : null }

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
    def p = ({ it.requestNum < 4 ? it.requestNum.toString() + "-" + it.subscriberNum.toString() : null } as Function).yield()
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

  def "flat yielding publisher"() {
    when:
    def p = ({ r ->
      harness.blocking {
        r.requestNum < 4 ? r.requestNum.toString() + "-" + r.subscriberNum.toString() : null
      }
    } as Function).flatYield()
    def s1 = CollectingSubscriber.subscribe(p)
    def s2 = CollectingSubscriber.subscribe(p)

    then:
    s1.received.isEmpty()
    s2.received.isEmpty()

    when:
    harness.run { s1.subscription.request(1) }
    harness.run { s2.subscription.request(1) }

    then:
    s1.received == ["0-0"]
    s2.received == ["0-1"]

    when:
    harness.run { s1.subscription.request(3) }
    harness.run { s2.subscription.request(3) }

    then:
    s1.received == ["0-0", "1-0", "2-0", "3-0"]
    s2.received == ["0-1", "1-1", "2-1", "3-1"]
  }

  def "failed promise stops flat yield"() {
    given:
    def p = ({ r ->
      harness.blocking {
        r.requestNum < 4 ? r.requestNum.toString() + "-" + r.subscriberNum.toString() : { throw new Exception("!") }.run()
      }
    } as Function).flatYield()
    def s1 = CollectingSubscriber.subscribe(p)

    when:
    harness.run { s1.subscription.request(Long.MAX_VALUE) }

    then:
    s1.received == ["0-0", "1-0", "2-0", "3-0"]
    s1.error.message == "!"
  }

  def "can multicast with back pressure"() {
    given:
    Runnable runnable = null
    def future = Mock(ScheduledFuture)
    def executor = Mock(ScheduledExecutorService) {
      scheduleWithFixedDelay(_, 0, Duration.ofSeconds(5).toNanos(), TimeUnit.NANOSECONDS) >> {
        runnable = it[0]
        future
      }
    }

    def sent = []
    def fooReceived = []
    def barReceived = []
    boolean fooComplete
    boolean barComplete
    Subscription fooSubscription
    Subscription barSubscription

    def stream = executor.periodically(Duration.ofSeconds(5)) { it < 10 ? it : null }.wiretap {
      if (it.data) {
        sent << it.item
      }
    }.multicast()

    when:
    stream.subscribe(new Subscriber<Integer>() {
      @Override
      void onSubscribe(Subscription s) {
        fooSubscription = s
      }

      @Override
      void onNext(Integer integer) {
        fooReceived << integer
      }

      @Override
      void onError(Throwable t) {
        fooReceived << t
      }

      @Override
      void onComplete() {
        fooComplete = true
      }
    })

    stream.subscribe(new Subscriber<Integer>() {
      @Override
      void onSubscribe(Subscription s) {
        barSubscription = s
      }

      @Override
      void onNext(Integer integer) {
        barReceived << integer
      }

      @Override
      void onError(Throwable t) {
        barReceived << t
      }

      @Override
      void onComplete() {
        barComplete = true
      }
    })

    then:
    !future.isCancelled()
    runnable == null
    sent.size() == 0
    fooReceived.isEmpty()
    barReceived.isEmpty()

    when:
    fooSubscription.request(1)

    then:
    fooReceived.size() == 0

    when:
    runnable.run()
    runnable.run()
    runnable.run()

    then:
    sent.size() == 3
    fooReceived.size() == 1

    when:
    barSubscription.request(2)

    then:
    barReceived.size() == 0 //bar has missed the first 3 because they happened before it started requesting

    when:
    runnable.run()
    runnable.run()
    runnable.run()

    then:
    sent.size() == 6
    barReceived.size() == 2
    fooReceived.size() == 1

    when:
    runnable.run()
    runnable.run()
    runnable.run()
    runnable.run()
    runnable.run()

    then:
    sent.size() == 10
    barReceived.size() == 2
    fooReceived.size() == 1

    when:
    fooSubscription.request(10)

    then:
    fooReceived.size() == 10
    fooComplete
    barReceived.size() == 2

    when:
    barSubscription.request(10)

    then:
    barReceived.size() == 7
    barComplete
  }

  def "can reject further multicast subscriptions when the upstream publisher has completed"() {
    given:
    def error
    def stream = [1].publish().multicast()
    stream.subscribe(new Subscriber() {
      @Override
      void onSubscribe(Subscription s) {
        s.request(1)
      }

      @Override
      void onNext(Object o) {}

      @Override
      void onError(Throwable t) {}

      @Override
      void onComplete() {}
    })

    when:
    stream.subscribe(new Subscriber() {
      @Override
      void onSubscribe(Subscription s) {}

      @Override
      void onNext(Object o) {}

      @Override
      void onError(Throwable t) {
        error = t
      }

      @Override
      void onComplete() {}
    })

    then:
    error instanceof IllegalStateException
    error.message == 'The upstream publisher has completed, either successfully or with error.  No further subscriptions will be accepted'
  }

  def "can fan out with back pressure"() {
    given:
    Runnable runnable = null
    def future = Mock(ScheduledFuture)
    def executor = Mock(ScheduledExecutorService) {
      scheduleWithFixedDelay(_, 0, Duration.ofSeconds(5).toNanos(), TimeUnit.NANOSECONDS) >> {
        runnable = it[0]
        future
      }
    }

    def queue = new LinkedBlockingDeque()
    boolean complete
    Subscription subscription

    def stream = executor.periodically(Duration.ofSeconds(5)) { it < 3 ? [0, 1, 2, 3] : null }
    stream = fanOut(stream)

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

    when:
    subscription.request(1)

    then:
    queue.toList() == []

    when:
    runnable.run()

    then:
    queue.toList() == [0]

    when:
    subscription.request(1)

    then:
    queue.toList() == [0, 1]

    when:
    subscription.request(2)

    then:
    queue.toList() == [0, 1, 2, 3]

    when:
    runnable.run()
    runnable.run()
    runnable.run()

    then:
    queue.toList() == [0, 1, 2, 3]

    when:
    subscription.request(3)

    then:
    queue.toList() == [0, 1, 2, 3, 0, 1, 2]

    when:
    subscription.request(10)

    then:
    queue.toList() == [0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3]
    complete
  }

  def "can merge publishers into a single stream"() {
    given:
    Runnable runnable1 = null
    Runnable runnable2 = null

    def future = Mock(ScheduledFuture)
    def executor1 = Mock(ScheduledExecutorService) {
      scheduleWithFixedDelay(_, 0, Duration.ofSeconds(5).toNanos(), TimeUnit.NANOSECONDS) >> {
        runnable1 = it[0]
        future
      }
    }
    def executor2 = Mock(ScheduledExecutorService) {
      scheduleWithFixedDelay(_, 0, Duration.ofSeconds(5).toNanos(), TimeUnit.NANOSECONDS) >> {
        runnable2 = it[0]
        future
      }
    }

    def queue = new LinkedBlockingDeque()
    boolean complete = false
    Subscription subscription

    def stream1 = executor1.periodically(Duration.ofSeconds(5)) { it < 3 ? it + 1 : null }
    def stream2 = executor2.periodically(Duration.ofSeconds(5)) { it < 3 ? it + 11 : null }
    def stream = merge(stream1, stream2)

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

    when:
    subscription.request(1)

    then:
    queue.toList() == []

    when:
    runnable1.run()

    then:
    queue.toList() == [1]

    when:
    subscription.request(2)

    then:
    queue.toList() == [1]

    when:
    runnable2.run()
    runnable2.run()
    runnable1.run()
    runnable1.run()
    runnable1.run()

    then:
    queue.toList() == [1, 11, 12]

    when:
    subscription.request(10)

    then:
    queue.toList() == [1, 11, 12, 2, 3]
    complete == false

    when:
    runnable2.run()
    runnable2.run()

    then:
    queue.toList() == [1, 11, 12, 2, 3, 13]
    complete == true
  }

  def "can convert stream to promise"() {
    expect:
    yieldSingle { publish([1]).toPromise() }.value == 1
    yieldSingle { publish(1..10).toPromise() }.throwable instanceof IllegalStateException
    yieldSingle { publish([]).toPromise() }.value == null
  }

  def "flatmap"() {
    when:
    def result = harness.yield { c ->
      Streams.publish(["a", "b", "c"]).flatMap { c.promiseOf(it) }.toList()
    }

    then:
    result.valueOrThrow == ["a", "b", "c"]
  }
}
