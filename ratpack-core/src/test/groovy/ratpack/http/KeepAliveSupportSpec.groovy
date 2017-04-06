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

package ratpack.http

import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch

class KeepAliveSupportSpec extends RatpackGroovyDslSpec {

  def "can serve keep alive requests - dev = #dev, post = #post"() {
    when:
    def threads = 2
    def requests = 20

    serverConfig {
      development(dev)
      it.threads(threads)
    }
    handlers {
      all {
        request.body.then {
          render "ok"
        }
      }
    }

    then:
    def url = applicationUnderTest.address.toURL()
    def latch = new CountDownLatch(threads * requests)
    def connections = new ConcurrentLinkedQueue<String>()
    threads.times { i ->
      Thread.start {
        requests.times {
          HttpURLConnection connection = url.openConnection()
          if (post) {
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.outputStream << "a" * (1024 * 6)
          }
          def connectionHeader = connection.getHeaderField("Connection")
          if (connectionHeader) {
            connections.add(connectionHeader)
          }
          latch.countDown()
        }
      }
    }
    latch.await()
    connections.size() == threads * requests
    connections.every { it == "keep-alive" }

    where:
    dev << [true, false, true, false]
    post << [true, true, false, false]
  }
}
