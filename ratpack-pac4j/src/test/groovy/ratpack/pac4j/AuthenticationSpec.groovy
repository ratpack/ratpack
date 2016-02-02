/*
 * Copyright 2016 the original author or authors.
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
package ratpack.pac4j

import org.pac4j.core.authorization.Authorizer
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.UserProfile
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.http.client.indirect.IndirectBasicAuthClient
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import ratpack.session.SessionModule
import ratpack.test.internal.RatpackGroovyDslSpec

class AuthenticationSpec extends RatpackGroovyDslSpec {

  def "secure handlers"() {
    given:
    serverConfig {
      development true
    }
    bindings {
      module SessionModule
    }
    handlers {
      all RatpackPac4j.authenticator(new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator()))
      prefix('require-auth') {
        all RatpackPac4j.requireAuth(IndirectBasicAuthClient)
        get {
          render "Hello " + get(UserProfile).getId()
        }
      }
      get {
        render "no auth required"
      }
    }
    httpClient.requestSpec { r ->
      r.redirects 1
    }

    expect:
    getText() == 'no auth required'
    get('require-auth').statusCode == 401
    requestSpec { r ->
      r.basicAuth('user', 'user')
    }.getText('require-auth') == 'Hello user'
  }

  def "check authorizations"() {
    given:
    bindings {
      module SessionModule
    }

    handlers {
      all(RatpackPac4j.authenticator(new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator())))
      prefix("notauthz") {
        all(RatpackPac4j.secure(FormClient, { WebContext context, UserProfile profile -> false } as Authorizer))
        get {
          def userProfile = maybeGet(UserProfile).orElse(null)
          response.send "notauthz:" + userProfile?.attributes?.username
        }
      }
      prefix("authz") {
        all(RatpackPac4j.secure(FormClient, { WebContext context, UserProfile profile -> true } as Authorizer,
          { WebContext context, UserProfile profile -> true } as Authorizer))
        get {
          def userProfile = maybeGet(UserProfile).orElse(null)
          response.send "authz:" + userProfile?.attributes?.username
        }
      }
    }

    when: "request a page that requires authentication and no authorization"
    get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=foo&password=foo&client_name=FormClient")
    def resp1 = get("notauthz")

    then: "the requested page is not authorized"
    resp1.statusCode == 403

    when: "request a page that requires authentication and authorization"
    def resp2 = get("authz")

    then: "the requested page is authorized"
    resp2.statusCode == 200
  }
}
