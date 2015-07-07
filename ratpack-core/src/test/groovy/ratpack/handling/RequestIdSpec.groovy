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

package ratpack.handling

import groovy.io.GroovyPrintStream
import org.slf4j.LoggerFactory
import ratpack.http.client.ReceivedResponse
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RequestIdSpec extends RatpackGroovyDslSpec {

  def "add request id"() {
    given:
    handlers {
      all RequestId.bind()
      all {
        render request.get(RequestId).id
      }
    }

    when:
    ReceivedResponse response = get()

    then:
    response.body.text.length() == 36 // not the best test ever but UUIDs should be 36 characters long including the dashes.
  }

  def "use custom request id generator"() {
    given:
    bindings {
      bindInstance RequestId.Generator, { ctx ->
          return { 'foo' } as RequestId
      } as RequestId.Generator
    }
    handlers {
      all RequestId.bind()
      all {
        render request.get(RequestId).id
      }
    }

    when:
    ReceivedResponse response = get()

    then:
    response.body.text == 'foo'
  }

  def "request log includes request id"() {
    def loggerOutput = new ByteArrayOutputStream()
    def logger = LoggerFactory.getLogger(RequestId)
    def originalStream = logger.TARGET_STREAM
    def latch = new CountDownLatch(2)
    logger.TARGET_STREAM = new GroovyPrintStream(loggerOutput) {

      @Override
      void println(String s) {
        super.println(s)
        originalStream.println(s)
        if (s.contains(RequestLog.simpleName)) {
          latch.countDown()
        }
      }
    }

    given:
    int count = 0
    bindings {
      bindInstance RequestId.Generator, { ctx ->
        return new RequestId() {

          private final String id = "request-${count++}"

          @Override
          String getId() {
            return id
          }
        }
      } as RequestId.Generator
    }
    handlers {
      all RequestId.bindAndLog()
      path("foo") {
        render request.get(RequestId).id
      }
      path("bar") {
        render request.get(RequestId).id
      }
    }

    when:
    ReceivedResponse getResponse = get("foo")
    ReceivedResponse postResponse = post("bar")

    then:
    latch.await(5, TimeUnit.SECONDS)
    String output = loggerOutput.toString()
    output.contains("\"GET /foo HTTP/1.1\" 200 ${getResponse.body.text.length()} id=$getResponse.body.text")
    output.contains("\"POST /bar HTTP/1.1\" 200 ${postResponse.body.text.length()} id=$postResponse.body.text")
    count == 2
  }
}
