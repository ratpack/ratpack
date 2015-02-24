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

import ratpack.http.client.ReceivedResponse
import ratpack.test.internal.RatpackGroovyDslSpec

class RequestIdSpec extends RatpackGroovyDslSpec {

  def "add request uuids"() {
    given: 'a ratpack app with the logging request handlers added'
    handlers {
      handler RequestId.bind()
      handler {
        render request.get(RequestId).id
      }
    }

    when: 'a response is received'
    ReceivedResponse response = get()

    then: 'a correlation id was returned'
    response.body.text.length() == 36 // not the best test ever but UUIDs should be 36 characters long including the dashes.
  }

  def "add request logging"() {
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    def origOut = System.out
    def origErr = System.err
    def loggerOutput = new ByteArrayOutputStream()
    System.out = new PrintStream(loggerOutput, true)
    System.err = new PrintStream(loggerOutput, true)

    given: 'a ratpack app with the logging request handler added'
    handlers {
      handler RequestId.bindAndLog()
      handler("foo") {
        render request.get(RequestId).id
      }
      handler("bar") {
        render request.get(RequestId).id
      }
    }

    when: 'a request is sent'
    ReceivedResponse getResponse = get("foo")
    ReceivedResponse postResponse = post("bar")

    then: 'the request is logged with its correlation id'
    loggerOutput.toString().contains("GET /foo 200 id=$getResponse.body.text")
    loggerOutput.toString().contains("POST /bar 200 id=$postResponse.body.text")

    cleanup:
    System.out = origOut
    System.err = origErr
    System.clearProperty("org.slf4j.simpleLogger.logFile")
  }
}
