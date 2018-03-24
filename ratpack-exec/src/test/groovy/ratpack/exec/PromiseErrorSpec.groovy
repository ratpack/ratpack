/*
 * Copyright 2015 the original author or authors.
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

import java.util.concurrent.CountDownLatch

class PromiseErrorSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()
  List events = []
  def latch = new CountDownLatch(1)


  def exec(Action<? super Execution> action, Action<? super Throwable> onError = Action.noop()) {
    execHarness.controller.fork()
      .onError(onError)
      .onComplete {
      events << "complete"
      latch.countDown()
    } start {
      action.execute(it)
    }

    latch.await()
  }

  def "on error can throw different error"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException("!"))
        .onError {
        throw new NullPointerException("!")
      }
      .then {
        events << "then"
      }
    } {
      events << it
    }

    then:
    events.size() == 2
    events[0] instanceof NullPointerException
    (events[0] as Exception).suppressed[0] instanceof IllegalArgumentException
  }

  def "on error can throw same error"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException("!"))
        .onError { throw it }
        .then {
        events << "then"
      }
    } {
      events << it
    }

    then:
    events.size() == 2
    events[0] instanceof IllegalArgumentException
    (events[0] as Exception).suppressed.length == 0
  }

  def "on error called when predicate passes"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException("!"))
        .onError { it.message == "!" } { events << "error" }
        .then {
        events << "then"
      }
    } {
      events << it
    }

    then:
    events == ["error", "complete"]
  }

  def "on error not called when predicate fails"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException("!"))
        .onError { it.message != "!" } { events << "error" }
        .then {
        events << "then"
      }
    } {
      events << it
    }

    then:
    events.size() == 2
    events[0] instanceof IllegalArgumentException
  }

  def "on error called for match on exception"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException("!"))
        .onError(IllegalArgumentException) { events << "error" }
        .then {
        events << "then"
      }
    } {
      events << it
    }

    then:
    events == ["error", "complete"]
  }

  def "on error not called when exception doesn't match"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException("!"))
        .onError(NullPointerException) { events << "error" }
        .then {
        events << "then"
      }
    } {
      events << it
    }

    then:
    events.size() == 2
    events[0] instanceof IllegalArgumentException
  }

  def "multiple on error handlers"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException("!"))
        .onError { t -> t.message == "foo" } { events << "error1" }
        .onError(IllegalArgumentException) { events << "error2" }
        .then {
        events << "then"
      }
    } {
      events << it
    }

    then:
    events == ["error2", "complete"]
  }

  def "can map error to value"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException()).mapError { 1 }.then(events.&add)
    }

    then:
    events == [1, "complete"]
  }

  def "can map error to different error"() {
    when:
    def e = new IllegalStateException()
    exec({
      Promise.error(new IllegalArgumentException()).mapError {
        throw e
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

  def "can map error of type"() {
    when:
    def e = new IllegalStateException()
    exec({
      Promise.error(new IllegalArgumentException()).mapError(IllegalArgumentException) {
        throw e
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

  def "can map error based on predicate"() {
    when:
    def e = new IllegalStateException()
    exec({
      Promise.error(new IllegalArgumentException("!")).mapError { it.message == "!" } {
        throw e
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

  def "error map does not apply when error is of different type"() {
    when:
    def e1 = new IllegalArgumentException()
    def e = new IllegalStateException()
    exec({
      Promise.error(e1).mapError(NullPointerException) {
        throw e
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e1, "complete"]
  }

  def "can flatmapmap error to value"() {
    when:
    exec {
      Promise.error(new IllegalArgumentException()).flatMapError { Promise.value(1) }.then(events.&add)
    }

    then:
    events == [1, "complete"]
  }

  def "can flatmap error to different error"() {
    when:
    def e = new IllegalStateException()
    exec({
      Promise.error(new IllegalArgumentException()).flatMapError {
        Promise.error(e)
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

  def "can flatmap error of type"() {
    when:
    def e = new IllegalStateException()
    exec({
      Promise.error(new IllegalArgumentException()).flatMapError(IllegalArgumentException) {
        Promise.error(e)
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

  def "can flatmap error based on predicate"() {
    when:
    def e = new IllegalStateException()
    exec({
      Promise.error(new IllegalArgumentException('!')).flatMapError { it.message == '!' } {
        Promise.error(e)
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

  def "error flatmap does not apply when error is of different type"() {
    when:
    def e1 = new IllegalArgumentException()
    def e = new IllegalStateException()
    exec({
      Promise.error(e1).flatMapError(NullPointerException) {
        Promise.error(e)
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e1, "complete"]
  }

  def "error thrown by error flatmap propagates"() {
    when:
    def e = new IllegalStateException()
    exec({
      Promise.error(new IllegalArgumentException()).flatMapError {
        throw e
      }.then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

}
