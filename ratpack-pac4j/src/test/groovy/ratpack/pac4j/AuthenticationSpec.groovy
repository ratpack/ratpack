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
import org.pac4j.core.profile.UserProfile
import org.pac4j.http.client.direct.DirectBasicAuthClient
import org.pac4j.http.client.direct.ParameterClient
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.http.client.indirect.IndirectBasicAuthClient
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.http.profile.HttpProfile
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
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

  def "secure handlers with direct auth"() {
    given:
    serverConfig {
      development true
    }
    bindings {
      module SessionModule
    }
    handlers {
      all RatpackPac4j.authenticator(new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator()))
      prefix('require-auth') {
        all RatpackPac4j.requireAuth(DirectBasicAuthClient)
        get {
          render "Hello " + get(UserProfile).getId()
        }
      }
      get {
        render "no auth required"
      }
    }
    httpClient.requestSpec { r ->
      r.redirects 0
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
        all(RatpackPac4j.requireAuth(FormClient, { ctx, p -> false } as Authorizer))
        get {
          def userProfile = maybeGet(UserProfile).orElse(null)
          response.send "notauthz:" + userProfile?.attributes?.username
        }
      }
      prefix("authz") {
        all(RatpackPac4j.requireAuth(FormClient, { ctx, p -> true } as Authorizer, { ctx, p -> true } as Authorizer))
        get {
          def userProfile = maybeGet(UserProfile).orElse(null)
          response.send "authz:" + userProfile?.attributes?.username
        }
      }
    }

    when: "request a page that requires authentication and no authorization"
    get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=foo&password=foo&client_name=FormClient")
    def resp1 = get("notauthz")

    then:
    resp1.statusCode == 403

    when:
    def resp2 = get("authz")

    then:
    resp2.statusCode == 200
  }

  def "check authorization with direct auth"() {
    given:
    serverConfig {
      development true
    }
    bindings {
      module SessionModule
    }
    handlers {
      all RatpackPac4j.authenticator(new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator()))
      prefix("allowed") {
        all RatpackPac4j.requireAuth(DirectBasicAuthClient, { ctx, p -> true } as Authorizer)
        get {
          render "Hello " + get(UserProfile).getId()
        }
      }
      prefix("forbidden") {
        all RatpackPac4j.requireAuth(DirectBasicAuthClient, { ctx, p -> false } as Authorizer)
        get {
          render "You're not allowed here " + get(UserProfile).getId()
        }
      }
    }
    httpClient.requestSpec { r ->
      r.redirects 0
    }

    expect:
    get('allowed').statusCode == 401
    requestSpec { r ->
      r.basicAuth('user', 'user')
    }.getText('allowed') == 'Hello user'

    and:
    resetRequest()
    get('forbidden').statusCode == 401
    requestSpec { r ->
      r.basicAuth('user', 'user')
    }.get('forbidden').statusCode == 403
  }

  def "request body with indirect auth"() {
    given:
    serverConfig {
      development true
    }
    bindings {
      module SessionModule
    }
    handlers {
      all RatpackPac4j.authenticator(new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator()))
      prefix("auth-post") {
        all RatpackPac4j.requireAuth(IndirectBasicAuthClient)
        get { ctx ->
          def user = get(UserProfile)
          ctx.request.body.then { body ->
            response.status(201)
            render user.id + " logged in"
          }
        }
      }
    }
    httpClient.requestSpec { r ->
      r.redirects 1
    }

    expect:
    requestSpec { r ->
      r.basicAuth("user", "user")
      r.body.type("text/plain").text("kthxbye")
    }.post("auth-post")

    response.statusCode == 201
    response.body.text == "user logged in"
  }

  def "request body with direct auth"() {
    given:
    serverConfig {
      development true
    }
    bindings {
      module SessionModule
    }
    handlers {
      all RatpackPac4j.authenticator(new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator()))
      prefix("auth-post") {
        all RatpackPac4j.requireAuth(DirectBasicAuthClient)
        post { ctx ->
          def user = get(UserProfile)
          ctx.request.body.then { body ->
            response.status(201)
            render user.getId() + " posted " + body.text
          }
        }
      }
    }
    httpClient.requestSpec { r ->
      r.redirects 0
    }

    expect:
    requestSpec { r ->
      r.basicAuth("user", "user")
      r.body.type("text/plain").text("kthxbye")
    }.post("auth-post")

    response.statusCode == 201
    response.body.text == "user posted kthxbye"
  }

  def "check authorization with jwt parameter"() {
    given:
    def secret = "12345678901234567890123456789012"
    def generator = new JwtGenerator(secret, false)
    def profile = new HttpProfile()
    profile.addAttribute("name", "user")
    def jwt = generator.generate(profile)
    def paramName = "jwt"

    serverConfig {
      development true
    }
    bindings {
      module SessionModule
    }
    handlers {
      def parameterClient = new ParameterClient(paramName, new JwtAuthenticator(secret))
      parameterClient.supportGetRequest = true

      all RatpackPac4j.authenticator(parameterClient)
      prefix('require-auth') {
        all RatpackPac4j.requireAuth(ParameterClient)
        get {
          render "Hello " + get(UserProfile).getAttribute("name")
        }
      }
      get {
        render "no auth required"
      }
    }
    httpClient.requestSpec { r ->
      r.redirects 0
    }

    expect:
    getText() == 'no auth required'
    get('require-auth').statusCode == 401
    params { it.put(paramName, jwt) }

    getText('require-auth') == 'Hello user'
  }
}
