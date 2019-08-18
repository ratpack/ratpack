/*
 * Copyright 2016 the original author or authors.
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

import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch

class ExecutionErrorHandlingSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()

  @Timeout(10)
  def "can add error handlers using onError"() {
    setup:
    def latch = new CountDownLatch(1)

    when:
    harness.controller.fork().onError { t ->
      latch.countDown()
    }.start {
      throw new RuntimeException("!")
    }

    and:
    latch.await()

    then:
    notThrown(Throwable)
  }

  void exec(ExecutionErrorListener... errorListeners) {
    harness.controller.fork().onStart { e ->
      errorListeners.each { errorListener ->
        e.add(ExecutionErrorListener, errorListener)
      }
    }.start {
      throw new RuntimeException("!")
    }
  }

  @Timeout(10)
  def "error listeners are pulled from registry"() {
    setup:
    def latch = new CountDownLatch(1)
    def result = ""
    def errorListener = { e, t ->
      result = t.message
      latch.countDown()
    } as ExecutionErrorListener

    when:
    exec(errorListener)

    and:
    latch.await()

    then:
    result == "!"
  }

  @Timeout(10)
  def "multiple error listeners will be invoked and in registry order"() {
    setup:
    def latch = new CountDownLatch(2)
    def results = []
    def errorListener1 = { e, t ->
      results << 1
      latch.countDown()
    } as ExecutionErrorListener
    def errorListener2 = { e, t ->
      results << 2
      latch.countDown()
    } as ExecutionErrorListener

    when:
    exec(errorListener1, errorListener2)

    and:
    latch.await()

    then:
    results == [2, 1]
  }

  def "can use promise inside of error listener"() {
    setup:
    def latch = new CountDownLatch(1)
    def result
    def errorListener = { e, t ->
      Operation.of {
        result = 1
        latch.countDown()
      }.then()
    } as ExecutionErrorListener

    when:
    exec(errorListener)

    and:
    latch.await()

    then:
    result == 1
  }
}
