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

import io.netty.handler.codec.http.QueryStringDecoder
import io.netty.util.CharsetUtil
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

  private String getSessionPayload() {
    sessionCookie?.split(":")?.getAt(0)
  }

  def getDecodedPairs() {
    new String(Base64.getUrlDecoder().decode(sessionPayload.getBytes("utf-8")))
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
        storage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("set/:value") { SessionStorage storage ->
        storage.set("value", pathTokens.value).then({

          storage.get("value", String).then({
            render it.orElse("null")
          })
        })
      }
    }

    when:
    get()

    then:
    response.body.text == "null"
    !sessionCookie
    !setCookie

    and:
    getText("set/foo") == "foo"
    decodedPairs.value == "foo"

  }

  @Unroll('key #key and value #value should be encoded')
  def "can handle keys/values that should be encoded"() {
    given:
    handlers {
      get { SessionStorage storage ->
        storage.set(key, value).then({

          storage.get(key, String).then({
            response.send it.orElse("null")
          })

        })

      }
    }

    expect:
    getText() == value
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
        next()
      }

      get("") { SessionStorage storage ->
        storage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("set/:value") { SessionStorage storage ->
        storage.set("value", pathTokens.value).then({

          storage.get("value", String).then({
            render it.orElse("null")
          })
        })
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
        storage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("set/:value") { SessionStorage storage ->
        storage.set("value", pathTokens.value).then({
          storage.get("value", String).then({
            render it.orElse("null")
          })
        })
      }
      get("clear") { SessionStorage storage ->
        storage.clear().then({
          render "OK"
        })
      }
    }

    when:
    get("set/foo")

    then:
    decodedPairs.value == "foo"

    when:
    get("clear")

    then:
    setCookie.startsWith("ratpack_session=; Max-Age=0; Expires=")
    !sessionCookie

    when:
    get("")

    then:
    !setCookie
    !sessionCookie
  }

  @Unroll
  def "a malformed cookie (#value) results in an empty session"() {
    given:
    handlers {
      get { SessionStorage storage ->
        storage.getKeys().then({ keys ->
          response.send(keys.isEmpty().toString())
        })

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
        storage.getKeys().then({ keys ->
          response.send(keys.isEmpty().toString())
        })
      }
    }

    requestSpec { RequestSpec spec ->
      spec.headers { MutableHeaders headers ->
        headers.set(HttpHeaderConstants.COOKIE, 'ratpack_session="dmFsdWU9Zm9v:DjZCDssly41x7tzrfXCaLvPuRAM="')
      }
    }

    when:
    get()

    then:
    response.body.text == "true"

  }

  def aut(Closure sessionModuleConfig) {
    GroovyEmbeddedApp.build {
      bindings {
        add ClientSideSessionsModule, {
          it.with sessionModuleConfig
        }
      }
      handlers {
        get { SessionStorage storage ->
          storage.get("value", String).then({
            render it.orElse("null")
          })
        }
        get("set/:value") { SessionStorage storage ->
          storage.set("value", pathTokens.value).then({

            storage.get("value", String).then({
              render it.orElse("null")
            })
          })
        }
      }
    }
  }

  @Unroll
  def "cookies can be read across servers with the same secret token and key"() {
    given:
    def app1 = aut(sessionModuleConfig)
    def client1 = app1.httpClient

    def app2 = aut(sessionModuleConfig)
    def client2 = app2.httpClient

    expect:
    client2.getText("") == "null"
    !client2.response.headers.get("Set-Cookie")

    and:
    client1.getText("set/foo") == "foo"
    client1.response.headers.get("Set-Cookie").startsWith("_sess")

    when:
    client2.requestSpec { RequestSpec spec ->
      spec.headers { MutableHeaders headers ->
        headers.set(HttpHeaderConstants.COOKIE, client1.response.headers.get("Set-Cookie"))
      }
    }

    then:
    client2.getText("") == "foo"
    !client2.response.headers.get("Set-Cookie")

    where:
    sessionModuleConfig << [{
                              secretToken = "secret"
                              sessionName = "_sess"
                              macAlgorithm = "HmacMD5"
                            }, {
                              secretToken = "secret"
                              secretKey = "a" * 16
                              sessionName = "_sess"
                              macAlgorithm = "HmacMD5"
                            }]

  }

  @Unroll
  def "sessions with value of length #length can be serialized/deserialized"() {
    given:
    modules.clear()
    bindings {
      add ClientSideSessionsModule, {
        it.secretKey = "a" * 16
      }
    }

    handlers {
      get("") { SessionStorage storage ->
        storage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("set/:value") { SessionStorage storage ->
        storage.set("value", pathTokens.value).then({
          storage.get("value", String).then({
            render it.orElse("null")
          })
        })
      }
    }

    expect:
    get()
    response.body.text == "null"
    !sessionCookie
    !setCookie

    def value = 'a' * length
    getText("set/$value") == value
    sessionPayload

    getText() == value

    where:
    length << [1, 3, 129, 255, 256]
  }

  @Unroll
  def "secretKey with #algorithm renders session unreadable"() {
    given:
    modules.clear()
    bindings {
      add ClientSideSessionsModule, {
        it.with {
          int length = 16
          switch (algorithm) {
            case ~/^AES.*/:
              length = 16
              break
            case ~/^DESede.*/:
              length = 24
              break
            case ~/^DES.*/:
              length = 8
              break
          }
          secretKey = "a" * length
          cipherAlgorithm = algorithm
        }
      }
    }

    handlers {
      get("") { SessionStorage storage ->
        storage.get("value", String).then({
          render it.orElse("null")
        })
      }
      get("set/:value") { SessionStorage storage ->
        storage.set("value", pathTokens.value).then({

          storage.get("value", String).then({
            render it.orElse("null")
          })
        })
      }
    }

    expect:
    get()
    response.body.text == "null"
    !sessionCookie
    !setCookie

    getText("set/foo") == "foo"
    sessionPayload
    String payload
    try {
      payload = new String(Base64.getUrlDecoder().decode(sessionPayload.bytes), CharsetUtil.UTF_8)
      QueryStringDecoder queryStringDecoder = new QueryStringDecoder(payload, CharsetUtil.UTF_8, false)
      queryStringDecoder.parameters().every { key, value ->
        key != "value" && value != "foo"
      }
    } catch (Exception e) {
    }

    getText() == "foo"

    where:
    algorithm << [
      "Blowfish",
      "AES/CBC/NoPadding",
      "AES/CBC/PKCS5Padding",
      "AES/ECB/NoPadding",
      "AES/ECB/PKCS5Padding",
      "DES/CBC/NoPadding",
      "DES/CBC/PKCS5Padding",
      "DES/ECB/NoPadding",
      "DES/ECB/PKCS5Padding",
      "DESede/CBC/NoPadding",
      "DESede/CBC/PKCS5Padding",
      "DESede/ECB/NoPadding",
      "DESede/ECB/PKCS5Padding"
    ]

  }

}
