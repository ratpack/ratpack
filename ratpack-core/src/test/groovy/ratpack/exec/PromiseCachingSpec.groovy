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

import ratpack.func.Action
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch

class PromiseCachingSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()
  Queue<?> events = new ConcurrentLinkedQueue<>()
  def latch = new CountDownLatch(1)

  def exec(Action<? super ExecControl> action, Action<? super Throwable> onError = Action.noop()) {
    execHarness.exec()
      .onError(onError)
      .onComplete({
      events << "complete"
      latch.countDown()
    })
      .start { action.execute(it.control) }
    latch.await()
  }

  ExecControl getControl() {
    execHarness
  }

  def "can cache promise"() {
    when:
    exec { e ->
      def i = 0
      def cached = e.blocking { "foo-${i++}" }.cache()
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
      def cached = e.blocking { "foo" }.cache()
      cached.then { events << it }
      e.exec().onComplete {
        innerEvents << "complete"
        innerLatch.countDown()
      } start { forkedExecution ->
        cached.map { it + "-bar" }
          .then {
          assert e.execution.is(forkedExecution)

          innerEvents << it
        }

        e.blocking { "next" }.cache().then {
          assert e.execution.is(forkedExecution)
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
      def cached = it.promise { it.error(e) }.cache()
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
      def cached = it.promise { it.error(e) }.cache()
      cached.map { throw new Error() }.onError { events << it }.then { throw new Error() }
    }

    then:
    events.toList() == [e, "complete"]
  }

  def "promise can be massively reused"() {
    when:
    def num = 10000
    def latch = new CountDownLatch(num)
      def cached = execHarness.promiseOf("foo").cache()
    exec { e ->
      num.times {
        e.exec().start({ cached.then {
          events << it
          latch.countDown() } })
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
      it.blocking {
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

}
