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

import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import org.pac4j.openid.credentials.OpenIdCredentials
import org.pac4j.openid.profile.google.GoogleOpenIdProfile
import ratpack.pac4j.InjectedPac4jModule
import ratpack.pac4j.Pac4jModule
import ratpack.pac4j.openid.pages.AuthPage
import ratpack.pac4j.openid.pages.ErrorPage
import ratpack.pac4j.openid.pages.NoAuthPage
import ratpack.test.embed.BaseDirBuilder
import ratpack.test.embed.PathBaseDirBuilder
import spock.lang.AutoCleanup
import spock.lang.Shared

/**
 * Tests OpenID Relying Party support.
 * Either install FireFox and run the "browserTest" target or configure the "GEB_SAUCE_LABS_USER" and
 * "GEB_SAUCE_LABS_ACCESS_PASSWORD" environment variables and run the "allSauceTests" target.
 */
class OpenIdRpSpec extends GebReportingSpec {
  private static final String EMAIL = "fake@example.com"

  @Shared
  @ClassRule
  TemporaryFolder temporaryFolder

  @Shared
  @AutoCleanup
  BaseDirBuilder baseDir

  @Shared
  RatpackOpenIdUnderTest autConstructed

  @Shared
  RatpackOpenIdUnderTest autInjected

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
    autConstructed = new RatpackOpenIdUnderTest(allocatePort(), baseDir, new Pac4jModule<>(new OpenIdTestClient(providerPort), new AuthPathAuthorizer<>()))
    autInjected = new RatpackOpenIdUnderTest(allocatePort(), baseDir, new InjectedPac4jModule<>(OpenIdCredentials, GoogleOpenIdProfile), new OpenIdTestModule(providerPort))
  }

  def cleanup() {
    if (browser) {
      browser.clearCookies()
    }
  }

  def cleanupSpec() {
    for (aut in [autConstructed, autInjected]) {
      if (aut) {
        aut.close()
      }
    }
  }

  def "test noauth"(RatpackOpenIdUnderTest aut) {
    setup:
    browser.baseUrl = aut.address.toString()

    when:
    to NoAuthPage

    then:
    body.text() == "noauth:null"

    where:
    aut << [autConstructed, autInjected]
  }

  def "test successful auth"(RatpackOpenIdUnderTest aut) {
    setup:
    browser.baseUrl = aut.address.toString()
    provider.addResult(true, EMAIL)

    when:
    go AuthPage.url

    then:
    at AuthPage
    body.text() == "auth:${EMAIL}"

    when:
    to NoAuthPage

    then:
    at NoAuthPage
    body.text() == "noauth:${EMAIL}"

    where:
    aut << [autConstructed, autInjected]
  }

  def "test failed auth"(RatpackOpenIdUnderTest aut) {
    setup:
    browser.baseUrl = aut.address.toString()
    provider.addResult(false, EMAIL)

    when:
    go AuthPage.url

    then:
    at ErrorPage
    body.text() == "An error was encountered."

    where:
    aut << [autConstructed, autInjected]
  }
}
