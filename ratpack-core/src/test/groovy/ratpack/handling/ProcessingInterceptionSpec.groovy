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

import ratpack.error.ServerErrorHandler
import ratpack.error.DebugErrorHandler
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.registry.Registries.registry

class ProcessingInterceptionSpec extends RatpackGroovyDslSpec {

  def record = []

  class RecordingInterceptor implements ProcessingInterceptor {

    final String id

    RecordingInterceptor(String id) {
      this.id = id
    }

    @Override
    void init(Context context) {
      record << "$id:init"

    }

    @Override
    void intercept(ProcessingInterceptor.Type type, Context context, Runnable continuation) {
      record << "$id:$type"
      continuation.run()
    }
  }

  def "interceptors are executed in order"() {
    given:
    def interceptor1 = new RecordingInterceptor("1")
    def interceptor2 = new RecordingInterceptor("2")

    modules {
      bind interceptor1
    }

    handlers {
      handler {
        next(registry(interceptor2))
      }
      get(":path") {
        background {
          2
        } then {
          render pathTokens.path
        }
      }
    }

    when:
    get("1")

    then:
    record == ["1:init", "1:FOREGROUND", "2:init", "2:FOREGROUND", "1:BACKGROUND", "2:BACKGROUND", "1:FOREGROUND", "2:FOREGROUND"]
  }

  class ErroringInterceptor extends RecordingInterceptor {

    ErroringInterceptor(String id) {
      super(id)
    }

    @Override
    void intercept(ProcessingInterceptor.Type type, Context context, Runnable continuation) {
      super.intercept(type, context, continuation)
      throw new RuntimeException("$type:$id")
    }
  }

  def "errors thrown by interceptors are ignored"() {
    given:
    def interceptor1 = new ErroringInterceptor("1")
    def interceptor2 = new ErroringInterceptor("2")

    modules {
      bind interceptor1
    }

    handlers {
      handler {
        next(registry(interceptor2))
      }
      get(":path") {
        background {
          2
        } then {
          render pathTokens.path
        }
      }
    }

    when:
    get("1")

    then:
    record == ["1:init", "1:FOREGROUND", "2:init", "2:FOREGROUND", "1:BACKGROUND", "2:BACKGROUND", "1:FOREGROUND", "2:FOREGROUND"]
  }

  static class SimpleErrorHandler implements ServerErrorHandler {
    @Override
    void error(Context context, Exception exception) throws Exception {
      context.response.status(500).send(exception.message)
    }
  }

  static class ErrorInInitInterceptor implements ProcessingInterceptor {
    @Override
    void init(Context context) {
      throw new RuntimeException("error in init")
    }

    @Override
    void intercept(ProcessingInterceptor.Type type, Context context, Runnable continuation) {

    }
  }

  def "error in init on insert halts pipeline"() {
    when:
    modules {
      bind new ErrorInInitInterceptor()
      bind ServerErrorHandler, new SimpleErrorHandler()
    }
    handlers { handler { render "ok" } }

    then:
    with(get()) {
      statusCode == 500
      text == "error in init"
    }
  }

  def "error in init on next halts pipeline"() {
    when:
    handlers {
      handler { next(registry().add(ServerErrorHandler, new SimpleErrorHandler()).add(new ErrorInInitInterceptor()).build()) }
      handler { render "ok" }
    }

    then:
    with(get()) {
      statusCode == 500
      text == "error in init"
    }
  }

  def "intercepted handlers can throw exceptions"() {
    given:
    modules {
      bind new RecordingInterceptor("id") // just need any interceptor
      bind ServerErrorHandler, new DebugErrorHandler()
    }

    when:
    handlers {
      handler { throw new RuntimeException("!") }
    }

    then:
    get().statusCode == 500
  }

}
