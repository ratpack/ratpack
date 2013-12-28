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

package ratpack.http

import ratpack.test.internal.RatpackGroovyDslSpec

class CookiesSpec extends RatpackGroovyDslSpec {

  def "can get and set cookies"() {
    given:
    handlers {
      get("get/:name") {
        response.send request.oneCookie(pathTokens.name) ?: "null"
      }

      get("set/:name/:value") {
        response.cookie(pathTokens.name, pathTokens.value)
        response.send()
      }

      get("clear/:name") {
        response.expireCookie(pathTokens.name)
        response.send()
      }
    }

    when:
    getText("set/a/1")

    then:
    getText("get/a") == "1"

    when:
    getText("set/a/2")
    getText("set/b/1")

    then:
    getText("get/a") == "2"
    getText("get/b") == "1"

    when:
    getText("clear/a")

    then:
    getText("get/a") == "null"
  }

}
