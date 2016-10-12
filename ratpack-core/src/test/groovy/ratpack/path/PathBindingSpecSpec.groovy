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

import ratpack.test.ApplicationUnderTest
import ratpack.test.internal.RatpackGroovyDslSpec

class PathBindingSpecSpec extends RatpackGroovyDslSpec {

  def "binding has description"() {
    when:
    handlers {
      prefix(":foo/:bar") {
        get("xxx") {
          render pathBinding.description
        }
        get("::\\d\\d") {
          render pathBinding.description
        }
        get(":n:\\d\\d\\d") {
          render pathBinding.description
        }
        get(":x?") {
          render pathBinding.description
        }
        prefix(":foo/:bar") {
          get("xxx") {
            render pathBinding.description
          }
        }
      }
    }

    then:
    getText("a/b/xxx") == ":foo/:bar/xxx"
    getText("a/b") == ":foo/:bar/:x?"
    getText("a/b/c") == ":foo/:bar/:x?"
    getText("a/b/11") == ":foo/:bar/::\\d\\d"
    getText("a/b/111") == ":foo/:bar/:n:\\d\\d\\d"
    getText("a/b/a/b/xxx") == ":foo/:bar/:foo/:bar/xxx"
  }

  def "binding incorrectly formatted paths to tokens should not cause server errors"() {
    when:
    handlers {
      get(":token") {
        response.send()
      }
    }

    then:
    statusCode(applicationUnderTest, "networking%party") != 500
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

}
