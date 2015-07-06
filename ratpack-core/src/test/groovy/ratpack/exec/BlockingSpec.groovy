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

package ratpack.exec

import ratpack.error.ServerErrorHandler
import ratpack.func.Block
import ratpack.http.client.RequestSpec
import ratpack.test.exec.ExecHarness
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

import static ratpack.util.Exceptions.uncheck

class BlockingSpec extends RatpackGroovyDslSpec {

  def "can perform groovy blocking operations"() {
    when:
    def steps = []
    handlers {
      get {
        steps << "start"
        blocking {
          sleep 300
          steps << "operation"
          2
        } then { Integer result ->
          steps << "then"
          response.send result.toString()
        }
        steps << "end"
      }
    }

    then:
    text == "2"
    steps == ["start", "end", "operation", "then"]
  }

  def "by default errors during blocking operations are forwarded to server error handler"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get {
        blocking {
          sleep 300
          throw new Exception("!")
        } then {
          /* never called */
        }
      }
    }

    then:
    text.startsWith(new Exception("!").toString())
    response.statusCode == 500
  }

  def "can use custom error handler"() {
    when:
    handlers {
      get {
        blocking {
          sleep 300
          throw new Exception("!")
        } onError {
          response.status(210).send("error: $it.message")
        } then {
          // never called
        }
      }
    }

    then:
    text == "error: !"
    response.statusCode == 210
  }

  def "errors in custom error handlers are forwarded to the server error handler"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get {
        blocking {
          throw new Exception("!")
        } onError { Throwable t ->
          throw new Exception("!!", t)
        } then {
          /* never called */
        }
      }
    }

    then:
    text.startsWith(new Exception("!!", new Exception("!")).toString())
    response.statusCode == 500
  }

  def "errors in success handlers are forwarded to the server error handler"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get {
        blocking {
          1
        } onError {
          throw new Exception("!")
        } then {
          throw new Exception("success")
        }
      }
    }

    then:
    text.startsWith(new Exception("success").toString())
    response.statusCode == 500
  }

  def "closure arg type mismatch errors on success handler are handled well"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get {
        //noinspection GroovyAssignabilityCheck
        blocking {
          1
        } then { List<String> result ->
          response.send("unexpected")
        }
      }
    }

    then:
    text.startsWith "groovy.lang.MissingMethodException"
    response.statusCode == 500
  }

  def "closure arg type mismatch errors on error handler are handled well"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get {
        //noinspection GroovyAssignabilityCheck
        blocking {
          throw new Exception("!")
        } onError { String string ->
          response.send("unexpected")
        } then {
          response.send("unexpected - value")
        }
      }
    }

    then:
    text.startsWith "groovy.lang.MissingMethodException"
    response.statusCode == 500
  }

  def "delegate in closure actions is no the arg"() {
    when:
    handlers {
      all {
        blocking {
          [foo: "bar"]
        } then {
          response.send it.toString()
        }
      }
    }

    then:
    getText() == [foo: "bar"].toString()
  }

  def "can read request body in blocking operation"() {
    when:
    handlers {
      all {
        blocking {
          sleep 1000 // allow the original compute thread to finish, Netty will reclaim the buffer
          request.body.block().text
        } then {
          render it.toString()
        }
      }
    }

    and:
    requestSpec { RequestSpec request ->
      request.body.type("text/plain").stream { it << "foo" }
    }

    then:
    postText() == "foo"
  }

  def "blocking processing does not start until compute processing has unwound"() {
    given:
    def events = []

    when:
    handlers {
      all {
        next()
        events << "compute"
      }
      all {
        blocking {
          events << "blocking"
        } then {
          blocking {
            events << "inner blocking"
          } then {
            render "ok"
          }
          sleep 500
          events << "inner compute"
        }
        sleep 500
      }
    }

    and:
    get()

    then:
    events == ["compute", "blocking", "inner compute", "inner blocking"]
  }

  def "should throw UnmanagedThreadException when trying to use blocking outside of Execution"() {
    given:
    def promise = ExecHarness.harness().blocking { }

    when:
    uncheck({ promise.then {} } as Block)

    then:
    thrown(UnmanagedThreadException)

    when:
    uncheck({ promise.then {} } as Block)

    then:
    thrown(UnmanagedThreadException)
  }
}
