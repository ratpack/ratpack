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

package ratpack.handling

import groovy.transform.TupleConstructor
import ratpack.error.ServerErrorHandler
import ratpack.registry.NotInRegistryException
import ratpack.registry.Registry
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

class RegistryInsertionHandlerSpec extends RatpackGroovyDslSpec {

  def setup() {
    bindings {
      bindInstance ServerErrorHandler, new SimpleErrorHandler()
    }
  }

  interface Thing {
    String getValue()
  }

  @TupleConstructor
  static class ThingImpl implements Thing {
    @SuppressWarnings("GrFinalVariableAccess")
    final String value
  }

  def "can register for downstream with next"() {
    when:
    handlers {
      prefix("foo") {
        register {
          add Thing, new ThingImpl("foo")
        }
        get {
          render get(Thing).value
        }
        prefix("bar") {
          get {
            render get(Thing).value + ":bar"
          }
        }
      }
      get {
        get(Thing)
      }
    }

    then:
    getText("foo") == "foo"
    getText("foo/bar") == "foo:bar"
    getText().startsWith "$NotInRegistryException.name: No object for type '$Thing.name'"
    response.statusCode == 500
  }

  def "can use static register handler method that takes registry"() {
    when:
    handlers {
      register Registry.single(Thing, new ThingImpl("foo"))
      all {
        render get(Thing).value
      }
    }

    then:
    text == "foo"
  }

}
