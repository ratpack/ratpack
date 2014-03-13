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

package ratpack.background

import ratpack.error.ServerErrorHandler
import ratpack.error.internal.PrintingServerErrorHandler
import ratpack.test.internal.RatpackGroovyDslSpec

class BackgroundSpec extends RatpackGroovyDslSpec {

  def "can perform groovy background operations"() {
    when:
    def steps = []
    handlers {
      get {
        steps << "start"
        background {
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

  def "by default errors during background operations are forwarded to server error handler"() {
    when:
    modules {
      bind ServerErrorHandler, PrintingServerErrorHandler
    }
    handlers {
      get {
        background {
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
        background {
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
    modules {
      bind ServerErrorHandler, PrintingServerErrorHandler
    }
    handlers {
      get {
        background {
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
    modules {
      bind ServerErrorHandler, PrintingServerErrorHandler
    }
    handlers {
      get {
        background {
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
    modules {
      bind ServerErrorHandler, PrintingServerErrorHandler
    }
    handlers {
      get {
        //noinspection GroovyAssignabilityCheck
        background {
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
    modules {
      bind ServerErrorHandler, PrintingServerErrorHandler
    }
    handlers {
      get {
        //noinspection GroovyAssignabilityCheck
        background {
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
      handler {
        background {
          [foo: "bar"]
        } then {
          response.send it.toString()
        }
      }
    }

    then:
    getText() == [foo: "bar"].toString()
  }

  def "can read request body in background"() {
    when:
    handlers {
      handler {
        background {
          sleep 1000 // allow the original foreground thread to finish, Netty will reclaim the buffer
          request.body.text
        } then {
          render it.toString()
        }
      }
    }

    then:
    request.content("foo")
    postText() == "foo"
  }

  def "background processing does not start until foreground processing has unwound"() {
    given:
    def events = []

    when:
    handlers {
      handler {
        next()
        events << "foreground"
      }
      handler {
        background {
          events << "background"
        } then {
          background {
            events << "inner background"
          } then {
            render "ok"
          }
          sleep 500
          events << "inner foreground"
        }
        sleep 500
      }
    }

    and:
    get()

    then:
    events == ["foreground", "background", "inner foreground", "inner background"]
  }



}
