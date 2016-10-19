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
import ratpack.func.Block
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

class ExecInterceptionSpec extends RatpackGroovyDslSpec {

  def record = []

  class RecordingInterceptor implements ExecInterceptor, ExecInitializer {

    final String id

    RecordingInterceptor(String id) {
      this.id = id
    }

    @Override
    void init(Execution execution) {
      record << "init:$id"
    }

    @Override
    void intercept(Execution execution, ExecInterceptor.ExecType type, Block executionSegment) {
      record << "$id:$type"
      executionSegment.execute()
    }
  }

  def "interceptors are executed in order"() {
    given:
    def interceptor1 = new RecordingInterceptor("1")
    def interceptor2 = new RecordingInterceptor("2")

    handlers {
      all {
        execution.addInterceptor(interceptor1) {
          execution.addInterceptor(interceptor2) {
            next()
          }
        }
      }
      get(":path") {
        Blocking.get {
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
    void intercept(Execution execution, ExecInterceptor.ExecType type, Block executionSegment) {
      super.intercept(execution, type, executionSegment)
      throw new RuntimeException("$type:$id")
    }
  }

  def "errors thrown by interceptors bubble up"() {
    given:
    def interceptor1 = new ErroringInterceptor("1")
    def interceptor2 = new ErroringInterceptor("2")

    when:
    handlers {
      register {
        add(new SimpleErrorHandler())
      }
      all {
        execution.addInterceptor(interceptor1) {
          execution.addInterceptor(interceptor2) {
            next()
          }
        }
      }
      get(":path") {
        Blocking.get {
          2
        } then {
          render pathTokens.path
        }
      }
    }

    then:
    getText("1") == "java.lang.RuntimeException: COMPUTE:2"
    record == ["1:COMPUTE", "2:COMPUTE"]
  }

  def "intercepted handlers can throw exceptions"() {
    given:
    bindings {
      bindInstance new RecordingInterceptor("id") // just need any interceptor
      bindInstance ServerErrorHandler, new DefaultDevelopmentErrorHandler()
    }

    when:
    handlers {
      all { throw new RuntimeException("!") }
    }

    then:
    get().statusCode == 500
  }

  def "interceptors are initd"() {
    given:
    bindings {
      bindInstance new RecordingInterceptor("global")
    }
    serverConfig {
      development false
    }
    when:
    handlers {
      all {
        Promise.async { d ->
          Execution.fork().register { it.add(new RecordingInterceptor("registry")) }.start {
            d.success("foo")
          }
        } then {
          render it
        }
      }
    }

    then:
    text
    record == [
      "init:global", "global:COMPUTE",
      "init:global", "init:registry", "global:COMPUTE", "registry:COMPUTE", "global:COMPUTE"
    ]
  }

  def "execution interceptors wrap error handler"() {
    when:
    boolean on = false
    bindings {
      bindInstance(ExecInterceptor, { Execution e, ExecInterceptor.ExecType type, Block segment ->
        on = true
        try {
          segment.execute()
        } finally {
          on = false
        }
      } as ExecInterceptor)
      bindInstance(ServerErrorHandler, { ctx, e ->
        ctx.render(Boolean.toString(on))
      } as ServerErrorHandler)
    }
    handlers {
      all {
        throw new Exception("!")
      }
    }

    then:
    text == "true"
  }

  def "execution interceptors wrap completion handler"() {
    when:
    boolean on = false
    bindings {
      bindInstance(ExecInterceptor, { Execution e, ExecInterceptor.ExecType type, Block segment ->
        on = true
        try {
          segment.execute()
        } finally {
          on = false
        }
      } as ExecInterceptor)
    }
    handlers {
      all {
        render Promise.async { d ->
          Execution.fork().onComplete { d.success(Boolean.toString(on)) } start {}
        }
      }
    }

    then:
    text == "true"
  }

}
