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

import ratpack.exec.util.ParallelBatch
import ratpack.exec.util.SerialBatch
import ratpack.func.Action
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class PromiseCachingSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()
  Queue<Object> events = new ConcurrentLinkedQueue<>()
  def latch = new CountDownLatch(1)

  def exec(Action<? super Execution> action, Action<? super Throwable> onError = Action.noop()) {
    execHarness.controller.fork()
      .onError(onError)
      .onComplete({
      events << "complete"
      latch.countDown()
    })
      .start { action.execute(it) }
    latch.await()
  }

  def "can cache promise"() {
    when:
    exec { e ->
      def i = 0
      def cached = Blocking.get { "foo-${i++}" }.cache()
      cached.then { events << it }
      cached.map { it + "-bar" }.then { events << it }
    }

    then:
    events.toList() == ["foo-0", "foo-0-bar", "complete"]
  }

  def "can use cached promise in different execution"() {
    when:
    def innerLatch = new CountDownLatch(1)
    def innerEvents = []
    exec { e ->
      def cached = Blocking.get { "foo" }.cache()
      cached.then { events << it }
      Execution.fork().onComplete {
        innerEvents << "complete"
        innerLatch.countDown()
      } start { forkedExecution ->
        cached.map { it + "-bar" }
          .then {
          assert Execution.current().is(forkedExecution)

          innerEvents << it
        }

        Blocking.get { "next" }.cache().then {
          assert Execution.current().is(forkedExecution)
          innerEvents << "next"
        }
      }
    }

    then:
    innerLatch.await()
    events.toList() == ["foo", "complete"]
    innerEvents == ["foo-bar", "next", "complete"]
  }

  def "failed promise can be cached"() {
    when:
    def e = new RuntimeException("!")
    exec {
      def cached = Promise.error(e).cache()
      cached.onError { events << it }.then { throw new Error() }
      cached.onError { events << it }.then { throw new Error() }
    }

    then:
    events.toList() == [e, e, "complete"]
  }

  def "failed promise can be transformed"() {
    when:
    def e = new RuntimeException("!")
    exec {
      def cached = Promise.error(e).cache()
      cached.map { throw new Error() }.onError { events << it }.then { throw new Error() }
    }

    then:
    events.toList() == [e, "complete"]
  }

  def "promise can be massively reused"() {
    when:
    def num = 10000
    def latch = new CountDownLatch(num)
    def cached = Promise.value("foo").cache()
    exec { e ->
      num.times {
        Execution.fork().start({
          cached.then {
            events << it
            latch.countDown()
          }
        })
      }
    }

    then:
    latch.await()
    events.toList() - ["complete"] == (1..num).collect { "foo" }
  }

  def "cached error handler is used when none specified"() {
    when:
    def e = new RuntimeException("!")
    exec {
      Blocking.get {
        throw e
      }.onError {
        events << it
      }.cache().with {
        then {}
        onError { events << "custom" }.then {}
      }
    }

    then:
    events.toList() == [e, "complete"]
  }

  def "can cache conditionally"() {
    when:
    def i = new AtomicInteger()
    def p = Promise.sync { i.getAndIncrement() }.cacheIf { it >= 5 }

    exec({
      SerialBatch.of((0..10).collect { p }).forEach { a, b -> events << b }.then()
    })

    then:
    events.toList() == [0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5, "complete"]
  }

  def "can cache conditionally in parallel"() {
    when:
    def i = new AtomicInteger()
    def p = Promise.sync { i.getAndIncrement() }.cacheIf { it >= 5 }

    exec({
      ParallelBatch.of((0..10).collect { p }).forEach { a, b -> events << b }.then()
    })

    then:
    events.toList().subList(0, events.size() - 1).sort() == [0, 1, 2, 3, 4, 5, 5, 5, 5, 5, 5]
  }

  def "can cache errors"() {
    when:
    def i = new AtomicInteger()
    def p = Promise.sync { throw new Exception("${i.incrementAndGet()}") }.cacheResultIf { it.error }

    exec({
      ParallelBatch.of((0..10).collect { p }).yieldAll().then { events.addAll(it.throwable) }
    })

    then:
    events.toList().subList(0, events.size() - 1).message == (0..10).collect { "1" }
  }

  def "can cache for time"() {
    given:
    def i = new AtomicInteger()
    def p = Promise.sync(i.&getAndIncrement).cacheResultFor { Duration.ofSeconds(1) }

    when:
    exec {
      p.then {
        events.add(it)
        p.then {
          events.add(it)
          sleep 1000
          p.then {
            events.add(it)
            p.then {
              events.add(it)
            }
          }
        }
      }
    }

    then:
    events.poll() == 0
    events.poll() == 0
    events.poll() == 1
    events.poll() == 1
  }

}
