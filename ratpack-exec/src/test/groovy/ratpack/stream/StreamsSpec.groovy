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
import ratpack.exec.Blocking
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.func.Function
import ratpack.stream.internal.BufferingPublisher
import ratpack.stream.internal.CollectingSubscriber
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingDeque

import static ratpack.stream.Streams.*
import static ratpack.test.exec.ExecHarness.yieldSingle

class StreamsSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()

  def "can firehose with buffering publisher"() {
    when:
    def p = (1..100).publish()
    def s = CollectingSubscriber.subscribe(p)
    s.subscription.request(Long.MAX_VALUE)

    then:
    s.received == (1..100).toList()
  }

  def "can periodically produce"() {
    expect:
    harness.yield {
      periodically(harness.controller.executor, Duration.ofMillis(500), { it < 5 ? it : null }).toList()
    }.value == [0, 1, 2, 3, 4]
  }

  def "can cancel periodic producer"() {
    when:
    def s = new CollectingSubscriber()
    def latch = new CountDownLatch(1)
    periodically(harness.controller.executor, Duration.ofMillis(1), {
      if (it == 500) {
        latch.countDown()
      }
      it
    }).subscribe(s)
    s.subscription.request(Long.MAX_VALUE)
    latch.await()
    s.subscription.cancel()
    sleep 100
    def size = s.received.size()
    sleep 100

    then:
    size == s.received.size()
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
      Blocking.get {
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
      Blocking.get {
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
    WriteStream write
    def cancelled
    def requests = []
    def publisher = new BufferingPublisher<Integer>(Action.noop(), {
      write = it
      new Subscription() {
        @Override
        void request(long n) {
          requests << n
        }

        @Override
        void cancel() {
          cancelled = true
        }
      }
    } as Function)

    def sent = []
    def fooReceived = []
    def barReceived = []
    boolean fooComplete
    boolean barComplete
    Subscription fooSubscription
    Subscription barSubscription

    def stream = publisher.wiretap {
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
    sent.size() == 0
    fooReceived.isEmpty()
    barReceived.isEmpty()

    when:
    fooSubscription.request(1)

    then:
    fooReceived.size() == 0

    when:
    write.item(1)
    write.item(1)
    write.item(1)

    then:
    sent.size() == 3
    fooReceived.size() == 1

    when:
    barSubscription.request(2)

    then:
    barReceived.size() == 0 //bar has missed the first 3 because they happened before it started requesting

    when:
    write.item(1)
    write.item(1)
    write.item(1)

    then:
    sent.size() == 6
    barReceived.size() == 2
    fooReceived.size() == 1

    when:
    write.item(1)
    write.item(1)
    write.item(1)
    write.item(1)
    write.complete()

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
    WriteStream write
    def publisher = new BufferingPublisher<Integer>(Action.noop(), {
      write = it
      new Subscription() {
        @Override
        void request(long n) {
        }

        @Override
        void cancel() {
        }
      }
    } as Function)

    def queue = new LinkedBlockingDeque()
    boolean complete
    Subscription subscription

    def stream = publisher
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
    write.item([])
    write.item([0, 1, 2, 3])

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
    write.item([0, 1, 2, 3])
    write.item([0, 1, 2, 3])
    write.complete()

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
    WriteStream write1
    def publisher1 = new BufferingPublisher<Integer>(Action.noop(), {
      write1 = it
      new Subscription() {
        @Override
        void request(long n) {
        }

        @Override
        void cancel() {
        }
      }
    } as Function)
    WriteStream write2
    def publisher2 = new BufferingPublisher<Integer>(Action.noop(), {
      write2 = it
      new Subscription() {
        @Override
        void request(long n) {
        }

        @Override
        void cancel() {
        }
      }
    } as Function)

    def stream = merge(publisher1, publisher2)
    def s1 = CollectingSubscriber.subscribe(stream)

    when:
    s1.subscription.request(1)

    then:
    s1.received.isEmpty()

    when:
    write1.item(1)

    then:
    s1.received == [1]

    when:
    s1.subscription.request(2)

    then:
    s1.received == [1]

    when:
    write2.item(11)
    write2.item(12)
    write1.item(2)
    write1.item(3)
    write1.complete()

    then:
    s1.received == [1, 11, 12]

    when:
    s1.subscription.request(10)

    then:
    s1.received == [1, 11, 12, 2, 3]
    !s1.complete

    when:
    write2.item(13)
    write2.complete()

    then:
    s1.received == [1, 11, 12, 2, 3, 13]
    s1.complete
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
      Streams.publish(["a", "b", "c"]).flatMap { Promise.value(it) }.toList()
    }

    then:
    result.valueOrThrow == ["a", "b", "c"]
  }

  def "flatmap error"() {
    when:
    def result = harness.yield { c ->
      Streams.publish(["a", "b", "c"]).flatMap { Promise.error(new IllegalStateException("!")) }.toList()
    }

    then:
    result.throwable.message == "!"
  }

  def "can filter stream elements"() {
    given:
    def p = (1..20).publish().filter { v ->
      v % 2 == 0
    }

    when:
    def s = CollectingSubscriber.subscribe(p)
    s.subscription.request(Long.MAX_VALUE)

    then:
    s.received == [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
  }

  static class ExceptionThrowingIterable implements Iterable<String> {

    @Override
    Iterator<String> iterator() {
      throw new IllegalStateException("!")
    }
  }

  def "can stream a promised iterable"() {
    expect:
    harness.yield {
      Promise.value(["a", "b", "c", "d"]).publish().map { it.toUpperCase() }.toList()
    }.value == ["A", "B", "C", "D"]

    and:
    harness.yield { c ->
      Promise.value(new ExceptionThrowingIterable()).publish().toList()
    }.throwable.message == "!"

    and:
    harness.yield { c ->
      Promise.error(new RuntimeException("!")).publish().toList()
    }.throwable.message == "!"
  }

}
