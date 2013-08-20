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

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class HandlersSpec extends RatpackGroovyDslSpec {

  def "empty chain handler"() {
    when:
    app {
      handlers {
        Handlers.chain([])
      }
    }

    then:
    get().statusCode == 404
  }

  def "single chain handler"() {
    when:
    app {
      handlers {
        Handlers.chain([get { response.send("foo") }])
      }
    }

    then:
    text == "foo"
  }

  def "multi chain handler"() {
    when:
    app {
      handlers {
        Handlers.chain([
            get("a") { response.send("foo") },
            get("b") { response.send("bar") }
        ])
      }
    }

    then:
    getText("a") == "foo"
    getText("b") == "bar"
  }

  def "default services available"() {
    when:
    app {
      handlers {
        handler {
          get(org.ratpackframework.error.ServerErrorHandler)
          get(org.ratpackframework.error.ClientErrorHandler)
          get(org.ratpackframework.file.MimeTypes)
          get(org.ratpackframework.launch.LaunchConfig)
          get(org.ratpackframework.file.FileSystemBinding)
          response.send "ok"
        }
      }
    }

    then:
    text == "ok"
  }
}
