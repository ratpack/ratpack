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

package ratpack.pac4j.cookiesession

import org.pac4j.core.profile.UserProfile
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import ratpack.handling.RequestId
import ratpack.handling.RequestLogger
import ratpack.pac4j.RatpackPac4j
import ratpack.session.SessionModule
import ratpack.test.internal.RatpackGroovyDslSpec

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class Pac4jSessionSpec extends RatpackGroovyDslSpec {

  def "successful authorization"() {
    given:
    bindings {
      module SessionModule
    }

    handlers {
      all(RatpackPac4j.authenticator(new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator())))
      get("noauth") {
        def userProfile = maybeGet(UserProfile).orElse(null)
        response.send "noauth:" + userProfile?.attributes?.username
      }
      prefix("auth") {
        all(RatpackPac4j.requireAuth(FormClient))
        get {
          def userProfile = maybeGet(UserProfile).orElse(null)
          response.send "auth:" + userProfile?.attributes?.username
        }
      }
      get("login") {
        def userProfile = maybeGet(UserProfile).orElse(null)
        response.send "login:" + userProfile?.attributes?.username
      }
    }

    when: "request a page that requires authentication"
    requestSpec {
      it.redirects(0)
    }
    def resp1 = get("auth")

    then:
    resp1.statusCode == FOUND.code()
    resp1.headers.get(LOCATION).contains("/login")

    when:
    def resp2 = get(resp1.headers.get(LOCATION))

    then:
    resp2.statusCode == OK.code()
    resp2.body.text == "login:null"

    when:
    def resp3 = get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=foo&password=foo&client_name=FormClient")

    then:
    resp3.statusCode == FOUND.code()
    resp3.headers.get(LOCATION).contains("/auth")

    when:
    def resp4 = get(resp3.headers.get(LOCATION))

    then:
    resp4.statusCode == OK.code()
    resp4.body.text == "auth:foo"

    when:
    def resp5 = get("auth")

    then:
    resp5.statusCode == OK.code()
    resp5.body.text == "auth:foo"

    when:
    resetRequest()
    requestSpec {
      it.redirects(0)
    }
    def resp6 = get("auth")

    then:
    resp6.statusCode == FOUND.code()
    resp6.headers.get(LOCATION).contains("login")

    when:
    def resp7 = get(resp6.headers.get(LOCATION))
    resp7.statusCode == OK.code()
    resp7.body.text == "login:null"
    def resp8 = get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=bar&password=bar&client_name=FormClient")

    then:
    resp8.statusCode == FOUND.code()
    resp8.headers.get(LOCATION).contains("auth")

    when:
    def resp9 = get(resp8.headers.get(LOCATION))

    then:
    resp9.statusCode == OK.code()
    resp9.body.text == "auth:bar"
  }

  def "log user id in request log"() {
    given:
    def router = Mock(RequestLogger.Router)
    bindings {
      module SessionModule
    }
    handlers {
      all RequestLogger.ncsaFormat().to(router)
      all RatpackPac4j.authenticator(new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator()))
      prefix("foo") {
        all(RatpackPac4j.requireAuth(FormClient))
        get {
          render get(RequestId)
        }
      }
      get("login") {
        def userProfile = maybeGet(UserProfile).orElse(null)
        render "login:" + userProfile?.attributes?.username
      }
    }

    when:
    requestSpec {
      it.redirects(0)
    }
    def resp1 = get("foo")

    then:
    resp1.statusCode == FOUND.code()
    resp1.headers.get(LOCATION).contains("/login")

    when:
    def resp2 = get(resp1.headers.get(LOCATION))

    then:
    resp2.statusCode == OK.code()
    resp2.body.text == "login:null"

    when:
    def resp3 = get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=foo&password=foo&client_name=FormClient")

    then:
    resp3.statusCode == FOUND.code()
    resp3.headers.get(LOCATION).contains("/foo")

    when:
    def resp4 = get(resp3.headers.get(LOCATION))

    then:
    resp4.statusCode == OK.code()
    1 * router.log(_, _, { it[2] == "foo" })
  }
}
