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
    execHarness.controller.exec()
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
        .onError { throw new NullPointerException("!") }
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

}
