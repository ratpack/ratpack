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
import ratpack.launch.LaunchConfigBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class ExecutionSpec extends Specification {

  @AutoCleanup
  ExecController controller
  def events = []
  def latch = new CountDownLatch(1)

  def setup() {
    controller = LaunchConfigBuilder.noBaseDir().build().execController
  }

  def exec(Action<? super Execution> action) {
    controller.start {
      it.onComplete {
        latch.countDown()
      }

      action.execute(it)
    }
    latch.await()
  }

  def "exception thrown after promise prevents promise from running"() {
    when:
    exec { e ->
      e.setErrorHandler {
        events << "error"
      }

      e.promise { f ->
        events << "action"
        e.fork {
          f.success(1)
        }
      } then {
        events << "then"
      }

      throw new RuntimeException("!")
    }

    then:
    events == ["error"]
  }

  def "error handler can perform blocking ops"() {
    when:
    exec { e ->
      e.setErrorHandler {
        e.blocking { 2 } then { events << "error" }
      }

      throw new RuntimeException("!")
    }

    then:
    events == ["error"]
  }

  def "error handler can perform blocking ops then blocking opts"() {
    when:
    exec { e ->
      e.setErrorHandler {
        e.blocking {
          2
        } then {
          e.blocking {
            2
          } then {
            events << "error"
          }
        }
      }

      throw new RuntimeException("!")
    }

    then:
    events == ["error"]
  }

  def "error handler can throw error"() {
    when:
    exec { e ->
      e.setErrorHandler {
        events << "e$it.message".toString()
        if (!("e2" in events)) {
          throw new RuntimeException("2")
        }
      }

      throw new RuntimeException("1")
    }

    then:
    events == ["e1", "e2"]
  }

  def "execution can queue promises"() {
    def innerLatch = new CountDownLatch(1)

    when:
    exec { e1 ->
      e1.setErrorHandler {
        it.printStackTrace()
      }
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
    events == ["1", "2"]
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
          assert e2.controller.execution == e2
          events << "then"
          innerLatch.countDown()
        }
      }
    }

    then:
    innerLatch.await()
    events == ["then"]
  }

  def "subscriber callbacks are bound to execution"() {
    when:
    def streamEvents = []
    def innerLatch = new CountDownLatch(4)

    exec { e1 ->
      controller.execution.onComplete {
        streamEvents << 'execution-complete'
        innerLatch.countDown()
      }

      e1.stream(new Publisher<String>() {
        @Override
        void subscribe(Subscriber subscriber) {
          streamEvents << 'publisher-subscribe'
          final AtomicInteger i = new AtomicInteger()

          Subscription subscription = new Subscription() {
            AtomicInteger capacity = new AtomicInteger()

            @Override
            void cancel() {
              capacity.set(-1)
              subscriber.onComplete()
            }

            @Override
            void request(int elements) {
              assert e1.controller.managedThread
              streamEvents << 'publisher-request'
              if (capacity.getAndAdd(elements) == 0) {
                // start sending again if it wasn't already running
                send()
              }
            }

            private void send() {
              Thread.start {
                while (capacity.getAndDecrement() > 0) {
                  assert !e1.controller.managedThread
                  streamEvents << 'publisher-send'
                  subscriber.onNext('foo' + i.incrementAndGet())
                  Thread.sleep(500)
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
          streamEvents << 'subscriber-onSubscribe'
          subscription.request(2)
        }

        @Override
        void onNext(String element) {
          assert e1.controller.managedThread
          streamEvents << "subscriber-onNext:$element".toString()
          innerLatch.countDown()
        }

        @Override
        void onComplete() {
          assert e1.controller.managedThread
          streamEvents << 'subscriber-onComplete'
          innerLatch.countDown()
        }

        @Override
        void onError(Throwable cause) {

        }
      })
    }

    then:
    innerLatch.await()
    streamEvents.toString() == [
      'publisher-subscribe',
      'subscriber-onSubscribe',
      'publisher-request',
      'publisher-send',
      'publisher-send',
      'subscriber-onNext:foo1',
      'subscriber-onNext:foo2',
      'subscriber-onComplete',
      'execution-complete'
    ].toString()
  }

}
