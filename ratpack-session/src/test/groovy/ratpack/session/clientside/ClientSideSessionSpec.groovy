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

import ratpack.http.MutableHeaders
import ratpack.http.client.RequestSpec
import ratpack.http.internal.HttpHeaderConstants
import ratpack.server.ServerConfig
import ratpack.session.Session
import ratpack.session.SessionModule
import ratpack.session.SessionSpec
import spock.lang.Issue

import java.time.Duration

class ClientSideSessionSpec extends SessionSpec {

  private String[] getCookies(String startsWith, String path) {
    getCookies(path).findAll { it.name().startsWith(startsWith)?.value }.toArray()
  }

  final static SUPPORTED_ALGORITHMS = [
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

  static int keyLength(String algorithm) {
    switch (algorithm) {
      case ~/^DESede.*/:
        return 24
      case ~/^DES.*/:
        return 8
      default:
        return 16
    }
  }

  String key
  String token

  def setup() {
    modules << new ClientSideSessionModule() {
      @Override
      protected void defaultConfig(ServerConfig serverConfig, ClientSideSessionConfig config) {
        if (key != null) {
          config.setSecretKey(key)
        }
        if (token != null) {
          config.setSecretToken(token)
        }
      }
    }
    supportsSize = false
  }

  def "session cookies are bounded to path"() {
    given:
    modules.clear()
    bindings {
      module SessionModule, {
        it.path = "/bar"
      }
      module ClientSideSessionModule
    }
    handlers {
      get("foo") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    when:
    get("foo")

    then:
    getCookies("ratpack_session", "/").length == 0
    getCookies("ratpack_session", "/bar").length == 1
    getCookies("ratpack_session", "/foo").length == 0
  }

  def "last access time is set and changed on load or on store"() {
    when:
    handlers {
      get("nosessionaccess") {
        response.send("foo")
      }
      get("store") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
      get("load") { Session session ->
        render session.get("foo").map { it.orElse "null" }
      }
    }

    then:
    !get("nosessionaccess").headers.getAll("Set-Cookie").contains("ratpack_lat")
    def values = get("store").headers.getAll("Set-Cookie")
    def value = values.find { it.contains("ratpack_lat") }
    value
    def values2 = get("load").headers.getAll("Set-Cookie")
    def value2 = values2.find { it.contains("ratpack_lat") }
    value2 != value
  }

  def "invalidated session clears session cookies"() {
    given:
    handlers {
      get("foo") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
      get("terminate") { Session session ->
        render session.terminate().map { "ok" }
      }
    }

    when:
    get("foo")

    then:
    getCookies("ratpack_session", "/").length == 1

    when:
    get("terminate")

    then:
    getCookies("ratpack_session", "/").length == 0
  }

  def "session partioned by max cookie size"() {
    given:
    handlers {
      get("foo") { Session session ->
        String value = "ab" * 800
        render session.set("foo", value).map { "ok" }
      }
      get("bar") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    when:
    get("foo")

    then:
    getCookies("ratpack_session", "/").length == 2

    when:
    get("bar")

    then:
    getCookies("ratpack_session", "/").length == 1
  }

  def "timed out session invalidates cookies"() {
    given:
    modules.clear()
    bindings {
      module SessionModule
      module ClientSideSessionModule, {
        it.maxInactivityInterval = Duration.ofSeconds(1)
      }
    }
    handlers {
      get("set") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
      get("get") { Session session ->
        render session.get("foo").map { it.orElse("null") }
      }
    }

    when:
    get("set")

    then:
    getCookies("ratpack_lat", "/").length == 1
    getCookies("ratpack_session", "/").length == 1

    when:
    sleep(1100)
    get("get")

    then:
    getCookies("ratpack_lat", "/").length == 0
    getCookies("ratpack_session", "/").length == 0
  }

  def "a malformed cookie (#value) results in an empty session"() {
    given:
    handlers {
      get { Session session ->
        render session.keys.map { it.isEmpty().toString() }
      }
    }
    requestSpec { RequestSpec spec ->
      spec.headers { MutableHeaders headers ->
        headers.set(HttpHeaderConstants.COOKIE, "ratpack_session_0=${value}")
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
      get { Session session ->
        render session.keys.map { it.isEmpty().toString() }
      }
    }
    requestSpec { RequestSpec spec ->
      spec.headers { MutableHeaders headers ->
        headers.set(HttpHeaderConstants.COOKIE, 'ratpack_session_0="dmFsdWU9Zm9v:DjZCDssly41x7tzrfXCaLvPuRAM="')
      }
    }
    when:
    get()

    then:
    response.body.text == "true"
  }

  def "session cookies are all HTTPOnly"() {
    when:
    handlers {
      get("foo") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    then:
    def values = get("foo").headers.getAll("Set-Cookie")
    values.findAll { it.contains("JSESSIONID") && it.contains("HTTPOnly") }.size() == 1
    values.findAll { it.contains("ratpack_lat") && it.contains("HTTPOnly") }.size() == 1
    values.findAll { it.contains("ratpack_session") && it.contains("HTTPOnly") }.size() == 1
  }

  def "session cookies are not HTTPOnly, when config.isHttpOnly() is false"() {
    given:
    modules.clear()
    bindings {
      module SessionModule, {
        it.httpOnly = false
      }
      module ClientSideSessionModule
    }
    handlers {
      get("foo") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    when:
    def values = get("foo").headers.getAll("Set-Cookie")

    then:
    values.findAll { it.contains("JSESSIONID") && !it.contains("HTTPOnly") }.size() == 1
    values.findAll { it.contains("ratpack_lat") && !it.contains("HTTPOnly") }.size() == 1
    values.findAll { it.contains("ratpack_session") && !it.contains("HTTPOnly") }.size() == 1
  }

  def "session cookies are secure, when config.isSecure() is true"() {
    given:
    modules.clear()
    bindings {
      module SessionModule, {
        it.secure = true
      }
      module ClientSideSessionModule
    }
    handlers {
      get("foo") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    when:
    def values = get("foo").headers.getAll("Set-Cookie")

    then:
    values.findAll { it.contains("JSESSIONID") && it.contains("Secure") }.size() == 1
    values.findAll { it.contains("ratpack_lat") && it.contains("Secure") }.size() == 1
    values.findAll { it.contains("ratpack_session") && it.contains("Secure") }.size() == 1
  }

  def "can use algorithm #algorithm"() {
    given:
    modules.clear()
    bindings {
      module SessionModule
      module ClientSideSessionModule, {
        it.with {
          secretKey = "a" * keyLength(algorithm)
          cipherAlgorithm = algorithm
        }
      }
    }
    handlers {
      get { Session session ->
        render session.get("value").map { it.orElse("null") }
      }
      post("set/:value") { Session session ->
        render session.set("value", pathTokens.value).map { "ok" }
      }
    }

    expect:
    text == "null"
    postText("set/foo") == "ok"
    text == "foo"

    where:
    algorithm << SUPPORTED_ALGORITHMS
  }

  @Issue("This alogrithms seem to intermittently fail, so we'll be ok if they break")
  def "can use algorithm #algorithm - ok if fails"() {
    given:
    modules.clear()
    bindings {
      module SessionModule
      module ClientSideSessionModule, {
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
      get { Session session ->
        render session.get("value").map { it.orElse("null") }
      }
      post("set/:value") { Session session ->
        render session.set("value", pathTokens.value).map { "ok" }
      }
    }

    expect:
    try {
      text == "null"
      postText("set/foo") == "ok"
      text == "foo"
    } catch (Exception e) {
      e.printStackTrace()
    }

    where:
    algorithm << [
      "DESede/CBC/NoPadding",
    ]
  }

  def "changing the signing token invalidates the session"() {
    when:
    handlers {
      get { Session session ->
        render session.get("value").map { it.orElse("null") }
      }
      get("set/:value") { Session session ->
        session
          .set("value", pathTokens.value)
          .then {
          render pathTokens.value
        }
      }
    }

    then:
    getText("set/bar") == "bar"
    text == "bar"

    when:
    token = "abcdefghi"
    server.reload()

    then:
    text == "null"
  }

  def "changing the encryption key invalidates the session"() {
    when:
    key = "secretsecretsecr"
    handlers {
      get { Session session ->
        render session.get("value").flatMapError {
          session.terminate().flatMap { session.get("value") }
        }.map { it.orElse("null") }
      }
      get("set/:value") { Session session ->
        session
          .set("value", pathTokens.value)
          .then {
          render pathTokens.value
        }
      }
    }

    then:
    getText("set/bar") == "bar"
    text == "bar"

    when:
    key = "secretsecretsecx"
    server.reload()

    then:
    text == "null"
  }
}
