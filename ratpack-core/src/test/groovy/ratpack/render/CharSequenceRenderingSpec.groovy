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

package ratpack.render

import ratpack.test.internal.RatpackGroovyDslSpec

class CharSequenceRenderingSpec extends RatpackGroovyDslSpec {

  def "can render string"() {
    when:
    handlers {
      get {
        render "foo"
      }
    }

    then:
    with(get()) {
      headers.get("content-type") == "text/plain;charset=UTF-8"
      body.text == "foo"
    }
  }

  def "can render gstring"() {
    when:
    handlers {
      get {
        render "${"foo"}"
      }
    }

    then:
    with(get()) {
      headers.get("content-type") == "text/plain;charset=UTF-8"
      body.text == "foo"
    }
  }

  def "can override content type"() {
    when:
    handlers {
      get {
        response.contentType("application/json")
        render "${"foo"}"
      }
    }

    then:
    with(get()) {
      headers.get("content-type") == "application/json"
      body.text == "foo"
    }
  }

}
