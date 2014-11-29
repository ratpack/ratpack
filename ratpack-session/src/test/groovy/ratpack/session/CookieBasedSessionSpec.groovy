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

package ratpack.session

import ratpack.http.MutableHeaders
import ratpack.http.client.RequestSpec
import ratpack.session.store.CookieBasedSessionsModule
import ratpack.session.store.SessionStorage
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

class CookieBasedSessionSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new CookieBasedSessionsModule()
  }

  def getDecodedPairs(String rawCookie) {
    def parts = rawCookie.split("-")[0]
    def encoded = parts['ratpack_session="'.length()..<parts.length()]
    new String(Base64.getUrlDecoder().decode(encoded.getBytes("utf-8")))
      .split("&")
      .inject([:]) { m, kvp ->
        def p = kvp.split("=")
        m[urlDecode(p[0])] = urlDecode(p[1])
        m
      }
  }

  def urlDecode(String s) {
    URLDecoder.decode(s, "utf-8")
  }

  def "new session with no entries should not set cookie"() {
    given:
    handlers {
      get { SessionStorage storage ->
        assert storage.size() == 0
        response.send "ok"
      }
    }

    when:
    get()

    then:
    !response.headers.get("Set-Cookie")
  }

  def "can store session vars"() {
    given:
    handlers {
      get("") { SessionStorage storage ->
        render storage.value.toString()
      }
      get("set/:value") { SessionStorage storage ->
        storage.value = pathTokens.value
        render storage.value.toString()
      }
    }
    def session = [:]

    when:
    getText("set/foo") == "foo"
    session = getDecodedPairs(response.headers.get("Set-Cookie"))

    then:
    session.value == "foo"
  }

  def "client should set-cookie only when session values have changed"() {
    given:
    handlers {
      get("") { SessionStorage storage ->
        render storage.value.toString()
      }
      get("set/:value") { SessionStorage storage ->
        storage.value = pathTokens.value
        render storage.value.toString()
      }
    }
    def session = [:]

    when:
    getText("set/foo")
    session = getDecodedPairs(response.headers.get("Set-Cookie"))

    then:
    response.body.text == "foo"
    session.value == "foo"

    when:
    getText("")

    then:
    response.body.text == "foo"
    !response.headers.get("Set-Cookie")

    when:
    getText("set/foo")

    then:
    response.body.text == "foo"
    !response.headers.get("Set-Cookie")

    when:
    getText("set/bar")
    session = getDecodedPairs(response.headers.get("Set-Cookie"))

    then:
    response.body.text == "bar"
    session.value == "bar"

    when:
    getText("set/bar")

    then:
    response.body.text == "bar"
    !response.headers.get("Set-Cookie")

  }

  def "can handle values that should be encoded"() {
    given:
    def unsafeSequence = "=&%25/"
    handlers {
      get { SessionStorage storage ->
        storage.value = unsafeSequence
        response.send storage.value
      }
    }
    def session = [:]

    when:
    get()
    session = getDecodedPairs(response.headers.get("Set-Cookie"))

    then:
    session.value == unsafeSequence

  }

  def "clearing an existing session informs client to expire cookie"() {
    given:
    handlers {
      get { SessionStorage storage ->
        storage.value = "foo"
        response.send storage.value.toString()
      }
      get("clear") { SessionStorage storage ->
        storage.clear()
        response.send "clear"
      }
    }
    def session = [:]

    when:
    get()
    session = getDecodedPairs(response.headers.get("Set-Cookie"))

    then:
    session.value == "foo"

    when:
    get("clear")

    then:
    response.headers.get("Set-Cookie").startsWith("ratpack_session=; Expires=")
  }

  @Unroll
  def "a malformed cookie (#value) results in an empty session"() {
    given:
    handlers {
      get { SessionStorage storage ->
        response.send storage.isEmpty().toString()
      }
    }

    requestSpec { RequestSpec spec ->
      spec.headers { MutableHeaders headers ->
        headers.set("Cookie", "ratpack_session=${value}")
      }
    }

    when:
    get()

    then:
    response.body.text == "true"

    where:
    value << [null, '', ' ', '\t', 'foo', '--', '-', 'askldjfwel-asljdfl']

  }

  def "a cookie with bad digest results in empty session"() {
    given:
    handlers {
      get { SessionStorage storage ->
        response.send storage.isEmpty().toString()
      }
    }

    requestSpec { RequestSpec spec ->
      spec.headers { MutableHeaders headers ->
        headers.set("Cookie", 'ratpack_session="dmFsdWU9Zm9v-DjZCDssly41x7tzrfXCaLvPuRAM="')
      }
    }

    when:
    get()

    then:
    response.body.text == "true"

  }

}
