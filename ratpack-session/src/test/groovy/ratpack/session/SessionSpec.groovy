/*
 * Copyright 2015 the original author or authors.
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

import com.google.inject.AbstractModule
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.session.internal.JavaBuiltinSessionSerializer
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

class SessionSpec extends RatpackGroovyDslSpec {

  boolean supportsSize = true

  def setup() {
    modules << new SessionModule()
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(SimpleErrorHandler)
      }
    }
  }

  def "can use session"() {
    when:
    handlers {
      get { Session session ->
        session
          .set("foo", "bar")
          .then {
          render session.require("foo")
        }
      }
    }

    then:
    text == "bar"
  }

  def "session is available via request"() {
    when:
    handlers {
      get {
        request.get(Session)
          .set("foo", "bar")
          .then {
          render request.get(Session).require("foo")
        }
      }
    }

    then:
    text == "bar"
  }

  def "can store strings"() {
    when:
    handlers {
      get { Session session ->
        render session.require("value")
      }
      get("set/:value") { Session session ->
        session
          .set("value", pathTokens.value)
          .then {
          render pathTokens.value
        }
      }
    }

    and:
    getText("set/foo") == "false"

    then:
    getText() == "foo"
  }

  static class Holder1 implements Serializable {
    String value
  }

  static class Holder2 implements Serializable {
    String value
  }

  def "can store objects"() {
    when:
    handlers {
      get { Session session ->
        render session.require(Holder1).map { it.value }
      }
      get("set/:value") { Session session ->
        def value = pathTokens.value
        render session
          .set(new Holder1(value: value))
          .map {
          value
        }
      }
    }

    and:
    getText("set/foo") == "foo"

    then:
    getText() == "foo"
  }

  def "objects are differentiated"() {
    when:
    handlers {
      get { Session session ->
        session.data.then {
          it.set(new Holder1(value: "1"))
          it.set(new Holder2(value: "2"))
          render "ok"
        }
      }
      get("get") { Session session ->
        session.data.then {
          render it.require(Holder1).value + it.require(Holder2).value
        }
      }
    }

    and:
    get() // put data

    then:
    getText("get") == "12"
  }

  def "can invalidate session vars"() {
    if (!supportsSize) {
      return
    }

    when:
    handlers {
      get { Session session ->
        render session.get("value").map { it.orElse("null") }
      }
      get("set/:value") { Session session ->
        render session.set("value", pathTokens.value).map {
          pathTokens.value
        }
      }
      get("invalidate") { Session session ->
        render session.terminate().map { "ok" }
      }
      get("size") { SessionStore storeAdapter ->
        render storeAdapter.size().map { it.toString() }
      }
    }

    and:
    getText("set/foo")

    then:
    getText() == "foo"
    getText("size") == "1"
    getText("invalidate") == "ok"
    getText() == "null"
    getText("size") == "0"
  }

  def "sessions are created on demand"() {
    if (!supportsSize) {
      return
    }

    when:
    handlers {
      get { SessionStore store ->
        render store.size().map { it.toString() }
      }
      get("readOnly") { Session session ->
        render session.data.map { "ok" }
      }
      get("write") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    then:
    getText() == "0"
    getText("readOnly") == "ok"
    getText("write") == "ok"
    getText() == "1"
    getText("write") == "ok"
    getText() == "1"
  }

  def "session cookies are only set when needed"() {
    when:
    handlers {
      get("nowrite") {
        response.send("foo")
      }
      get("write") { Session session ->
        render session.set("foo", "bar").map { "ok" }
      }
    }

    then:
    get("nowrite").headers.get("Set-Cookie") == null
    get("write").headers.get("Set-Cookie").contains('JSESSIONID')

    // null because the session cookieSessionId is already set
    !get("write").headers.get("Set-Cookie")?.contains('JSESSIONID')
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
  }

  def "session cookies are not HTTPOnly"() {
    given:
    modules.clear()
    bindings {
      module SessionModule, {
        it.httpOnly = false
      }

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
  }

  def "session cookies are all Secure, can be transmitted via secure protocol"() {
    given:
    modules.clear()
    bindings {
      module SessionModule, {
        it.secure = true
      }
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
  }

  def "session is not available in a non request execution"() {
    when:
    handlers {
      get {
        Promise.async { d ->
          Execution.fork().onError { d.error(it) }.start {
            Execution.current().get(Session)
          }
        } then {
          render "ok"
        }
      }
    }

    then:
    text.contains "com.google.inject.OutOfScopeException: Cannot access Key[type=ratpack.session.Session, annotation=[none]] outside of a request"
  }

  def "deserialization errors are discarded and value nulled"() {
    when:
    def sessionSerializer = new SessionSerializer() {
      @Override
      def <T> void serialize(Class<T> type, T value, OutputStream out) throws Exception {
        throw new UnsupportedOperationException()
      }

      @Override
      def <T> T deserialize(Class<T> type, InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException()
      }
    }

    handlers {
      get("add") {
        get(Session).set("foo", "bar").then {
          render "ok"
        }
      }

      get("read") {
        get(Session).get("foo", sessionSerializer).then {
          render Objects.toString(it)
        }
      }

      get("keys") {
        get(Session).keys.then {
          render it.toString()
        }
      }
    }

    then:
    getText("add") == "ok"
    getText("keys") == "[SessionKey[name='foo', type=java.lang.String]]"
    getText("read") == "Optional.empty"
    getText("keys") == "[]"
  }

  def "error deserializing session causes session to be discarded"() {
    when:
    def error = false
    def sessionSerializer = new JavaBuiltinSessionSerializer() {
      @Override
      def <T> T deserialize(Class<T> type, InputStream inputStream) throws Exception {
        error ? { throw new UnsupportedOperationException() }() : super.deserialize(type, inputStream)
      }
    }

    bindings {
      module SessionModule
      bindInstance(SessionSerializer, sessionSerializer)
      bindInstance(JavaSessionSerializer, sessionSerializer)
    }
    handlers {
      get("add") {
        get(Session).set("foo", "bar").then {
          render "ok"
        }
      }

      get("read") {
        get(Session).get("foo").then {
          render Objects.toString(it)
        }
      }

      get("keys") {
        get(Session).keys.then {
          render it.toString()
        }
      }
    }

    then:
    getText("add") == "ok"

    when:
    error = true

    then:
    getText("keys") == "[]"

    when:
    error = false

    then:
    getText("keys") == "[]"
  }
}
