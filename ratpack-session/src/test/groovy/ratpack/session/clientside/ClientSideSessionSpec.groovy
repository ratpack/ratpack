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

package ratpack.session.clientside

import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.MutableHeaders
import ratpack.http.client.RequestSpec
import ratpack.http.internal.HttpHeaderConstants
import ratpack.session.store.SessionStorage
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

class ClientSideSessionSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new ClientSideSessionsModule()
  }

  private String getSetCookie() {
    response.headers.get("Set-Cookie")
  }

  private String getSessionCookie() {
    cookies.find { it.name() == "ratpack_session" }?.value()
  }

  def getDecodedPairs() {
    def encoded = sessionCookie.split(":")[0]
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
    setCookie == null
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

    expect:
    get()
    response.body.text == "null"
    !sessionCookie
    !setCookie

    getText("set/foo") == "foo"
    decodedPairs.value == "foo"

  }

  @Unroll('key #key and value #value should be encoded')
  def "can handle keys/values that should be encoded"() {
    given:
    handlers {
      get { SessionStorage storage ->
        storage[key] = value
        response.send storage[key].toString()
      }
    }

    expect:
    get()
    response.body.text == value
    decodedPairs[key] == value

    where:
    key   | value
    'a'   | 'a'
    ':'   | ':'
    '='   | '='
    '/'   | '/'
    '\\'  | '\\'
    '&'   | ':'
    '&=:' | ':=&'

  }

  def "client should set-cookie only when session values have changed"() {
    given:
    handlers {

      handler { SessionStorage storage ->
        storage.size()
        next()
      }

      get("") { SessionStorage storage ->
        render storage.value.toString()
      }
      get("set/:value") { SessionStorage storage ->
        storage.value = pathTokens.value
        render storage.value.toString()
      }
    }

    expect:
    get("")
    response.body.text == "null"
    setCookie == null

    getText("set/foo")
    response.body.text == "foo"
    setCookie.startsWith("ratpack_session=")
    decodedPairs.value == "foo"

    getText("")
    response.body.text == "foo"
    setCookie == null
    decodedPairs.value == "foo"

    getText("set/foo")
    response.body.text == "foo"
    setCookie == null
    decodedPairs.value == "foo"

    getText("set/bar")
    response.body.text == "bar"
    setCookie.startsWith("ratpack_session=")
    decodedPairs.value == "bar"

    getText("set/bar")
    response.body.text == "bar"
    setCookie == null
    decodedPairs.value == "bar"

  }

  def "clearing an existing session informs client to expire cookie"() {
    given:
    handlers {
      get("") { SessionStorage storage ->
        render storage.value.toString()
      }
      get("set/:value") { SessionStorage storage ->
        storage.value = pathTokens.value
        render storage.value.toString()
      }
      get("clear") { SessionStorage storage ->
        storage.clear()
        response.status 200
      }
    }

    expect:
    get("set/foo")
    decodedPairs.value == "foo"

    get("clear")
    setCookie.startsWith("ratpack_session=; Expires=")
    !sessionCookie

    get("")
    !setCookie
    !sessionCookie
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
        headers.set(HttpHeaderConstants.COOKIE, "ratpack_session=${value}")
      }
    }

    when:
    get()

    then:
    response.body.text == "true"

    where:
    value << [null, '', ' ', '\t', 'foo', '::', ':', 'invalid:sequence', 'a:b:c']

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
        headers.set(HttpHeaderConstants.COOKIE, 'ratpack_session="dmFsdWU9Zm9v-DjZCDssly41x7tzrfXCaLvPuRAM="')
      }
    }

    when:
    get()

    then:
    response.body.text == "true"

  }

  def aut() {
    GroovyEmbeddedApp.build {
      bindings {
        add ClientSideSessionsModule, {
          it.with {
            secretKey = "secret"
            sessionName = "_sess"
            macAlgorithm = "HmacMD5"
          }
        }
      }
      handlers {
        get { SessionStorage storage ->
          response.send storage.value.toString()
        }
        get("set/:value") { SessionStorage storage ->
          storage.value = pathTokens.get("value")
          response.send storage.value.toString()
        }
      }
    }
  }

  def "cookies can be read across servers with the same secret"() {
    given:
    def app1 = aut()
    def client1 = app1.httpClient

    def app2 = aut()
    def client2 = app2.httpClient

    expect:
    client2.get("")
    client2.response.body.text == "null"
    !client2.response.headers.get("Set-Cookie")

    and:
    client1.get("set/foo")
    client1.response.body.text == "foo"
    client1.response.headers.get("Set-Cookie").startsWith("_sess")

    when:
    client2.requestSpec { RequestSpec spec ->
      spec.headers { MutableHeaders headers ->
        headers.set(HttpHeaderConstants.COOKIE, client1.response.headers.get("Set-Cookie"))
      }
    }

    then:
    client2.get("")
    client2.response.body.text == "foo"
    !client2.response.headers.get("Set-Cookie")

  }

}
