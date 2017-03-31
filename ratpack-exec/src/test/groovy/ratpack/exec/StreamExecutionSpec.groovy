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

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.func.Action
import ratpack.func.Function
import ratpack.http.ResponseChunks
import ratpack.stream.Streams
import ratpack.stream.internal.BufferingPublisher
import ratpack.stream.internal.CollectingSubscriber
import ratpack.test.exec.ExecHarness
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.AutoCleanup
import spock.lang.Ignore

import java.time.Duration
import java.util.concurrent.TimeUnit

import static ratpack.stream.Streams.periodically

class StreamExecutionSpec extends RatpackGroovyDslSpec {

  @AutoCleanup
  def harness = ExecHarness.harness()

  def "stream can use promises"() {
    when:
    serverConfig { development(true) }
    handlers {
      get { ctx ->
        def s = Streams.bindExec(periodically(ctx, Duration.ofMillis(100)) { it < 10 ? it : null })
          .flatMap { n ->
          Promise.async { f ->
            ctx.get(ExecController).executor.schedule({ f.success(n) } as Runnable, 10, TimeUnit.MILLISECONDS)
          }
        }
        .map {
          it.toString()
        }

        render(ResponseChunks.stringChunks(s))
      }
    }

    then:
    text == "0123456789"
  }

  def "stream can consume stream during event promises"() {
    when:
    serverConfig { development(true) }
    handlers {
      get { ctx ->
        def s = Streams.bindExec(periodically(ctx, Duration.ofMillis(100)) { it < 10 ? it : null })
          .flatMap { n ->
          Promise.async { f ->
            def c = new CollectingSubscriber({
              f.success(it.value.get(0))
            }, { it.request(10) })

            Streams.bindExec(periodically(ctx, Duration.ofMillis(100)) {
              it < 1 ? n : null
            }).subscribe(c)
          }
        }.map { it.toString() }

        render(ResponseChunks.stringChunks(s))
      }
    }

    then:
    text == "0123456789"
  }

  def "requests for more items can be issued off execution"() {
    when:
    def p = Streams.flatYield { r -> r.requestNum < 3 ? Promise.value(1) : Promise.value(null) }.bindExec()
    def e = []

    harness.yield {
      Promise.async { down ->
        p.subscribe(new Subscriber() {
          Subscription subscription

          private void request() {
            Thread.start {
              sleep 100
              subscription.request(1)
            }
          }

          @Override
          void onSubscribe(Subscription s) {
            subscription = s
            request()
          }

          @Override
          void onNext(Object o) {
            e << o
            request()
          }

          @Override
          void onError(Throwable t) {
            down.error(t)
          }

          @Override
          void onComplete() {
            down.success(e)
          }
        })
      }
    }

    then:
    e == [1, 1, 1]
  }

  def "cancel can be issued off thread"() {
    when:
    def p = Streams.flatYield { r -> r.requestNum < 3 ? Promise.value(1) : Promise.value(null) }.bindExec()
    def e = []

    harness.yield {
      Promise.async { down ->
        p.subscribe(new Subscriber() {
          Subscription subscription

          private void request() {
            Thread.start {
              sleep 100
              subscription.request(1)
            }
          }

          private void cancel() {
            Thread.start {
              sleep 100
              subscription.cancel()
              down.complete()
            }
          }

          @Override
          void onSubscribe(Subscription s) {
            subscription = s
            request()
          }

          @Override
          void onNext(Object o) {
            e << o
            cancel()
          }

          @Override
          void onError(Throwable t) {
            down.error(t)
          }

          @Override
          void onComplete() {
            down.success(e)
          }
        })
      }
    }

    then:
    e == [1]
  }

  def "exec bound publisher will stop publishing on sync cancel"() {
    when:
    def p = Streams.constant(1).bindExec()

    then:
    def v = harness.yield {
      Promise.async { down ->
        p.subscribe(new Subscriber<Integer>() {
          Subscription subscription

          @Override
          void onSubscribe(Subscription s) {
            subscription = s
            s.request(Long.MAX_VALUE)
          }

          def i = 0

          @Override
          void onNext(Integer integer) {
            if (++i == 100) {
              subscription.cancel()
              down.success(true)
            }
          }

          @Override
          void onError(Throwable t) {
            down.error(t)
          }

          @Override
          void onComplete() {
            down.complete()
          }
        })
      }
    }.valueOrThrow

    then:
    v == true
  }

  def "exec bound publisher will stop publishing on async cancel"() {
    when:
    def p = Streams.constant(1).bindExec()

    then:
    def v = harness.yield {
      Promise.async { down ->
        p.subscribe(new Subscriber<Integer>() {
          Subscription subscription

          @Override
          void onSubscribe(Subscription s) {
            subscription = s
            s.request(Long.MAX_VALUE)
          }

          def i = 0

          @Override
          void onNext(Integer integer) {
            if (++i == 100) {
              Thread.start {
                subscription.cancel()
                down.success(true)
              }
            }
          }

          @Override
          void onError(Throwable t) {
            down.error(t)
          }

          @Override
          void onComplete() {
            down.complete()
          }
        })
      }
    }.valueOrThrow

    then:
    v == true
  }

  def "items are serialized"() {
    when:
    def p = new BufferingPublisher(Action.noop(), { write ->
      Thread.start {
        100.times {
          sleep(3)
          write.item(it)
        }
        write.complete()
      }

      new Subscription() {
        @Override
        void request(long n) {

        }

        @Override
        void cancel() {

        }
      }
    } as Function)

    def max = 0
    def i = 0
    def l = harness.yield {
      p.bindExec().flatMap {
        max = Math.max(max, ++i)
        Promise.value(it).wiretap {
          --i
        }.defer(Duration.ofMillis(5))
      }.toList()
    }.valueOrThrow

    then:
    l == (0..99).toList()
    max == 1
  }

  @Ignore("LD")
  def "items are serialized even when dispatched on event loop"() {
    when:
    def max = 0
    def i = 0
    def l = harness.yield {
      Streams.publish(1..100).bindExec().flatMap {
        max = Math.max(max, ++i)
        Promise.value(it).wiretap {
          --i
        }
      }.toList()
    }.valueOrThrow

    then:
    l == (1..100).toList()
    max == 1
  }
}
