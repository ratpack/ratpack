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

import com.google.inject.Module
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import org.pac4j.core.profile.UserProfile
import org.pac4j.openid.credentials.OpenIdCredentials
import org.pac4j.openid.profile.yahoo.YahooOpenIdProfile
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.client.RequestSpec
import ratpack.http.internal.HttpHeaderConstants
import ratpack.pac4j.InjectedPac4jModule
import ratpack.pac4j.Pac4jModule
import ratpack.session.SessionModule
import ratpack.session.store.MapSessionsModule
import ratpack.test.embed.BaseDirBuilder
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED
import static ratpack.pac4j.internal.AbstractPac4jModule.DEFAULT_CALLBACK_PATH

/**
 * Tests OpenID Relying Party support.
 */
class OpenIdRpSpec extends Specification {
  private static final String CUSTOM_CALLBACK_PATH = "custom-callback"
  private static final String EMAIL = "fake@example.com"

  @Shared
  @ClassRule
  TemporaryFolder temporaryFolder

  @Shared
  @AutoCleanup
  BaseDirBuilder baseDir

  @Shared
  @AutoCleanup
  EmbeddedApp autConstructed

  @Shared
  @AutoCleanup
  EmbeddedApp autInjected

  @Shared
  @AutoCleanup
  EmbeddedApp autCustom

  @Shared
  @AutoCleanup
  EmbeddedProvider provider

  def testApp(Module... additionalModules) {
    GroovyEmbeddedApp.build {
      baseDir(baseDir)

      bindings {
        additionalModules.each { module(it) }
        module SessionModule
        module new MapSessionsModule(10, 5)
      }

      handlers {
        get("noauth") {
          def typedUserProfile = request.maybeGet(YahooOpenIdProfile).orElse(null)
          def genericUserProfile = request.maybeGet(UserProfile).orElse(null)
          response.send "noauth:${typedUserProfile?.email}:${genericUserProfile?.attributes?.email}"
        }
        get("auth") {
          def typedUserProfile = request.maybeGet(YahooOpenIdProfile).orElse(null)
          def genericUserProfile = request.maybeGet(UserProfile).orElse(null)
          response.send "auth:${typedUserProfile.email}:${genericUserProfile?.attributes?.email}"
        }
      }
    }
  }

  def setupSpec() {
    provider = new EmbeddedProvider()
    provider.open()
    baseDir = BaseDirBuilder.tmpDir()
    autConstructed = testApp(new Pac4jModule<>(new OpenIdTestClient(provider.port), new AuthPathAuthorizer()))
    autInjected = testApp(new InjectedPac4jModule<>(OpenIdCredentials, YahooOpenIdProfile), new OpenIdTestModule(provider.port))
    autCustom = testApp(new Pac4jModule<>(new OpenIdTestClient(provider.port), new AuthPathAuthorizer()), new CustomConfigModule(CUSTOM_CALLBACK_PATH))
  }

  @Unroll
  def "test noauth"(EmbeddedApp aut) {
    setup:
    def client = aut.httpClient

    when: "request a page that doesn't require authentication"
    def response = client.get("noauth")

    then: "the page is returned without any redirects, and without authentication"
    response.body.text == "noauth:null:null"

    where:
    aut << [autConstructed, autInjected, autCustom]
  }

  @Unroll
  def "test successful auth"(EmbeddedApp aut, String expectedCallbackPath) {
    setup:
    def client = aut.httpClient
    client.requestSpec { it.redirects(0) }
    provider.addResult(true, EMAIL)

    when: "request a page that requires authentication"
    def response1 = client.get("auth")

    then: "the request is redirected to the openid provider"
    response1.statusCode == FOUND.code()
    response1.headers.get(LOCATION).contains("/openid_provider")

    when: "following the redirect"
    def response2 = client.get(response1.headers.get(LOCATION))

    then: "the response is redirected to the callback"
    response2.statusCode == FOUND.code()
    response2.headers.get(LOCATION).contains(expectedCallbackPath)

    when: "following the redirect"
    client.resetRequest()
    client.requestSpec {
      it.headers.set("Cookie", response1.headers.getAll("Set-Cookie"))
      it.redirects(0)
    }
    def response3 = client.get(response2.headers.get(LOCATION))

    then: "the response is redirected to the original page"
    response3.statusCode == FOUND.code()
    response3.headers.get(LOCATION).contains("/auth")

    when: "following the redirect"
    client.resetRequest()
    client.requestSpec {
      it.headers.set("Cookie", response1.headers.getAll("Set-Cookie"))
    }
    def response4 = client.get(response3.headers.get(LOCATION))

    then: "the original page is returned"
    response4.body.text == "auth:${EMAIL}:${EMAIL}"

    when: "request a page that doesn't require authentication after authenticating"
    client.resetRequest()
    client.requestSpec {
      it.headers.set("Cookie", response1.headers.getAll("Set-Cookie"))
    }
    def response5 = client.get("noauth")

    then: "authentication information is still available"
    response5.body.text == "noauth:${EMAIL}:${EMAIL}"

    where:
    aut            | expectedCallbackPath
    autConstructed | DEFAULT_CALLBACK_PATH
    autInjected    | DEFAULT_CALLBACK_PATH
    autCustom      | CUSTOM_CALLBACK_PATH
  }

  @Unroll
  def "it should not redirect ajax requests"(EmbeddedApp aut) {
    setup:
    def client = aut.httpClient

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

    where:
    aut << [autConstructed, autInjected, autCustom]
  }

}
