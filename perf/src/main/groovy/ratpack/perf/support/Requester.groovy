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

package ratpack.perf.support

import groovy.transform.CompileStatic

import java.math.RoundingMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicLong

@CompileStatic
class Requester {

  private final String baseUrl

  Requester(String baseUrl) {
    this.baseUrl = baseUrl
  }

  RunResults run(String name, int numRequests, int rounds, int cooldown, ExecutorService executor, String endpoint) {
    List<BigDecimal> roundResults = []
    println "starting $name... ($numRequests requests per round)"
    rounds.times { int it ->
      println "  round ${it + 1} of $rounds"
      roundResults << runRound(numRequests, executor, endpoint)
      println "  cooldown"
      sleep(cooldown * 1000)
    }
    println "done"

    def result = (roundResults.sum(0) as BigDecimal) / rounds
    result = result.setScale(2, RoundingMode.HALF_UP)
    new RunResults(result)
  }

  private BigDecimal runRound(int numRequests, ExecutorService executor, String endpoint) {
    def url = new URL("$baseUrl/$endpoint")
    def latch = new CountDownLatch(numRequests)
    def counter = new AtomicLong(0)
    numRequests.times {
      executor.submit {
        try {
          def connection = url.openConnection()
          connection.inputStream.close()

          def responseHeaderValue = connection.getHeaderField("X-Response-Time")
          def responseMs = new BigDecimal(responseHeaderValue)

          // Adjust to long for atomic long
          def adjusted = responseMs.multiply(1000).toBigDecimal().unscaledValue().longValue() // keep 3 decimal points of accuracy

          counter.addAndGet(adjusted)
        } catch (Exception e) {
          e.printStackTrace()
        } finally {
          latch.countDown()
        }
      }
    }
    latch.await()

    counter.get() / 1000 / numRequests
  }

  void stopApp() {
    new URL("$baseUrl/stop").text
  }

}
