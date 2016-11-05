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

import ratpack.exec.internal.ThreadBinding
import ratpack.func.Action
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll
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
      .controller.fork()
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

  @Unroll
  def "can mapIf promise when the predicate is #predicate"() {
    when:
    exec {
      Blocking.get { originalValue }
        .mapIf( { it == "foo" }, { it + "-true" })
        .then { events << it }
    }

    then:
    events == [mappedValue, "complete"]

    where:
    originalValue | mappedValue | predicate
    "foo"         | "foo-true"  | true
    "bar"         | "bar"       | false
  }

  @Unroll
  def "can mapIfOrElse promise when the predicate is #predicate"() {
    when:
    exec {
      Blocking.get { originalValue }
        .mapIfOrElse( { it == "foo" }, { it + "-true" }, { it + "-false" })
        .then { events << it }
    }

    then:
    events == [mappedValue, "complete"]

    where:
    originalValue | mappedValue | predicate
    "foo"         | "foo-true"  | true
    "bar"         | "bar-false" | false
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

  @Unroll
  def "can flatMapIf promise when the predicate is #predicate"() {
    when:
    exec {
      Blocking.get { originalValue }
        .flatMapIf( { s -> s == "foo" }, { s -> Blocking.get { s + "-true" } } )
        .then { events << it }
    }

    then:
    events == [mappedValue, "complete"]

    where:
    originalValue | mappedValue | predicate
    "foo"         | "foo-true"  | true
    "bar"         | "bar" | false
  }

  @Unroll
  def "can flatMapIfOrElse promise when the predicate is #predicate"() {
    when:
    exec {
      Blocking.get { originalValue }
        .flatMapIfOrElse( { s -> s == "foo" }, { s -> Blocking.get { s + "-true" } } , { s -> Blocking.get { s + "-false" } } )
        .then { events << it }
    }

    then:
    events == [mappedValue, "complete"]

    where:
    originalValue | mappedValue | predicate
    "foo"         | "foo-true"  | true
    "bar"         | "bar-false" | false
  }

  def "errors are propagated down map chain"() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec { e ->
      Promise.async { it.error(ex) }
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
      Promise.async { it.error(ex) }
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
      Promise.async { it.error(ex) }
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
      Promise.async { it.error(ex) }
        .map {}
        .mapError { throw new RuntimeException(it.message + "-changed") }
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
        Promise.async { f -> Thread.start { f.success("foo") } }
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
    execHarness.controller.fork().onComplete { latch.countDown() }.start {
      Promise.async { f -> Thread.start { f.success("foo") } }.defer({ runner.set(it) }).then {
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

  def "deferred promise can use promises even when promises are queued"() {
    when:
    def runner = new BlockingVariable<Runnable>(200)
    execHarness.controller.fork().onComplete { latch.countDown() }.start {
      Promise.value("foo").defer { runner.set(it) }.then {
        Promise.async { it.success("foo") }.then {
          events << "inner"
        }
      }
      Promise.value("outer").then { events << it }
    }

    then:
    events == []

    when:
    runner.get().run()

    then:
    latch.await()
    events == ["inner", "outer"]
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

  def "wiretap receives failure results"() {
    when:
    exec { e ->
      Promise.error(new Exception("!")).wiretap { r -> events.add(r.throwable.message) }.then {}
    }

    then:
    events == ["!", "complete"]
  }

  def "can use blocking map"() {
    when:
    exec {
      Promise.value("foo").blockingMap {
        events << ThreadBinding.get().get().isCompute()
        "bar"
      } then {
        events << it
      }
    }

    then:
    events == [false, "bar", "complete"]
  }

  def "can error in blocking map"() {
    when:
    exec {
      Promise.value("foo").blockingMap {
        throw new Error("!")
      } onError {
        events << it.message
      } then {
        events << "bar"
      }
    }

    then:
    events == ["!", "complete"]
  }

  def "can use blocking op"() {
    when:
    exec {
      Promise.value("foo").blockingOp {
        events << ThreadBinding.get().get().isCompute()
      } then {
        events << it
      }
    }

    then:
    events == [false, "foo", "complete"]
  }

  def "can error in blocking op"() {
    when:
    exec {
      Promise.value("foo").blockingOp {
        throw new Error("!")
      } onError {
        events << it.message
      } then {
        events << it
      }
    }

    then:
    events == ["!", "complete"]
  }

  def "can apply action to promised value before continuing"() {
    when:
    exec {
      Promise.value("foo").next { v ->
        events << "one"
      }.map { v ->
        v.reverse()
      }.next { v ->
        events << "two"
      }.then { v ->
        events << "three"
      }
    }

    then:
    events == ["one", "two", "three", "complete"]
  }

  def "can execute async actions on promise"() {
    when:
    exec {
      Promise.value("foo").next { v ->
        Promise.async { d -> Thread.start { d.success(v) } }.then { v2 ->
          events << "one"
        }
      }.then { v ->
        events << "two"
      }
    }

    then:
    events == ["one", "two", "complete"]
  }

  def "can apply operation to promised value before continuing"() {
    when:
    exec {
      Promise.value("foo").nextOp { v ->
        return Operation.of {
          events << "one"
        }
      }.next { v ->
        events << v
      }.nextOp { v ->
        return Operation.of {
          events << "two"
        }
      }.map { v ->
        return v.reverse()
      }.nextOp { v ->
        Operation.of {
          events << "three"
        }
      }.next { v ->
        events << v
      }.operation { v ->
        events << "four"
      }.next {
        events << "five"
      }.then()
    }

    then:
    events == ["one", "foo", "two", "three", "oof", "four", "five", "complete"]
  }

  def "exception thrown in promise consumer is routed to execution error handler"() {
    given:
    def ex = new Exception("!")

    when:
    exec ({
      Promise.value("foo").onError {
        events << "unexpected"
      }.then {
        throw ex
      }
    }, {
      events << it
    }
    )

    then:
    events == [ex, "complete"]
  }

  def "unchecked exception thrown in promise consumer is routed to execution error handler"() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec ({
      Promise.value("foo").onError {
        events << "unexpected"
      }.then {
        throw ex
      }
    }, {
      events << it
    })

    then:
    events == [ex, "complete"]
  }

  def "error thrown in promise consumer is routed to execution error handler"() {
    given:
    def ex = new Error("!")

    when:
    exec ({
      Promise.value("foo").onError {
        events << "unexpected"
      }.then {
        throw ex
      }
    }, {
      events << it
    })

    then:
    events == [ex, "complete"]
  }
}
