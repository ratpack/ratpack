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

package ratpack.jackson

import ratpack.test.internal.RatpackGroovyDslSpec

import static Jackson.jsonNode

class JacksonParsingSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new JacksonModule()
  }

  def "can parse json node"() {
    when:
    app {
      handlers {
        post {
          def node = parse jsonNode()
          response.send node.get("value").toString()
        }
      }
    }

    and:
    request.contentType("application/json").body([value: 3])

    then:
    postText() == "3"
  }


}
