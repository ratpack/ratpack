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
import ratpack.session.Session
import ratpack.session.SessionModule
import ratpack.session.SessionSpec
import spock.lang.Unroll

import java.time.Duration

class ClientSideSessionSpec extends SessionSpec {

  private String[] getCookies(String startsWith, String path) {
    getCookies(path).findAll { it.name.startsWith(startsWith)?.value } .toArray()
  }

  def setup() {
    modules << new ClientSideSessionModule()
    supportsSize = false
  }

  def "session cookies are bounded to path"() {
    given:
    modules.clear()
    bindings {
      module SessionModule
      module ClientSideSessionModule, {
        it.path = "/bar"
      }
    }
    handlers {
      get("foo") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
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
        session.data.then { it.set("foo", "bar"); render "ok" }
      }
      get("load") { Session session ->
        session.data.then { render it.get("foo").orElse("null")}
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
        session.data.then { it.set("foo", "bar"); render "ok"}
      }
      get("terminate") { Session session ->
        session.terminate().then { render "ok" }
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
        String value = ""
        for (int i = 0; i < 800; i++) {
          value += "ab"
        }
        session.data.then { it.set("foo", value); render "ok" }
      }
      get("bar") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok"}
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
      get("foo") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok"}
      }
      get("wait") { Session session ->
        Thread.sleep(1100)
        session.data.then { render it.get("foo").orElse("null") }
      }
    }

    when:
    get("foo")

    then:
    getCookies("ratpack_lat", "/").length == 1
    getCookies("ratpack_session", "/").length == 1

    when:
    get("wait")

    then:
    getCookies("ratpack_lat", "/").length == 0
    getCookies("ratpack_session", "/").length == 0
  }

  @Unroll
  def "a malformed cookie (#value) results in an empty session"() {
    given:
    handlers {
      get { Session session ->
        session.data.then { response.send(it.getKeys().isEmpty().toString())}
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
        session.data.then { response.send(it.getKeys().isEmpty().toString())}
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
        session.data.then { it.set("foo", "bar"); render "ok" }
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
      module ClientSideSessionModule, {
        it.httpOnly = false
      }
    }
    handlers {
      get("foo") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
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
      module ClientSideSessionModule, {
        it.secure = true
      }
    }
    handlers {
      get("foo") { Session session ->
        session.data.then { it.set("foo", "bar"); render "ok" }
      }
    }

    when:
    def values = get("foo").headers.getAll("Set-Cookie")

    then:
    values.findAll { it.contains("JSESSIONID") && it.contains("Secure") }.size() == 1
    values.findAll { it.contains("ratpack_lat") && it.contains("Secure") }.size() == 1
    values.findAll { it.contains("ratpack_session") && it.contains("Secure") }.size() == 1
  }
}
