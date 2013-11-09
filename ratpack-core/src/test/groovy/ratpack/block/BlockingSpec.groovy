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

package ratpack.block

import ratpack.error.internal.PrintingServerErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.groovy.Util.exec

class BlockingSpec extends RatpackGroovyDslSpec {

  def "can perform blocking operations"() {
    when:
    def steps = []
    app {
      handlers {
        get {
          steps << "start"
          exec blocking, { sleep 300; steps << "operation"; 2 }, { Integer result ->
            steps << "then"
            response.send result.toString()
          }
          steps << "end"
        }
      }
    }

    then:
    text == "2"
    steps == ["start", "end", "operation", "then"]
  }

  def "by default errors during blocking operations are forwarded to server error handler"() {
    when:
    app {
      modules {
        bind ServerErrorHandler, PrintingServerErrorHandler
      }
      handlers {
        get {
          exec blocking,
              { sleep 300; throw new Exception("!") }, // blocking
              { /* never called */ } // on success
        }
      }
    }

    then:
    text.startsWith(new Exception("!").toString())
    response.statusCode == 500
  }

  def "can use custom error handler"() {
    when:
    app {
      handlers {
        get {
          exec blocking,
              { sleep 300; throw new Exception("!") }, // blocking
              { response.status(210).send("error: $it.message") }, // on error
              { /* never called */ } // on success
        }
      }
    }

    then:
    text == "error: !"
    response.statusCode == 210
  }

  def "errors in custom error handlers are forwarded to the server error handler"() {
    when:
    app {
      modules {
        bind ServerErrorHandler, PrintingServerErrorHandler
      }
      handlers {
        get {
          exec blocking,
              { throw new Exception("!") },
              { throw new Exception("!!", it) },
              { /* never called */ }
        }
      }
    }

    then:
    text.startsWith(new Exception("!!", new Exception("!")).toString())
    response.statusCode == 500
  }

  def "errors in success handlers are forwarded to the server error handler"() {
    when:
    app {
      modules {
        bind ServerErrorHandler, PrintingServerErrorHandler
      }
      handlers {
        get {
          exec blocking,
              { 1 },
              { throw new Exception("!") }, // not expecting this to be called
              { throw new Exception("success") }
        }
      }
    }

    then:
    text.startsWith(new Exception("success").toString())
    response.statusCode == 500
  }

  def "closure arg type mismatch errors on success handler are handled well"() {
    when:
    app {
      modules {
        bind ServerErrorHandler, PrintingServerErrorHandler
      }
      handlers {
        get {
          exec blocking,
              { 1 },
              { List<String> result -> response.send("unexpected") }
        }
      }
    }

    then:
    text.startsWith "groovy.lang.MissingMethodException"
    response.statusCode == 500
  }

  def "closure arg type mismatch errors on error handler are handled well"() {
    when:
    app {
      modules {
        bind ServerErrorHandler, PrintingServerErrorHandler
      }
      handlers {
        get {
          exec blocking,
              { throw new Exception("!") },
              { String string -> response.send("unexpected") },
              { response.send("unexpected - value") }
        }
      }
    }

    then:
    text.startsWith "groovy.lang.MissingMethodException"
    response.statusCode == 500
  }

  def "delegate in closure actions is no the arg"() {
    when:
    app {
      handlers {
        handler {
          exec blocking, {
            [foo: "bar"]
          }, {
            response.send it.toString()
          }
        }
      }
    }

    then:
    getText() == [foo: "bar"].toString()
  }
}
