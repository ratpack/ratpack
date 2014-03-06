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

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import ratpack.test.internal.RatpackGroovyDslSpec

class BackgroundInterceptionSpec extends RatpackGroovyDslSpec {

  def "background interceptor is called when entering and leaving the background"() {
    given:
    def interceptor = Mock(BackgroundInterceptor)
    modules {
      bind BackgroundInterceptor, interceptor
    }
    handlers {
      get(":path") {
        background {
          2
        } onError {
          error it
        } then {
          render pathTokens.path
        }
      }
    }

    when:
    get("1")

    then:
    1 * interceptor.toBackground({ Context context -> context.request.path == "1" }, _) >> { it[1].run() }

    then:
    1 * interceptor.toForeground({ Context context -> context.request.path == "1" }, _) >> { it[1].run() }
  }

  def "background interceptors are executed in order"() {
    given:
    def interceptor1 = Mock(BackgroundInterceptor)
    def interceptor2 = Mock(BackgroundInterceptor)

    modules {
      register new AbstractModule() {
        @Override
        protected void configure() {
          Multibinder.newSetBinder(binder(), BackgroundInterceptor).with {
            addBinding().toInstance(interceptor1)
            addBinding().toInstance(interceptor2)
          }
        }
      }
    }

    handlers {
      get(":path") {
        background {
          2
        } onError {
          error new Exception(it)
        } then {
          render pathTokens.path
        }
      }
    }

    when:
    get("1")

    then:
    1 * interceptor2.toBackground({ Context context -> context.request.path == "1" }, _) >> { it[1].run() }

    then:
    1 * interceptor1.toBackground({ Context context -> context.request.path == "1" }, _) >> { it[1].run() }

    then:
    1 * interceptor1.toForeground({ Context context -> context.request.path == "1" }, _) >> { it[1].run() }

    then:
    1 * interceptor2.toForeground({ Context context -> context.request.path == "1" }, _) >> { it[1].run() }
  }


}
