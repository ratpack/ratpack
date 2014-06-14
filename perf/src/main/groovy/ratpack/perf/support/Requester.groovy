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
import ratpack.perf.Settings

import java.math.RoundingMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicLong
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class Requester {

  private final static int DECIMAL_ACCURACY = 5

  private final String baseUrl

  Requester(String baseUrl) {
    this.baseUrl = baseUrl
  }

  RunResults run(String name, Settings settings, ExecutorService executor, String endpoint) {
    List<BigDecimal> roundResults = []
    log.info "starting $name... ($settings.numRequests requests per round)"
    settings.rounds.times { int it ->
      log.debug "  round ${it + 1} of $settings.rounds"
      roundResults << runRound(settings.numRequests, executor, endpoint)
      log.debug "  cooldown"
      sleep(settings.cooldown * 1000)
    }
    log.info "done"

    def result = (roundResults.sum(0) as BigDecimal) / settings.rounds
    new RunResults(result.setScale(DECIMAL_ACCURACY, RoundingMode.HALF_UP))
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

          // Adjust to long for atomic long
          long value = new BigDecimal(responseHeaderValue).setScale(DECIMAL_ACCURACY).unscaledValue().longValue()

          counter.addAndGet(value)
        } catch (Exception e) {
          log.error "", e
        } finally {
          latch.countDown()
        }
      }
    }
    latch.await()

    def whole = new BigDecimal(counter.get())
    def millis = whole.divide(Math.pow(10, DECIMAL_ACCURACY).toBigDecimal())
    def average = millis.divide(numRequests.toBigDecimal())
    average.setScale(DECIMAL_ACCURACY, RoundingMode.HALF_UP)
  }

  void stopApp() {
    new URL("$baseUrl/stop").text
  }

}
