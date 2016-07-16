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

package ratpack.groovy.test.embed

import spock.lang.Specification

class GroovyEmbeddedAppSpec extends Specification {

  def "embedded app without base dir"() {
    expect:
    GroovyEmbeddedApp.of {
      handlers {
        all {
          render "foo"
        }
      }
    } test {
      assert it.text == "foo"
    }
  }

  def "embedded app using ratpack.groovy syntax"() {
    expect:
    GroovyEmbeddedApp.ratpack {
      bindings {
        bindInstance String, "foo"
      }
      handlers {
        all {
          render get(String)
        }
      }
    } test {
      assert it.text == "foo"
    }
  }

}
