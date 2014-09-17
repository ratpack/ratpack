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
import ratpack.func.Actions
import ratpack.launch.LaunchConfigBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch

import static ratpack.func.Actions.throwException

class PromiseCachingSpec extends Specification {

  @AutoCleanup
  ExecController controller
  Queue<?> events = new ConcurrentLinkedQueue<>()
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

  def "can cache promise"() {
    when:
    exec { e ->
      def cached = e.blocking { "foo" }.cache()
      cached.then { events << it }
      cached.map { it + "-bar" }.then { events << it }
    }

    then:
    events.toList() == ["foo", "foo-bar", "complete"]
  }

  def "can use cached promise in different execution"() {
    when:
    def innerLatch = new CountDownLatch(1)
    def innerEvents = []
    exec { e ->
      def cached = e.blocking { "foo" }.cache()
      cached.then { events << it }
      e.fork({ forkedExecution ->
        cached.map { it + "-bar" }
          .then {
          assert e.execution.is(forkedExecution)

          innerEvents << it
        }

        e.blocking { "next" }.cache().then {
          assert e.execution.is(forkedExecution)
          innerEvents << "next"
        }
      }, throwException(), {
        innerEvents << "complete"
        innerLatch.countDown()
      })
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
    def num = 100000
    def latch = new CountDownLatch(num)
    exec { e ->
      def cached = e.blocking { "foo" }.cache()
      num.times {
        e.fork({ cached.then { events << it } }, throwException(), {
          latch.countDown()
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
    events.toList() == [e, "custom", "complete"]
  }

}
