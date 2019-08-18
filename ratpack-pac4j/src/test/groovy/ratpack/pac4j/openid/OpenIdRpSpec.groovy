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

package ratpack.pac4j.openid

import org.pac4j.core.profile.UserProfile
import org.pac4j.openid.profile.yahoo.YahooOpenIdProfile
import ratpack.func.Action
import ratpack.groovy.handling.GroovyChainAction
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.pac4j.RatpackPac4j
import ratpack.session.SessionModule
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.AutoCleanup

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED
import static ratpack.http.internal.HttpHeaderConstants.*
import static ratpack.pac4j.RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH

/**
 * Tests OpenID Relying Party support.
 */
class OpenIdRpSpec extends RatpackGroovyDslSpec {

  private static final String EMAIL = "fake@example.com"
  private static final String AUTH_PATH = "auth"
  private static final String NOAUTH_PATH = "noauth"
  private static final String PREFIX_PATH = "prefix"
  private static final String PROVIDER_PATH = "/openid_provider"

  @AutoCleanup
  EmbeddedProvider provider = new EmbeddedProvider()

  def setup() {
    provider.open()

    bindings {
      module SessionModule
    }

    handlers {
      prefix PREFIX_PATH, new TestAuthenticationChainConfiguration()

      insert new TestAuthenticationChainConfiguration()
    }

  }

  final class TestAuthenticationChainConfiguration extends GroovyChainAction {

    void execute() {
      all(RatpackPac4j.authenticator(new OpenIdTestClient(provider.port)))
      get(NOAUTH_PATH) {
        def typedUserProfile = maybeGet(YahooOpenIdProfile).orElse(null)
        def genericUserProfile = maybeGet(UserProfile).orElse(null)
        response.send "noauth:${typedUserProfile?.email}:${genericUserProfile?.attributes?.email}"
      }
      prefix(AUTH_PATH) {
        all(RatpackPac4j.requireAuth(OpenIdTestClient))
        get {
          def typedUserProfile = maybeGet(YahooOpenIdProfile).orElse(null)
          def genericUserProfile = maybeGet(UserProfile).orElse(null)
          response.send "auth:${typedUserProfile.email}:${genericUserProfile?.attributes?.email}"
        }
      }
    }

  }

  def "test no auth given prefix path [#prefix]"() {
    when:
    def response = client.get("$prefix$NOAUTH_PATH")

    then:
    response.body.text == "noauth:null:null"

    where:
    prefix << ["", "$PREFIX_PATH/"]
  }

  def "successful auth using prefix #prefix"() {
    setup:
    reset { request -> noRedirects request }
    provider.addResult(true, EMAIL)

    when:
    def initialResponse = client.get("$prefix$AUTH_PATH")

    then:
    initialResponse.statusCode == FOUND.code()
    location(initialResponse).contains PROVIDER_PATH

    when:
    def providerResponse = client.get(location(initialResponse))

    then:
    providerResponse.statusCode == FOUND.code()
    location(providerResponse).contains DEFAULT_AUTHENTICATOR_PATH

    when:
    reset { request ->
      copyCookie initialResponse, request
      noRedirects request
    }
    def authenticatorResponse = client.get(location(providerResponse))

    then:
    authenticatorResponse.statusCode == FOUND.code()
    location(authenticatorResponse).contains("$prefix$AUTH_PATH")

    when:
    reset { request ->
      copyCookie initialResponse, request
    }
    def protectedResourceResponse = client.get(location(authenticatorResponse))

    then:
    protectedResourceResponse.body.text == "auth:${EMAIL}:${EMAIL}"

    when:
    reset { request ->
      copyCookie initialResponse, request
    }
    def unprotectedResourceResponse = client.get("$prefix$NOAUTH_PATH")

    then:
    unprotectedResourceResponse.body.text == "noauth:null:null"

    where:
    prefix << ["", "$PREFIX_PATH/"]
  }

  def "it should not redirect ajax requests"() {
    setup:
    ajaxRequest()

    when: "make a request that requires authentication"
    def response = client.get(AUTH_PATH)

    then: "a 401 response is returned and no redirect is made"
    response.statusCode == UNAUTHORIZED.code()
  }

  private void ajaxRequest() {
    client.requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.add(X_REQUESTED_WITH, XML_HTTP_REQUEST)
    }
  }

  private RequestSpec copyCookie(ReceivedResponse from, RequestSpec to) {
    to.headers.set COOKIE, from.headers.getAll(SET_COOKIE)
    to
  }

  private RequestSpec noRedirects(RequestSpec request) {
    request.redirects 0
    request
  }

  private String location(ReceivedResponse response) {
    response.headers.get LOCATION
  }

  private void reset(Action<? super RequestSpec> requestAction) {
    client.resetRequest()
    client.requestSpec requestAction
  }

}
