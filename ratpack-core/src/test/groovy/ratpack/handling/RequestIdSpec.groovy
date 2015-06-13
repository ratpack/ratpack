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

class RequestIdSpec extends RatpackGroovyDslSpec {

  def "add request uuids"() {
    given: 'a ratpack app with the logging request handlers added'
    handlers {
      all RequestId.bind()
      all {
        render request.get(RequestId).id
      }
    }

    when: 'a response is received'
    ReceivedResponse response = get()

    then: 'a correlation id was returned'
    response.body.text.length() == 36 // not the best test ever but UUIDs should be 36 characters long including the dashes.
  }

  def "add request logging"() {
    def loggerOutput = new ByteArrayOutputStream()
    def logger = LoggerFactory.getLogger(RequestId)
    logger.TARGET_STREAM = new GroovyPrintStream(loggerOutput)

    given:
    handlers {
      all RequestId.bindAndLog()
      path("foo") {
        render request.get(RequestId).id
      }
      path("bar") {
        render request.get(RequestId).id
      }
    }

    when: 'a request is sent'
    ReceivedResponse getResponse = get("foo")
    ReceivedResponse postResponse = post("bar")

    then: 'the request is logged with its correlation id'
    String output = loggerOutput.toString()
    output.contains("\"GET /foo HTTP/1.1\" 200 - id=$getResponse.body.text")
    output.contains("\"POST /bar HTTP/1.1\" 200 - id=$postResponse.body.text")
  }
}
