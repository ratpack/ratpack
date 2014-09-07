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

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.func.Action
import ratpack.func.Actions
import ratpack.launch.LaunchConfigBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

class ExecutionSpec extends Specification {

  @AutoCleanup
  ExecController controller
  List<String> events = []
  def latch = new CountDownLatch(1)

  def setup() {
    controller = LaunchConfigBuilder.noBaseDir().build().execController
  }

  def exec(Action<? super ExecControl> action, Action<? super Throwable> onError = Actions.noop()) {
    controller.control.fork({
      action.execute(it.control)
    }, onError, {
      events << "complete"
      latch.countDown()
    })
    latch.await()
  }

  ExecControl getControl() {
    controller.control
  }

  def "exception thrown after promise prevents promise from running"() {
    when:
    exec({ e ->
      e.promise { f ->
        events << "action"
        e.fork {
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
      control.blocking { 2 } then { events << "error" }
    })

    then:
    events == ["error", "complete"]
  }

  def "error handler can perform blocking ops then blocking opts"() {
    when:
    exec({
      throw new RuntimeException("!")
    }, {
      control.blocking {
        2
      } then {
        control.blocking {
          2
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
    events == ["e1", "e2", "complete"]
  }

  def "execution can queue promises"() {
    def innerLatch = new CountDownLatch(1)

    when:
    exec { e1 ->
      e1.promise { f ->
        Thread.start {
          sleep 100
          f.success "1"
        }
      } then {
        events << it
      }
      e1.promise { f ->
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

    exec { e1 ->
      def p = e1.promise { f ->
        e1.fork {
          f.success(2)
        }
      }

      e1.fork { e2 ->
        p.then {
          assert e2.controller.control.execution == e2
          events << "then"
          innerLatch.countDown()
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
      e1.stream(new Publisher<String>() {
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
              assert e1.controller.managedThread
              events << 'publisher-request'
              if (capacity.getAndAdd(elements) == 0) {
                // start sending again if it wasn't already running
                send()
              }
            }

            private void send() {
              Thread.start {
                while (capacity.getAndDecrement() > 0) {
                  assert !e1.controller.managedThread
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
      }, new Subscriber<String>() {
        @Override
        void onSubscribe(Subscription subscription) {
          assert e1.controller.managedThread
          events << 'subscriber-onSubscribe'
          subscription.request(2)
        }

        @Override
        void onNext(String element) {
          assert e1.controller.managedThread
          events << "subscriber-onNext:$element".toString()
        }

        @Override
        void onComplete() {
          assert e1.controller.managedThread
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


}
