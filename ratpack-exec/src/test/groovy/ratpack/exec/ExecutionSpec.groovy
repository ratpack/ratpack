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

package ratpack.exec

import com.google.common.util.concurrent.Futures
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.func.Action
import ratpack.stream.Streams
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ExecutionSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()
  List<Object> events = []
  def latch = new CountDownLatch(1)


  def exec(Action<? super Execution> action) {
    exec(action, Action.noop())
  }

  def exec(Action<? super Execution> action, Action<? super Throwable> onError) {
    harness.controller.fork().onError(onError).onComplete {
      events << "complete"
      latch.countDown()
    } start {
      action.execute(it)
    }
    latch.await()
  }

  def "exception thrown after promise prevents promise from running"() {
    when:
    exec({ e ->
      Promise.async { f ->
        events << "action"
        e.fork().start {
          f.success(1)
        }
      } then {
        events << "then"
      }

      throw new RuntimeException("!")
    }, {
      events << "error"
    })

    then:
    events == ["error", "complete"]
  }

  def "error handler can perform blocking ops"() {
    when:
    exec({ e ->
      throw new RuntimeException("!")
    }, {
      try {
        Blocking.get { 2 } then { events << "error" }

      } catch (e) {
        e.printStackTrace()
      }
    })

    then:
    events == ["error", "complete"]
  }

  def "error handler can perform blocking ops then blocking opts"() {
    when:
    exec({
      throw new RuntimeException("!")
    }, {
      Blocking.get {
        2
      } then {
        Blocking.get {
          5
        } then {
          events << "error"
        }
      }
    })

    then:
    events == ["error", "complete"]
  }

  def "error handler can throw error"() {
    when:
    exec({ e ->
      throw new RuntimeException("1")
    }, {
      events << "e$it.message".toString()
      if (!("e2" in events)) {
        throw new RuntimeException("2")
      }
    })

    then:
    events == ["e1", "complete"]
  }

  def "execution can queue promises"() {
    def innerLatch = new CountDownLatch(1)

    when:
    exec { e1 ->
      Promise.async { f ->
        Thread.start {
          sleep 100
          f.success "1"
        }
      } then {
        events << it
      }
      Promise.async { f ->
        Thread.start {
          f.success "2"
        }
      } then {
        events << it
        innerLatch.countDown()
      }
    }

    then:
    innerLatch.await()
    events == ["1", "2", "complete"]
  }

  def "promise is bound to subscribing execution"() {
    when:
    def innerLatch = new CountDownLatch(1)

    exec { outerExec ->
      def p = Promise.value { f ->
        Execution.fork().start {
          f.success(2)
        }
      }

      outerExec.onComplete {
        Execution.fork().start { e2 ->
          p.then {
            assert Execution.current() == e2
            events << "then"
            innerLatch.countDown()
          }
        }
      }
    }

    then:
    innerLatch.await()
    events == ["complete", "then"]
  }

  def "subscriber callbacks are bound to execution"() {
    when:
    exec { e1 ->
      Streams.bindExec(new Publisher<String>() {
        @Override
        void subscribe(Subscriber subscriber) {
          events << 'publisher-subscribe'
          final AtomicLong i = new AtomicLong()

          Subscription subscription = new Subscription() {
            AtomicLong capacity = new AtomicLong()

            @Override
            void cancel() {
              capacity.set(-1)
              subscriber.onComplete()
            }

            @Override
            void request(long elements) {
              assert Execution.isManagedThread()
              events << 'publisher-request'
              if (capacity.getAndAdd(elements) == 0) {
                // start sending again if it wasn't already running
                send()
              }
            }

            private void send() {
              Thread.start {
                while (capacity.getAndDecrement() > 0) {
                  assert !Execution.isManagedThread()
                  events << 'publisher-send'
                  subscriber.onNext('foo' + i.incrementAndGet())
                  Thread.sleep(50)
                }

                cancel()
              }
            }
          }

          subscriber.onSubscribe(subscription)
        }
      }).subscribe(new Subscriber<String>() {
        @Override
        void onSubscribe(Subscription subscription) {
          assert Execution.isManagedThread()
          events << 'subscriber-onSubscribe'
          subscription.request(2)
        }

        @Override
        void onNext(String element) {
          assert Execution.isManagedThread()
          events << "subscriber-onNext:$element".toString()
        }

        @Override
        void onComplete() {
          assert Execution.isManagedThread()
          events << 'subscriber-onComplete'
        }

        @Override
        void onError(Throwable cause) {

        }
      })
    }

    then:
    events == [
      'publisher-subscribe',
      'subscriber-onSubscribe',
      'publisher-request',
      'publisher-send',
      'subscriber-onNext:foo1',
      'publisher-send',
      'subscriber-onNext:foo2',
      'subscriber-onComplete',
      "complete"
    ]
  }

  def "can subscribe to promise more than once"() {
    when:
    exec({
      def p = Blocking.get { 2 }
      code(p).then { events << it }
      code(p).then { events << it }
    }) {
      events << it
    }

    then:
    events.size() == 3

    where:
    code << [
      { it.flatMap { Blocking.get { 2 } } },
      { it },
      { it.map { 2 } },
      { it.flatMap { Blocking.get { 2 } } },
      { it.onNull { 2 } },
      { it.route({ it == 4 }) { throw new UnsupportedOperationException() } },
    ]
  }

  def "can subscribe to success promise more than once"() {
    when:
    exec({
      def p = Blocking.get { 2 }.onError { throw new UnsupportedOperationException() }
      code(p).then { events << it }
      code(p).then { events << it }
    }) {
      events << it
    }

    then:
    events.size() == 3

    where:
    code << [
      { it.flatMap { Blocking.get { 2 } } },
      { it },
      { it.map { 2 } },
      { it.flatMap { Blocking.get { 2 } } },
      { it.onNull { 2 } },
      { it.route({ it == 4 }) { throw new UnsupportedOperationException() } },
    ]
  }

  def "can complete future"() {
    when:
    exec({ e ->
      Promise.async {
        it.accept(CompletableFuture.supplyAsync({ "foo" }, e.controller.executor))
      } then {
        events << it
      }
    })

    then:
    events == ["foo", "complete"]
  }

  def "can complete ListenableFuture"() {
    when:
    exec({ c ->
      Promise.async {
        it.accept(Futures.immediateFuture("foo"))
      } then {
        events << it
      }
    })

    then:
    events == ["foo", "complete"]
  }

  def "can error from ListenableFuture"() {
    when:
    exec({ c ->
      Promise.<String> async { f ->
        f.accept(Futures.immediateFailedFuture(new RuntimeException("error")))
      } onError {
        events << "error"
      } then {
        events << it
      }
    })

    then:
    events == ["error", "complete"]
  }


  def "can nest promises"() {
    when:
    exec({ e ->
      Promise.async { f1 ->
        Promise.async { f -> f.success("foo") }.result { r -> f1.accept(r) }
      } then {
        events << it
      }
    }, {
      events << it.class
    })

    then:
    events == ["foo", "complete"]
  }

  def "can have multiple on error"() {
    when:
    exec {
      Promise.error(new Exception("!")).onError { events << "1" }.onError { events << "2" }.then { events << "3" }
    }

    then:
    events == ["1", "complete"]
  }

  def "can safely resume from different thread"() {
    when:
    def n = 10_000
    def l = new CountDownLatch(n)
    def i = new AtomicInteger()

    n.times {
      harness.controller.fork().start {
        def d = new AtomicBoolean()
        Execution.current().onComplete {
          if (!d.get()) {
            i.incrementAndGet()
          }
          l.countDown()
        }
        Promise.async { down ->
          harness.controller.eventLoopGroup.execute {
            down.success(1)
          }
        }.then {
          d.set(true)
        }
      }
    }

    then:
    l.await()
    i.get() == 0
  }
}
