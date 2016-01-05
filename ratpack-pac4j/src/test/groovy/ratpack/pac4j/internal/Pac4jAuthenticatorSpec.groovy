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

import ratpack.handling.Context
import ratpack.pac4j.RatpackPac4j
import ratpack.path.PathBinding
import ratpack.server.PublicAddress
import ratpack.server.internal.ConstantPublicAddress
import spock.lang.Specification
import spock.lang.Unroll

import static ratpack.pac4j.RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH

class Pac4jAuthenticatorSpec extends Specification {

  @Unroll("can create clients with callback url given [#path], [#boundTo] and [#uri]")
  void "can create clients with callback url given context and path binding"() {
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
    def actual = authenticator.createClients(context, pathBinding)

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

  private static URI uri(String url) {
    new URL(url).toURI()
  }

}
