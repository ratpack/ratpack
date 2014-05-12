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

package ratpack.groovy.handling

import ratpack.test.internal.RatpackGroovyDslSpec

class ClosureParamInjectionSpec extends RatpackGroovyDslSpec {

  static class Thing {}

  def "can have global services"() {
    when:
    file "templates/foo.html", "bar"

    bindings {
      bind new Thing()
    }

    handlers { Thing thing ->
      get {
        render thing.class.name
      }
    }

    then:
    text == Thing.name
  }

  def "can inject request scoped objects"() {
    when:
    handlers {
      handler {
        request.register(new Thing())
        next()
      }
      get { Thing thing ->
        render thing.class.name
      }
    }

    then:
    text == Thing.name
  }

  def "context scope shadows request scope for handlers"() {
    when:
    bindings {
      bind "bar"
      bind new Thing()
    }

    handlers {
      handler {
        request.register("foo")
        next()
      }
      get { Thing thing, String string ->
        render "${thing.class.name} $string"
      }
    }

    then:
    text == "${Thing.name} bar"
  }

}
