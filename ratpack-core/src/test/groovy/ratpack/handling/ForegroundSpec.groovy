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

import ratpack.test.internal.RatpackGroovyDslSpec

class ForegroundSpec extends RatpackGroovyDslSpec {

  static class Thing {
    String value
  }

  def "can use context provider"() {
    when:
    Foreground foreground = null

    handlers {
      handler {
        foreground = context.foreground
        next()
      }
      register(new Thing(value: "1")) {
        handler {
          response.headers.set("L1", foreground.context.get(Thing).value)
          next()
        }
        register(new Thing(value: "2")) {
          handler {
            background {
              response.headers.set("L2", foreground.context.get(Thing).value)
            } then {
              foreground.context.next()
            }
          }
        }
        register(new Thing(value: "3")) {
          get {
            render foreground.context.get(Thing).value
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
