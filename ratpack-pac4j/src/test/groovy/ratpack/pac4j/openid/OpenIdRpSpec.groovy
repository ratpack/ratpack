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

import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import org.pac4j.openid.credentials.OpenIdCredentials
import org.pac4j.openid.profile.google.GoogleOpenIdProfile
import ratpack.http.client.RequestSpec
import ratpack.pac4j.InjectedPac4jModule
import ratpack.pac4j.Pac4jModule
import ratpack.test.ApplicationUnderTest
import ratpack.test.embed.BaseDirBuilder
import ratpack.test.embed.PathBaseDirBuilder
import ratpack.test.http.TestHttpClient
import ratpack.test.http.TestHttpClients
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND

/**
 * Tests OpenID Relying Party support.
 */
class OpenIdRpSpec extends Specification {
  private static final String EMAIL = "fake@example.com"

  @Shared
  @ClassRule
  TemporaryFolder temporaryFolder

  @Shared
  @AutoCleanup
  BaseDirBuilder baseDir

  @Shared
  RatpackOpenIdTestApplication autConstructed

  @Shared
  RatpackOpenIdTestApplication autInjected

  @Shared
  @AutoCleanup
  EmbeddedProvider provider

  static int allocatePort() {
    def socket = new ServerSocket(0)
    try {
      return socket.localPort
    } finally {
      socket.close()
    }
  }

  def setupSpec() {
    def providerPort = allocatePort()
    provider = new EmbeddedProvider()
    provider.open(providerPort)
    baseDir = new PathBaseDirBuilder(temporaryFolder.newFolder("app"))
    autConstructed = new RatpackOpenIdTestApplication(allocatePort(), baseDir, new Pac4jModule<>(new OpenIdTestClient(providerPort), new AuthPathAuthorizer()))
    autInjected = new RatpackOpenIdTestApplication(allocatePort(), baseDir, new InjectedPac4jModule<>(OpenIdCredentials, GoogleOpenIdProfile), new OpenIdTestModule(providerPort))
  }

  def cleanupSpec() {
    for (aut in [autConstructed, autInjected]) {
      if (aut) {
        aut.close()
      }
    }
  }

  def cleanup() {
    provider.clear()
  }

  @Unroll
  def "test noauth"(RatpackOpenIdTestApplication aut) {
    setup:
    def client = newClient(aut)

    when: "request a page that doesn't require authentication"
    def response = client.get("noauth")

    then: "the page is returned without any redirects, and without authentication"
    response.body.text == "noauth:null:null"

    where:
    aut << [autConstructed, autInjected]
  }

  @Unroll
  def "test successful auth"(RatpackOpenIdTestApplication aut) {
    setup:
    def client = newClient(aut)
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
    response2.headers.get(LOCATION).contains("/pac4j-callback")

    when: "following the redirect"
    client.resetRequest()
    client.requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Cookie",response1.headers.getAll("Set-Cookie"))
    }
    def response3 = client.get(response2.headers.get(LOCATION))

    then: "the response is redirected to the original page"
    response3.statusCode == FOUND.code()
    response3.headers.get(LOCATION).contains("/auth")

    when: "following the redirect"
    client.resetRequest()
    client.requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Cookie",response1.headers.getAll("Set-Cookie"))
    }
    def response4 = client.get(response3.headers.get(LOCATION))

    then: "the original page is returned"
    response4.body.text == "auth:${EMAIL}:${EMAIL}"

    when: "request a page that doesn't require authentication after authenticating"
    client.resetRequest()
    client.requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Cookie",response1.headers.getAll("Set-Cookie"))
    }
    def response5 = client.get("noauth")

    then: "authentication information is still available"
    response5.body.text == "noauth:${EMAIL}:${EMAIL}"

    where:
    aut << [autConstructed, autInjected]
  }

  private static TestHttpClient newClient(ApplicationUnderTest aut) {
    return TestHttpClients.testHttpClient(aut)
  }
}
