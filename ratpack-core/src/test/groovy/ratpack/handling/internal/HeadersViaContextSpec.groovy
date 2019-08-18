/*
 * Copyright 2016 the original author or authors.
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

package ratpack.handling.internal

import ratpack.test.internal.RatpackGroovyDslSpec

class HeadersViaContextSpec extends RatpackGroovyDslSpec {

  def "can read header via context"() {
    when:
    handlers {
      get {
        render header("foo").get()
      }
    }

    then:
    request { it.headers.set("foo", "bar") }.body.text == "bar"
  }

  def "can set header via context"() {
    when:
    handlers {
      get {
        header("foo", "bar", "baz").render("ok")
      }
    }

    then:
    get().headers.getAll("foo") == ["bar", "baz"]
  }

}
