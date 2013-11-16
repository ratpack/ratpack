/*
 * Copyright 2013 the original author or authors.
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

package ratpack.http

import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentRequestSpec extends RatpackGroovyDslSpec {

  def "can serve requests concurrently without mixing up params"() {
    when:
    app {
      handlers {
        get(":id") {
          response.send pathTokens.id + ":" + request.queryParams.id
        }
      }
    }

    and:
    def threads = 500
    def latch = new CountDownLatch(threads)
    def results = []
    threads.times {
      results << null
    }

    startServerIfNeeded()

    threads.times { i ->
      Thread.start {
        try {
          def text = createRequest().get("$applicationUnderTest.address$i?id=$i").asString()
          assert text ==~ "\\d+:\\d+"
          def (id, value) = text.split(':').collect { it.toInteger() }
          results[id] = value
        } finally {
          latch.countDown()
        }
      }
    }

    latch.await(30, TimeUnit.SECONDS)

    then:
    (0..<threads).each {
      assert results[it] == it
    }
  }


}
