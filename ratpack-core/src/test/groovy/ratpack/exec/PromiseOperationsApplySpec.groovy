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

class PromiseOperationsApplySpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()

  List events = []
  def latch = new CountDownLatch(1)

  def exec(Action<? super ExecControl> action, Action<? super Throwable> onError = Action.noop()) {
    execHarness
      .exec()
      .onError(onError)
      .onComplete {
      events << "complete"
      latch.countDown()
    }.start {
      action.execute(it.control)
    }

    latch.await()
  }

  def "can apply operation"() {
    when:
    exec {
      it.blocking { 1 }
        .apply { it.map { it * 2 } }
        .then { events << it }
    }

    then:
    events == [2, "complete"]
  }

  def "can apply failing operation"() {
    when:
    exec({
      it.blocking { 1 }
        .apply { throw new Exception("!@") }
        .then { events << it }
    }, {
      events << it.message
    })

    then:
    events == ["!@", "complete"]
  }

  def "can catch apply failing operation"() {
    when:
    exec({
      it.blocking { 1 }
        .apply { throw new Exception("!@") }
        .onError { events << it.message }
        .then { events << it }
    })

    then:
    events == ["!@", "complete"]
  }

  def "can apply function to failed promise"() {
    when:
    exec({
      it.blocking { throw new Exception("!@") }
        .apply {
        events << "in apply"
        it.map { events << "in apply map"; it }
      }
      .onError { events << it.message }
        .then { events << it }
    })

    then:
    events == ["in apply", "!@", "complete"]
  }

  def "can catch error in apply"() {
    when:
    exec({
      it.blocking { throw new Exception("!@") }
        .apply {
        events << "in apply"
        it.onError { events << "in apply onError" }
      }
      .onError { events << "in outer onError" + it }
        .then { events << it }
    })

    then:
    events == ["in apply", "in apply onError", "complete"]
  }

  def "can apply unneeded error strategy"() {
    when:
    exec({
      it.blocking { 1 }
        .apply {
        events << "in apply"; it.onError { events << "in apply onError" }
      }
      .then { events << it }
    })

    then:
    events == ["in apply", 1, "complete"]
  }

}
