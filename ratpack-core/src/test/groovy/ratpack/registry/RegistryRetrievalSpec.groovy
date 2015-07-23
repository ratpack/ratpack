/*
 * Copyright 2015 the original author or authors.
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

package ratpack.registry

import ratpack.exec.Execution
import ratpack.test.internal.RatpackGroovyDslSpec

class RegistryRetrievalSpec extends RatpackGroovyDslSpec {

  def "Registry should be LIFO"() {
    given:
    bindings {
      add "3"
    }
    handlers {
      all {
        next(Registry.single("2"))
      }
      all {
        next(Registry.single("1"))
      }
      all {
        render getAll(String).join(', ')
      }
    }

    expect:
    text == "1, 2, 3"
  }

  def "Request registry and Execution registry are the same"() {
    given:
    handlers {
      all {
        request.add("request 2")
        next()
      }
      all {
        request.add("request 1")
        next()
      }
      all {
        render Execution.current().getAll(String).join(', ')
      }
    }

    expect:
    text == "request 1, request 2"
  }

  def "Context should check request registry first"() {
    given:
    bindings {
      add "3"
    }
    handlers {
      all {
        request.add("request 2")
        next()
      }
      all {
        next(Registry.single("2"))
      }
      all {
        request.add("request 1")
        next()
      }
      all {
        next(Registry.single("1"))
      }
      get {
        render getAll(String).join(', ')
      }
    }

    expect:
    text == "request 1, request 2, 1, 2, 3"
  }

}
