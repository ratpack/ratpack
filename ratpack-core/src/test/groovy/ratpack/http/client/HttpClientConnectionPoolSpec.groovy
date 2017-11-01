/*
 * Copyright 2017 the original author or authors.
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

package ratpack.http.client

import ratpack.exec.ExecResult
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Shared

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class HttpClientConnectionPoolSpec extends BaseHttpClientSpec {

  @AutoCleanup(value = "shutdown")
  @Shared
  def executor = Executors.newCachedThreadPool()

  @Shared
  def harness = ExecHarness.harness()

  def "pool limits the number of connection and its queue"() {
    given:
    def blockLatch = new CountDownLatch(1)
    def orderLatch = new CountDownLatch(1)

    def poolingHttpClient = HttpClient.of {
      it.poolSize(1).poolQueueSize(1)
    }

    otherApp {
      get {
        orderLatch.countDown()
        blockLatch.await()
        render "ok"
      }
    }
    def requestClosure = { poolingHttpClient.get(otherAppUrl()).map { r -> r.body.text } }

    when:
    def blockedFuture = executor.submit({ harness.yield(requestClosure) } as Callable<ExecResult<String>>)
    orderLatch.await()

    def queuedOrRejectedFuture1 = executor.submit({ harness.yield(requestClosure) } as Callable<ExecResult<String>>)
    def queuedOrRejectedFuture2 = executor.submit({ harness.yield(requestClosure) } as Callable<ExecResult<String>>)

    then:
    !blockedFuture.isDone()

    blockLatch.countDown()
    blockedFuture.get().value == "ok"

    queuedOrRejectedFuture1.get().error ^ queuedOrRejectedFuture2.get().error

    with(queuedOrRejectedFuture1.get()) {
        error ?
          throwable.message == "Too many outstanding acquire operations"
          : value == "ok"
    }
    with(queuedOrRejectedFuture2.get()) {
      error ?
        throwable.message == "Too many outstanding acquire operations"
        : value == "ok"
    }
  }
}
