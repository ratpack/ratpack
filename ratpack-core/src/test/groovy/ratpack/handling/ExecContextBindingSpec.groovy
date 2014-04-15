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

import ratpack.exec.ExecController
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.registry.Registries.just

/**
 * Tests that ExecController.getContext() supplies the right value during request processing.
 */
class ExecContextBindingSpec extends RatpackGroovyDslSpec {

  static class Thing {
    String value
  }

  def "exec controller provides right context"() {
    when:
    ExecController controller = null

    handlers {
      handler {
        controller = context.execController
        next()
      }
      register(just(new Thing(value: "1"))) {
        handler {
          response.headers.set("L1", controller.context.get(Thing).value)
          next()
        }
        register(just(new Thing(value: "2"))) {
          handler {
            blocking {
              response.headers.set("L2", controller.context.get(Thing).value)
            } then {
              controller.context.next()
            }
          }
        }
        register(just(new Thing(value: "3"))) {
          get {
            render controller.context.get(Thing).value
          }
        }
      }
    }

    then:
    getText() == "3"
    response.header("L1") == "1"
    response.header("L2") == "2"
  }

}
