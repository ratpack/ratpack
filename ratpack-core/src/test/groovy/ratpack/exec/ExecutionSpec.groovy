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
import ratpack.launch.LaunchConfigBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

class ExecutionSpec extends Specification {

  @AutoCleanup
  ExecController controller
  def events = []
  def latch = new CountDownLatch(1)

  def setup() {
    controller = LaunchConfigBuilder.noBaseDir().build().execController
  }

  def exec(Action<? super Execution> action) {
    controller.start {
      it.onComplete {
        latch.countDown()
      }

      action.execute(it)
    }
    latch.await()
  }

  def "exception thrown after promise prevents promise from running"() {
    when:
    exec { e ->
      e.setErrorHandler {
        events << "error"
        e.complete()
      }

      e.promise { f ->
        events << "action"
        controller.executor.submit {
          f.success(1)
        }
      } then {
        events << "then"
        e.complete()
      }

      throw new RuntimeException("!")
    }

    then:
    events == ["error"]
  }

  def "error handler can perform blocking ops"() {
    when:
    exec { e ->
      e.setErrorHandler {
        e.blocking { 2 } then { events << "error"; e.complete() }
      }

      throw new RuntimeException("!")
    }

    then:
    events == ["error"]
  }

  def "error handler can perform blocking ops then blocking opts"() {
    when:
    exec { e ->
      e.setErrorHandler {
        e.blocking {
          2
        } then {
          e.blocking {
            2
          } then {
            events << "error"
            e.complete()
          }
        }
      }

      throw new RuntimeException("!")
    }

    then:
    events == ["error"]
  }

  def "error handler can throw error"() {
    when:
    exec { e ->
      e.setErrorHandler {
        events << "e$it.message".toString()
        if ("e2" in events) {
          e.complete()
        } else {
          throw new RuntimeException("2")
        }
      }

      throw new RuntimeException("1")
    }

    then:
    events == ["e1", "e2"]
  }

  def "promise is bound to subscribing execution"() {
    when:
    exec { e1 ->
      def p = deferredPromise { it.success(2) }

      e1.controller.start { e2 ->
        e2.onComplete {
          e1.complete()
        }
        p.then {
          assert e2.controller.execution == e2
          events << "then"
          e2.complete()
        }
      }
    }

    then:
    events == ["then"]
  }

  def <T> Promise<T> deferredPromise(Action<Fulfiller<T>> action) {
    controller.control.promise { f ->
      Thread.start {
        action.execute(f)
      }
    }
  }
}
