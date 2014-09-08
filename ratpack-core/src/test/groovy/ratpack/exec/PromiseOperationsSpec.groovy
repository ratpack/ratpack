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

import java.util.concurrent.CountDownLatch

import static ratpack.func.Actions.throwException

class PromiseOperationsSpec extends Specification {

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

  def "can map promise"() {
    when:
    exec {
      it.blocking { "foo" }
        .map { it + "-bar" }
        .map { it.toUpperCase() }
        .then { events << it }
    }

    then:
    events == ["FOO-BAR", "complete"]
  }

  def "can flat map promise"() {
    when:
    exec({ e ->
      e.blocking { "foo" }
        .flatMap { s -> e.blocking { s + "-bar" } }
        .map { it.toUpperCase() }
        .then { events << it }
    })

    then:
    events == ["FOO-BAR", "complete"]
  }

  def "errors are propagated down map chain"() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec { e ->
      e.promise { it.error(ex) }
        .map {}
        .map {}
        .onError { events << it }
        .then { throw new IllegalStateException("cant get here") }
    }

    then:
    events == [ex, "complete"]
  }

  def "errors are propagated down flatmap chain"() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec { e ->
      e.promise { it.error(ex) }
        .map {}
        .flatMap { e.blocking { "foo" } }
        .onError { events << it }
        .then { throw new IllegalStateException("cant get here") }
    }

    then:
    events == [ex, "complete"]
  }

  def "errors handler can terminate exception"() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec { e ->
      e.promise { it.error(ex) }
        .map {}
        .onError { events << it }
        .map {}
        .onError { events << "shouldn't get this" }
        .then { throw new IllegalStateException("cant get here") }
    }

    then:
    events == [ex, "complete"]
  }

  def "errors handler after exception receive it "() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec { e ->
      e.blocking { "foo" }
        .map { it }
        .onError { events << "shouldn't get this" }
        .map { throw ex }
        .onError { events << ex }
        .then { throw new IllegalStateException("cant get here") }
    }

    then:
    events == [ex, "complete"]
  }

  def "errors handler can transform exception"() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec { e ->
      e.promise { it.error(ex) }
        .map {}
        .onError { throw new RuntimeException(it.message + "-changed") }
        .map {}
        .onError { events << it.message }
        .then { throw new IllegalStateException("cant get here") }
    }

    then:
    events == ["!-changed", "complete"]
  }

  def "can route mapped promised value"() {
    when:
    exec {
      it.blocking { "foo" }
        .route({ it == "bar" }, { events << "received bar" })
        .map { it.toUpperCase() }
        .route({ it == "FOO" }, { events << it })
        .then(throwException(new RuntimeException("!")))
    }

    then:
    events == ["FOO", "complete"]
  }

  def "can exceptions thrown when routed propagate"() {
    when:
    def ex = new Exception("!")
    exec { e ->
      e.blocking { "foo" }
        .route({ it == "foo" }, { throw ex })
        .onError { events << it }
        .then(throwException(new RuntimeException("then-at-end")))
    }

    then:
    events == [ex, "complete"]
  }

  def "can terminate null"() {
    when:
    exec { e ->
      e.blocking { (String) null }
        .onNull { events << "null" }
        .map { it.toUpperCase() }
        .route({ it == "foo" }, { throw ex })
        .then(throwException(new RuntimeException("then-at-end")))
    }

    then:
    events == ["null", "complete"]
  }

  def "can perform blocking map"() {
    when:
    exec { e ->
      e.blocking { "foo" }
        .blockingMap { it + "-bar" }
        .map { it.toUpperCase() }
        .then { events << it }
    }

    then:
    events == ["FOO-BAR", "complete"]
  }

}
