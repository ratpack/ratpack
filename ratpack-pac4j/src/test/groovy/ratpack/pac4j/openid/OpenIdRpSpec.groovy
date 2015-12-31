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
import ratpack.groovy.handling.GroovyChain
import ratpack.http.client.RequestSpec
import ratpack.http.internal.HttpHeaderConstants
import ratpack.pac4j.RatpackPac4j
import ratpack.session.SessionModule
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.AutoCleanup
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED

/**
 * Tests OpenID Relying Party support.
 */
class OpenIdRpSpec extends RatpackGroovyDslSpec {
  private static final String EMAIL = "fake@example.com"

  @AutoCleanup
  EmbeddedProvider provider = new EmbeddedProvider()

  def setup() {
    provider.open()

    bindings {
      module SessionModule
    }

    handlers {
      prefix 'prefix', {
        configurePac4j delegate
      }

      configurePac4j delegate
    }

  }

  GroovyChain configurePac4j(GroovyChain chain) {
    chain.with {
      all(RatpackPac4j.authenticator(new OpenIdTestClient(provider.port)))
      get("noauth") {
        def typedUserProfile = maybeGet(YahooOpenIdProfile).orElse(null)
        def genericUserProfile = maybeGet(UserProfile).orElse(null)
        response.send "noauth:${typedUserProfile?.email}:${genericUserProfile?.attributes?.email}"
      }
      prefix("auth") {
        all(RatpackPac4j.requireAuth(OpenIdTestClient))
        get {
          def typedUserProfile = maybeGet(YahooOpenIdProfile).orElse(null)
          def genericUserProfile = maybeGet(UserProfile).orElse(null)
          response.send "auth:${typedUserProfile.email}:${genericUserProfile?.attributes?.email}"
        }
      }
    }

    chain
  }



  def "test noauth"() {
    when:
    def response = client.get("prefix/noauth")

    then:
    response.body.text == "noauth:null:null"
  }

  @Unroll('successful auth using prefix #prefix')
  def "test successful auth"() {
    setup:
    client.requestSpec { it.redirects(0) }
    provider.addResult(true, EMAIL)

    when:
    def response1 = client.get("${prefix}auth")

    then:
    response1.statusCode == FOUND.code()
    response1.headers.get(LOCATION).contains("/openid_provider")

    when:
    def response2 = client.get(response1.headers.get(LOCATION))

    then:
    response2.statusCode == FOUND.code()
    response2.headers.get(LOCATION).contains(RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH)

    when:
    client.resetRequest()
    client.requestSpec {
      it.headers.set("Cookie", response1.headers.getAll("Set-Cookie"))
      it.redirects(0)
    }
    def response3 = client.get(response2.headers.get(LOCATION))

    then:
    response3.statusCode == FOUND.code()
    response3.headers.get(LOCATION).contains("${prefix}auth")

    when:
    client.resetRequest()
    client.requestSpec {
      it.headers.set("Cookie", response1.headers.getAll("Set-Cookie"))
    }
    def response4 = client.get(response3.headers.get(LOCATION))

    then:
    response4.body.text == "auth:${EMAIL}:${EMAIL}"

    when:
    client.resetRequest()
    client.requestSpec {
      it.headers.set("Cookie", response1.headers.getAll("Set-Cookie"))
    }
    def response5 = client.get("${prefix}noauth")

    then:
    response5.body.text == "noauth:null:null"

    where:
    prefix << ['', 'prefix/']
  }

  def "it should not redirect ajax requests"() {
    setup:
    // Set AJAX request header.
    client.requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.add(
        HttpHeaderConstants.X_REQUESTED_WITH,
        HttpHeaderConstants.XML_HTTP_REQUEST
      )
    }

    when: "make a request that requires authentication"
    def response = client.get("auth")

    then: "a 401 response is returned and no redirect is made"
    assert response.statusCode == UNAUTHORIZED.code()
  }

}
