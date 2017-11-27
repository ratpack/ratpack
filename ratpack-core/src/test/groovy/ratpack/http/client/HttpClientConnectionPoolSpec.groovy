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

import ratpack.exec.util.ParallelBatch
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.util.concurrent.AsyncConditions

import java.util.concurrent.CountDownLatch

class HttpClientConnectionPoolSpec extends BaseHttpClientSpec {

  @AutoCleanup
  def harness = ExecHarness.harness()

  def "pool limits the number of connection and its queue"() {
    given:
    def async = new AsyncConditions(2)
    def requestReceivedLatch = new CountDownLatch(1)
    def resumeRequestLatch = new CountDownLatch(1)

    def poolingHttpClient = HttpClient.of {
      it.poolSize(1).poolQueueSize(1)
    }

    otherApp {
      get {
        requestReceivedLatch.countDown()
        resumeRequestLatch.await()
        render "ok"
      }
    }

    when:
    def request = poolingHttpClient.get(otherAppUrl()).map { r -> r.body.text }

    then:
    Thread.start {
      async.evaluate {
        assert harness.yield { request }.valueOrThrow == "ok"
      }
    }
    requestReceivedLatch.await()

    and:
    Thread.start {
      async.evaluate {
        def results = harness.yield { ParallelBatch.of([request] * 3).yieldAll() }.valueOrThrow

        def success = results.findAll { it.success }
        assert success.size() == 1
        assert success.every { it.value == "ok" }

        def errors = results.findAll { it.error }
        assert errors.size() == 2
        assert errors.every { it.throwable.message == "Too many outstanding acquire operations" }
      }
    }
    resumeRequestLatch.countDown()

    and:
    async.await()
  }
}
