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

import ratpack.exec.ExecInterceptor
import ratpack.exec.Execution
import ratpack.func.Block
import ratpack.handling.Context
import ratpack.test.internal.RatpackGroovyDslSpec

class ReasonableErrorResponseSpec extends RatpackGroovyDslSpec {

  class ExceptionThrowingInterceptor implements ExecInterceptor {
    final private Error error

    ExceptionThrowingInterceptor(Error error) {
      this.error = error
    }

    void intercept(Execution execution, ExecInterceptor.ExecType execType, Block executionSegment) {
      throw error
    }
  }

  def "handler throws Error subclass during execution"() {
    given:
    def e = new Error("Error")

    when:
    handlers {
      get {
        throw e
      }
    }

    then:
    text == ""
    response.statusCode == 500
  }

  def "promise is fulfilled with an Error subclass error"() {
    given:
    def e = new Error("Error")

    when:
    handlers {
      get {
        async { downstream ->
          downstream.error(e)
        } then {
          response.send("This should never be reached")
        }
      }
    }

    then:
    text == ""
    response.statusCode == 500
  }

  def "promise throws Error subclass error"() {
    given:
    def e = new Error("Error")

    when:
    handlers {
      get {
        async {
          throw e
        } then {
          response.send("This should never be reached")
        }
      }
    }

    then:
    text == ""
    response.statusCode == 500
  }

  def "handler throws Error subclass from interceptor"() {
    given:
    def e = new Error("Error")
    def exceptionThrowingInterceptor = new ExceptionThrowingInterceptor(e)

    when:
    handlers {

      all {
        context.execution.addInterceptor(exceptionThrowingInterceptor, { Context context ->
          context.next()
        } as Block)
      }

      get {
        response.send("This should never be reached")
      }
    }

    then:
    text == ""
    response.statusCode == 500
  }

  def "promise is fulfilled with an Error subclass error thrown from interceptor"() {
    given:
    def e = new Error("Error")
    def exceptionThrowingInterceptor = new ExceptionThrowingInterceptor(e)

    when:
    handlers {

      all {
        context.execution.addInterceptor(exceptionThrowingInterceptor, { Context context ->
          context.next()
        } as Block)
      }

      get {
        async { downstream ->
          downstream.success("This should never be reached")
        } then { String string ->
          response.send(string)
        }

      }
    }

    then:
    text == ""
    response.statusCode == 500
  }

}
