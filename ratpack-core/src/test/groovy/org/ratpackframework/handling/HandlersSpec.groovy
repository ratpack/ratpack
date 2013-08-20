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

import org.ratpackframework.error.ClientErrorHandler
import org.ratpackframework.error.ServerErrorHandler
import org.ratpackframework.file.FileSystemBinding
import org.ratpackframework.file.MimeTypes
import org.ratpackframework.groovy.handling.internal.ClosureBackedHandler
import org.ratpackframework.launch.LaunchConfig
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

import static org.ratpackframework.groovy.Util.asHandler
import static org.ratpackframework.handling.Handlers.chain

class HandlersSpec extends RatpackGroovyDslSpec {

  def "empty chain handler"() {
    when:
    app {
      handlers {
        chain([])
      }
    }

    then:
    get().statusCode == 404
  }

  def "single chain handler"() {
    when:
    app {
      handlers {
        handler chain(
          Handlers.get(asHandler { response.send("foo") })
        )
      }
    }

    then:
    text == "foo"
  }

  def "multi chain handler"() {
    when:
    app {
      handlers {
        handler chain(
          Handlers.get("a", asHandler { response.send("foo") }),
          Handlers.get("b", asHandler { response.send("bar") })
        )
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
          get(ServerErrorHandler)
          get(ClientErrorHandler)
          get(MimeTypes)
          get(LaunchConfig)
          get(FileSystemBinding)
          response.send "ok"
        }
      }
    }

    then:
    text == "ok"
  }
}
