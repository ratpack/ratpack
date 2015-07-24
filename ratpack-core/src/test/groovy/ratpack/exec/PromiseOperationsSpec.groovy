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
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.CountDownLatch

import static ratpack.func.Action.throwException

class PromiseOperationsSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()
  List<String> events = []
  def latch = new CountDownLatch(1)


  def exec(Action<? super Execution> action, Action<? super Throwable> onError = Action.noop()) {
    execHarness
      .controller.exec()
      .onError(onError)
      .onComplete {
      events << "complete"
      latch.countDown()
    }.start {
      action.execute(it)
    }

    latch.await()
  }

  def "can map promise"() {
    when:
    exec {
      Blocking.get { "foo" }
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
      Blocking.get { "foo" }
        .flatMap { s -> Blocking.get { s + "-bar" } }
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
      Promise.of { it.error(ex) }
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
      Promise.of { it.error(ex) }
        .map {}
        .flatMap { Blocking.get { "foo" } }
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
      Promise.of { it.error(ex) }
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
      Blocking.get { "foo" }
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
      Promise.of { it.error(ex) }
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
      Blocking.get { "foo" }
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
      Blocking.get { "foo" }
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
      Blocking.get { (String) null }
        .onNull { events << "null" }
        .map { it.toUpperCase() }
        .route({ it == "foo" }, { throw ex })
        .then(throwException(new RuntimeException("then-at-end")))
    }

    then:
    events == ["null", "complete"]
  }

  def "can use other promise with flatMap"() {
    when:
    exec { e ->
      Blocking.get {
        "foo"
      } flatMap {
        Promise.of { f -> Thread.start { f.success("foo") } }
      } then {
        events << it
      }
    }

    then:
    events == ["foo", "complete"]
  }

  def "can defer promise"() {
    when:
    def runner = new BlockingVariable<Runnable>()
    execHarness.controller.exec().onComplete { latch.countDown() }.start {
      Promise.of { f -> Thread.start { f.success("foo") } }.defer({ runner.set(it) }).then {
        events << it
      }
    }

    then:
    events == []

    when:
    runner.get().run()

    then:
    latch.await()
    events == ["foo"]
  }

  def "can be notified on promise starting"() {
    when:
    exec { e ->
      Blocking.get {
        events << "blocking"
        "foo"
      } onYield {
        events << "yield"
      } wiretap {
        events << "wiretap"
      } then {
        events << it
      }
    }

    then:
    events == ["yield", "blocking", "wiretap", "foo", "complete"]
  }

}
