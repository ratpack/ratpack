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

import com.google.inject.AbstractModule
import io.netty.handler.codec.http.cookie.Cookie
import ratpack.core.http.MutableHeaders
import ratpack.core.http.client.RequestSpec
import ratpack.core.http.internal.HttpHeaderConstants
import ratpack.core.server.ServerConfig
import ratpack.session.Session
import ratpack.session.SessionId
import ratpack.session.SessionModule
import ratpack.session.SessionSpec
import ratpack.session.clientside.internal.DefaultCrypto
import ratpack.exec.util.internal.InternalRatpackError

import java.time.Duration

class ClientSideSessionSpec extends SessionSpec {

  private List<Cookie> getCookies(String startsWith, String path) {
    getCookies(path).findAll { it.name() ==~ /${startsWith}_\d+/ }
  }

  final static SUPPORTED_ALGORITHMS = [
    "Blowfish",
    "AES/CBC/PKCS5Padding",
    "AES/ECB/PKCS5Padding",
    "DES/CBC/PKCS5Padding",
    "DES/ECB/PKCS5Padding",
    "DESede/CBC/PKCS5Padding",
    "DESede/ECB/PKCS5Padding"
  ]

  final static UNSUPPORTED_ALGORITHMS = [
    "AES/CBC/NoPadding",
    "AES/ECB/NoPadding",
    "DES/CBC/NoPadding",
    "DES/ECB/NoPadding",
    "DESede/CBC/NoPadding",
    "DESede/ECB/NoPadding"
  ]

  static int keyLength(String algorithm) {
    switch (algorithm) {
      case ~/^DESede.*/:
        return 24
      case ~/^DES.*/:
        return 8
      default: // also matches ~/^AES.*/
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
    getCookies("ratpack_session", "/").size() == 0
    getCookies("ratpack_session", "/bar").size() == 1
    getCookies("ratpack_session", "/foo").size() == 0
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
    !get("nosessionaccess").headers.getAll("Set-Cookie").contains("ratpack_session_lat")
    def values = get("store").headers.getAll("Set-Cookie")
    def value = values.find { it.contains("ratpack_session_lat") }
    value
    def values2 = get("load").headers.getAll("Set-Cookie")
    def value2 = values2.find { it.contains("ratpack_session_lat") }
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
    getCookies("ratpack_session", "/").size() == 1

    when:
    get("terminate")

    then:
    getCookies("ratpack_session", "/").size() == 0
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
    getCookies("ratpack_session", "/").size() == 2

    when:
    get("bar")

    then:
    getCookies("ratpack_session", "/").size() == 1
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
    getCookies("ratpack_session_lat", "/").size() == 1
    getCookies("ratpack_session", "/").size() == 1

    when:
    sleep(1100)
    get("get")

    then:
    getCookies("ratpack_session_lat", "/").size() == 0
    getCookies("ratpack_session", "/").size() == 0
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
    values.findAll { it.contains("ratpack_session_lat_0") && it.contains("HTTPOnly") }.size() == 1
    values.findAll { it.contains("ratpack_session_0") && it.contains("HTTPOnly") }.size() == 1
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
    values.findAll { it.contains("ratpack_session_lat_0") && !it.contains("HTTPOnly") }.size() == 1
    values.findAll { it.contains("ratpack_session_0") && !it.contains("HTTPOnly") }.size() == 1
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
    values.findAll { it.contains("ratpack_session_lat_0") && it.contains("Secure") }.size() == 1
    values.findAll { it.contains("ratpack_session_0") && it.contains("Secure") }.size() == 1
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

  def "can not use algorithm #algorithm"() {
    setup:
    modules.clear()

    when:
    def crypto = new DefaultCrypto("key".bytes, algorithm)

    then:
    thrown InternalRatpackError
    crypto == null

    where:
    algorithm << UNSUPPORTED_ALGORITHMS
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

  def "only send 1 last access time cookie"() {
    when:
    handlers {
      get { Session session ->
        session.get("test").nextOp {
          session.set("test", System.currentTimeMillis())
        }.then {
          render "ok"
        }
      }
    }

    then:
    getText() == "ok"
    def values = get().headers.getAll("Set-Cookie")
    values.findAll { it.contains("ratpack_session_lat") }.size() == 1
  }
def "accessing empty session doesn't cause lat cookie to be set"() {
    when:
    handlers {
      get("set") { Session session ->
        session.set("test", "foo").then { render "ok" }
      }
      get("empty") { Session session ->
        session.remove("test").then { render "ok" }
      }
      get("get") { Session session ->
        session.get("test").then { render "ok" }
      }
    }

    then:
    get("set")
    latCookies().size() == 1
    dataCookies().size() == 1
    get("empty")
    latCookies().size() == 0 // unset
    dataCookies().size() == 0
    get("get")
    latCookies().size() == 0
    dataCookies().size() == 0
  }

  private List<Cookie> latCookies() {
    getCookies("ratpack_session_lat", "/")
  }

  private List<Cookie> dataCookies() {
    getCookies("ratpack_session", "/")
  }
  def "can disable session ID"() {
    given:
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(SessionId).toInstance(SessionId.empty())
      }
    }

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
    def r = get("store")
    r.headers.getAll("Set-Cookie").size() == 2 // last access token and session
    getText("load") == "bar" // session still works
  }

  def "can set cookie name"() {
    given:
    modules.clear()
    bindings {
      module SessionModule
      module ClientSideSessionModule, {
        it.sessionCookieName = "foo"
      }
    }
    handlers {
      get("foo") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    when:
    get("foo")

    then:
    getCookies("foo", "/").size() == 1
    getCookies("foo_lat", "/").size() == 1
  }

  def "can set lat cookie name"() {
    given:
    modules.clear()
    bindings {
      module SessionModule
      module ClientSideSessionModule, {
        it.sessionCookieName = "foo"
        it.lastAccessTimeCookieName = "lat"
      }
    }
    handlers {
      get("foo") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    when:
    get("foo")

    then:
    getCookies("foo", "/").size() == 1
    getCookies("lat", "/").size() == 1
  }
}
