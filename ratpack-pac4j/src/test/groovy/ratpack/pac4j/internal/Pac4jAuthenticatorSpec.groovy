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

package ratpack.pac4j.internal

import org.pac4j.core.exception.CredentialsException
import org.pac4j.http.client.indirect.IndirectBasicAuthClient
import org.pac4j.http.credentials.authenticator.UsernamePasswordAuthenticator
import org.pac4j.http.profile.HttpProfile
import org.pac4j.http.profile.creator.AuthenticatorProfileCreator
import ratpack.exec.Blocking
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.handling.Context
import ratpack.pac4j.RatpackPac4j
import ratpack.path.PathBinding
import ratpack.server.PublicAddress
import ratpack.server.internal.ConstantPublicAddress
import ratpack.session.SessionModule
import ratpack.test.exec.ExecHarness
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.pac4j.RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH

class Pac4jAuthenticatorSpec extends RatpackGroovyDslSpec {

  void "can create clients with callback url given [#path], [#boundTo] and [#uri]"() {
    given:
    def context = Mock(Context) {
      _ * get(PublicAddress) >> new ConstantPublicAddress(uri)
    }
    def pathBinding = Mock(PathBinding) {
      _ * getBoundTo() >> boundTo
    }

    and:
    def clientsProvider = Mock(RatpackPac4j.ClientsProvider) {
      _ * get(context) >> []
    }
    def authenticator = new Pac4jAuthenticator(path, clientsProvider)

    when:
    def actual = ExecHarness.yieldSingle { authenticator.createClients(context, pathBinding) }.valueOrThrow

    then:
    expected == actual.callbackUrl

    when:
    new URL(actual.callbackUrl)

    then:
    noExceptionThrown()

    where:
    path                       | uri                          | boundTo      | expected
    DEFAULT_AUTHENTICATOR_PATH | uri("http://some.host:1234") | ""           | "http://some.host:1234/$DEFAULT_AUTHENTICATOR_PATH"
    DEFAULT_AUTHENTICATOR_PATH | uri("http://some.host:1234") | "app"        | "http://some.host:1234/app/$DEFAULT_AUTHENTICATOR_PATH"
    DEFAULT_AUTHENTICATOR_PATH | uri("http://some.host:1234") | "app%2Fpath" | "http://some.host:1234/app%2Fpath/$DEFAULT_AUTHENTICATOR_PATH"
    "customAuthenticationPath" | uri("https://some.host")     | ""           | "https://some.host/customAuthenticationPath"
    "customAuthenticationPath" | uri("https://some.host")     | "app"        | "https://some.host/app/customAuthenticationPath"
  }

  void "authenticator should execute in blocking thread"() {
    given:
    def authenticator = { credentials ->
      credentials.userProfile = new HttpProfile(id: credentials.username)
      if (!Execution.isBlockingThread()) {
        throw new CredentialsException("!")
      }

      // Can use async API if necessary
      Blocking.on(Promise.async { down ->
        Thread.start { down.success(1) }
      })
    } as UsernamePasswordAuthenticator
    def client = new IndirectBasicAuthClient(authenticator, AuthenticatorProfileCreator.INSTANCE)

    and:
    bindings {
      module(SessionModule)
    }
    handlers {
      all(RatpackPac4j.authenticator(client))
      get("auth") {
        RatpackPac4j.login(context, IndirectBasicAuthClient).then {
          redirect "/"
        }
      }
      get {
        RatpackPac4j.userProfile(context)
          .route { o -> o.present } { render "ok" }
          .then { render "not ok" }
      }
    }

    when:
    def resp = requestSpec({ r -> r.basicAuth("u", "p") }).get("auth")

    then:
    resp.body.text == "ok"
  }

  private static URI uri(String url) {
    new URL(url).toURI()
  }

}
