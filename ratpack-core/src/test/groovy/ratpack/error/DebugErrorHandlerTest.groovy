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

package ratpack.error

import ratpack.test.internal.RatpackGroovyDslSpec

class DebugErrorHandlerTest extends RatpackGroovyDslSpec {

  def "debug error handler prints info"() {
    given:
    def e = new RuntimeException("!")

    when:
    modules {
      bind ServerErrorHandler, new DebugErrorHandler()
      bind ClientErrorHandler, new DebugErrorHandler()
    }

    handlers {
      get("client") { clientError(404) }
      get("server") { error(e) }
    }

    then:
    with(get("client")) {
      statusCode == 404
      body.asString() == "Client error 404"
    }

    def str = new StringWriter().with {
      e.printStackTrace(new PrintWriter(it))
      it.toString()
    }

    with(get("server")) {
      statusCode == 500
      body.asString() == str
    }
  }
}
