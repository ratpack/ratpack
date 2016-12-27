/*
 * Copyright 2016 the original author or authors.
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

package ratpack.path

import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import ratpack.test.ApplicationUnderTest
import ratpack.test.internal.RatpackGroovyDslSpec

class InvalidPathEncodingSpec extends RatpackGroovyDslSpec {

  def "requesting incorrectly formatted paths results in a 400 response by default"() {
    when:
    handlers {
      get(":token") {
        response.send()
      }
    }

    then:
    statusCode(applicationUnderTest, "/networking%party") == 400
  }

  def "responses to requests with incorrectly formatted paths can be customised"() {
    when:
    bindings {
      bind(ServerErrorHandler, CustomInvalidPathEncodingServerErrorHandler)
    }
    handlers {
      get(":token") {
        response.send()
      }
    }

    then:
    statusCode(applicationUnderTest, "/networking%party") == 404
  }

  private int statusCode(ApplicationUnderTest applicationUnderTest, String path) {
    def port = applicationUnderTest.address.port
    Socket s = new Socket("localhost", port)
    def statusLine = s.withCloseable { socket ->
      def pw = new PrintWriter(socket.outputStream)
      pw.println("GET $path HTTP/1.1")
      pw.println("Host: localhost:$port")
      pw.println("")
      pw.flush()
      def reader = new InputStreamReader(socket.inputStream)
      reader.readLine()
    }
    statusLine.tokenize()[1].toInteger()
  }

  private static class CustomInvalidPathEncodingServerErrorHandler implements ServerErrorHandler {

    @Override
    void error(Context context, Throwable throwable) throws Exception {
      context.response.status(500).send()
    }

    @Override
    void error(Context context, InvalidPathEncodingException exception) throws Exception {
      context.clientError(404)
    }
  }

}
