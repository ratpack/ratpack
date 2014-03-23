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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService

@CompileStatic
class Requester {

  private final String baseUrl

  Requester(String baseUrl) {
    this.baseUrl = baseUrl
  }

  RunResults run(String name, int batchSize, int batches, int rounds, int cooldown, ExecutorService executor, String endpoint) {
    def results = new RunResults()
    println "starting $name... ($batches batches of $batchSize requests)"
    rounds.times { int it ->
      println "  round ${it + 1} of $rounds"
      results.rounds << runRound(batchSize, batches, executor, endpoint)
      println "  cooldown"
      sleep (cooldown * 1000)
    }
    println "done"
    results
  }

  private RoundResults runRound(int batchSize, int batches, ExecutorService executor, String endpoint) {
    def results = new RoundResults()
    batches.times {
      def latch = new CountDownLatch(batchSize)

      def start = System.nanoTime()
      batchSize.times {
        executor.submit {
          try {
            new URL("$baseUrl/$endpoint").openConnection().inputStream.close()
          } finally {
            latch.countDown()
          }
        }
      }
      latch.await()
      def stop = System.nanoTime()
      results.batches << ((stop - start) / 1000000)
    }

    results
  }

  void stopApp() {
    new URL("$baseUrl/stop").text
  }

}
