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

import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PromiseSpec extends Specification {

  @AutoCleanup
  def exec = ExecHarness.harness()

  def "simple promise"() {
    expect:
    exec.run {
      Promise.async {
        it.success(1)
      }.then {
        assert it == 1
      }
    }
  }

  def "cannot subscribe to promise when blocking"() {
    when:
    exec.yield {
      Blocking.get {
        Promise.value(1).then { it }
      }
    }.valueOrThrow

    then:
    def e = thrown ExecutionException
    e.message.startsWith("Promise.then() can only be called on a compute thread")
  }

  def "can execute block on complete"() {
    given:
    def latch = new CountDownLatch(1)

    when:
    def result = exec.yield {
      Promise.async { down ->
        Execution.fork().start {
          down.complete()
        }
      }.onComplete {
        latch.countDown()
      }
    }

    then:
    latch.await(1, TimeUnit.SECONDS)
    result.complete
    !result.success
    !result.error
  }
}
