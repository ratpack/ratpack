/*
 * Copyright 2018 the original author or authors.
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
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch

class PromiseCompletableFutureSpec extends Specification {

  def "test toCompletableFuture - success"() {
    given: "A null CompletableFuture"
    String value = "A"
    CompletableFuture<String> future = null
    def latch = new CountDownLatch(1)

    when: "Promise is converted to a CompletableFuture"
    ExecHarness.executeSingle {
      Operation.of {
        future = Promise.value(value).toCompletableFuture()
        latch.countDown()
      }
    }
    latch.await()

    then: "The future is accurate"
    future.get() == value
  }

  def "test toCompletableFuture - error"() {
    given: "A null CompletableFuture"
    Exception error = new Exception("A")
    CompletableFuture<String> future = null
    def latch = new CountDownLatch(1)

    when: "Promise is converted to a CompletableFuture"
    ExecHarness.executeSingle {
      Operation.of {
        future = Promise.error(error).toCompletableFuture()
        latch.countDown()
      }
    }
    latch.await()
    future.get()

    then: "The future is accurate"
    future.completedExceptionally
    def t = thrown(java.util.concurrent.ExecutionException)
    t.cause.message == "A"
  }

  def "test fromCompletableFuture - success"() {
    given: "A future"
    def value = "A"
    CompletableFuture<String> future = CompletableFuture.supplyAsync { value }

    when: "convert to a promise"
    def result = ExecHarness.yieldSingle {
      Promise.toPromise(future)
    }

    then: "it worked"
    result.success
    result.value == value
  }

  def "test fromCompletableFuture - error"() {
    given: "A future"
    Exception error = new Exception("A")
    CompletableFuture<String> future = CompletableFuture.supplyAsync { throw error }

    when: "convert to a promise"
    def result = ExecHarness.yieldSingle {
      Promise.toPromise(future)
    }

    then: "it worked"
    result.error
    result.throwable instanceof CompletionException
    result.throwable.cause.cause.message == "A"
  }

}
