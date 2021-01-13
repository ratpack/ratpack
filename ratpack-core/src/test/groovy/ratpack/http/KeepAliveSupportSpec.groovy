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
    threads.times { i ->
      Thread.start {
        requests.times {
          HttpURLConnection connection = url.openConnection()
          if (post) {
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.outputStream << "a" * (1024 * 6)
          }
          connection.getHeaderField("Connection") == "keep-alive"
          latch.countDown()
        }
      }
    }
    latch.await()

    where:
    dev << [true, false, true, false]
    post << [true, true, false, false]
  }

  def "can serve keep alive requests to files"() {
    when:
    def threads = 5
    def requests = 20

    serverConfig {
      it.threads(threads)
    }
    def content = "a" * 32342 // must be > than a read chunk size
    baseDir.write("f.txt", content)
    handlers {
      files()
    }

    then:
    def url = "${applicationUnderTest.address}f.txt".toURL()
    def latch = new CountDownLatch(threads * requests)
    threads.times { i ->
      Thread.start {
        requests.times {
          try {
            HttpURLConnection headConnection = url.openConnection()
            headConnection.setRequestMethod("HEAD")
            assert headConnection.getHeaderField("Content-Length") == content.length().toString()
            assert headConnection.responseCode == 200
            assert headConnection.inputStream.text == ""
            HttpURLConnection getConnection = url.openConnection()
            assert getConnection.getHeaderField("Content-Length") == content.length().toString()
            assert getConnection.responseCode == 200
            assert getConnection.inputStream.text == content
          } finally {
            latch.countDown()
          }
        }
      }
    }
    latch.await()
  }

}
