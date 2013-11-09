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

package ratpack.groovy.handling

import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.test.internal.RatpackGroovyDslSpec

class ErrorHandlingSpec extends RatpackGroovyDslSpec {

  def "handles 404"() {
    when:
    app {}

    then:
    get().statusCode == 404
  }

  def "handles internal error"() {
    when:
    app {
      handlers {
        get { throw new RuntimeException('error here') }
      }
    }

    then:
    get().statusCode == 500
  }

  def "can handle errors on forked threads"() {
    given:
    def errorHandler = new ServerErrorHandler() {
      void error(Context exchange, Exception exception) {
        exchange.response.send("Caught: $exception.message")
      }
    }

    when:
    app {
      modules {
        bind ServerErrorHandler, errorHandler
      }
      handlers {
        get {
          withErrorHandling new Thread({
            throw new Exception("thrown in forked thread")
          })
        }
      }
    }

    then:
    text == "Caught: thrown in forked thread"
  }

  def "can use service on forked threads"() {
    given:
    def errorHandler1 = new ServerErrorHandler() {
      void error(Context exchange, Exception exception) {
        exchange.response.send("1: $exception.message")
      }
    }

    def errorHandler2 = new ServerErrorHandler() {
      void error(Context exchange, Exception exception) {
        exchange.response.send("2: $exception.message")
      }
    }

    when:
    app {
      modules {
        bind ServerErrorHandler, errorHandler1
      }
      handlers {
        get { exchange ->
          withErrorHandling new Thread({
            insert(Arrays.asList(new Handler() {
              @Override
              void handle(Context context) {
                throw new Exception("down here")
              }
            }), ServerErrorHandler, errorHandler2)
          })
        }
      }
    }

    then:
    text == "2: down here"
  }

  def "can segment error handlers"() {
    given:
    def errorHandler1 = new ServerErrorHandler() {
      void error(Context exchange, Exception exception) {
        exchange.response.send("1: $exception.message")
      }
    }
    def errorHandler2 = new ServerErrorHandler() {
      void error(Context exchange, Exception exception) {
        exchange.response.send("2: $exception.message")
      }
    }

    when:
    app {
      modules {
        bind ServerErrorHandler, errorHandler1
      }
      handlers {
        register(ServerErrorHandler, errorHandler2) {
          get("a") {
            throw new Exception("1")
          }
        }
        get("b") {
          throw new Exception("2")
        }
      }
    }

    then:
    getText("a") == "2: 1"
    getText("b") == "1: 2"
  }

  def "can use service handler"() {
    given:
    def errorHandler = new ServerErrorHandler() {
      void error(Context exchange, Exception exception) {
        exchange.response.send("Caught: $exception.message")
      }
    }

    when:
    app {
      modules {
        bind ServerErrorHandler, errorHandler
      }
      handlers {
        get {
          throw new Exception("thrown")
        }
      }
    }

    then:
    text == "Caught: thrown"
  }

}
