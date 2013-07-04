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

package org.ratpackframework.handling

import org.ratpackframework.groovy.handling.ClosureHandlers
import org.ratpackframework.test.DefaultRatpackSpec

class HandlersSpec extends DefaultRatpackSpec {

  def "empty chain handler"() {
    when:
    app {
      handlers {
        add Handlers.chain([])
      }
    }

    then:
    get().statusCode == 404
  }

  def "single chain handler"() {
    when:
    app {
      handlers {
        add Handlers.chain([ClosureHandlers.get { response.send("foo") }])
      }
    }

    then:
    text == "foo"
  }

  def "multi chain handler"() {
    when:
    app {
      handlers {
        add Handlers.chain([
            ClosureHandlers.get("a") { response.send("foo") },
            ClosureHandlers.get("b") { response.send("bar") }
        ])
      }
    }

    then:
    getText("a") == "foo"
    getText("b") == "bar"
  }
}
