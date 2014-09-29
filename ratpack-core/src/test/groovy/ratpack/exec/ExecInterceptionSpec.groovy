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

package ratpack.exec

import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.test.internal.RatpackGroovyDslSpec

class ExecInterceptionSpec extends RatpackGroovyDslSpec {

  def record = []

  class RecordingInterceptor implements ExecInterceptor {

    final String id

    RecordingInterceptor(String id) {
      this.id = id
    }

    @Override
    void intercept(ExecInterceptor.ExecType type, Runnable continuation) {
      record << "$id:$type"
      continuation.run()
    }
  }

  def "interceptors are executed in order"() {
    given:
    def interceptor1 = new RecordingInterceptor("1")
    def interceptor2 = new RecordingInterceptor("2")

    handlers {
      handler {
        addInterceptor(interceptor1) {
          addInterceptor(interceptor2) {
            next()
          }
        }
      }
      get(":path") {
        blocking {
          2
        } then {
          render pathTokens.path
        }
      }
    }

    when:
    get("1")

    then:
    record == ["1:COMPUTE", "2:COMPUTE", "1:BLOCKING", "2:BLOCKING", "1:COMPUTE", "2:COMPUTE"]
  }

  class ErroringInterceptor extends RecordingInterceptor {

    ErroringInterceptor(String id) {
      super(id)
    }

    @Override
    void intercept(ExecInterceptor.ExecType type, Runnable continuation) {
      println "$type:$id"
      super.intercept(type, continuation)
      throw new RuntimeException("$type:$id")
    }
  }

  def "errors thrown by interceptors are ignored"() {
    given:
    def interceptor1 = new ErroringInterceptor("1")
    def interceptor2 = new ErroringInterceptor("2")

    handlers {
      handler {
        addInterceptor(interceptor1) {
          addInterceptor(interceptor2) {
            next()
          }
        }
      }
      get(":path") {
        blocking {
          2
        } then {
          render pathTokens.path
        }
      }
    }

    when:
    get("1")

    then:
    record == ["1:COMPUTE", "2:COMPUTE", "1:BLOCKING", "2:BLOCKING", "1:COMPUTE", "2:COMPUTE"]
  }

  def "intercepted handlers can throw exceptions"() {
    given:
    bindings {
      bind new RecordingInterceptor("id") // just need any interceptor
      bind ServerErrorHandler, new DefaultDevelopmentErrorHandler()
    }

    when:
    handlers {
      handler { throw new RuntimeException("!") }
    }

    then:
    get().statusCode == 500
  }

}
