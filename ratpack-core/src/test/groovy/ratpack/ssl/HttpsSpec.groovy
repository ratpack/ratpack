/*
 * Copyright 2013 the original author or authors.
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

package ratpack.ssl

import org.junit.Rule
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.ssl.client.NonValidatingSSLClientContext

class HttpsSpec extends RatpackGroovyDslSpec {

  @Rule
  NonValidatingSSLClientContext clientContext = new NonValidatingSSLClientContext()

  def "can serve content over HTTPS"() {
    given:
    launchConfig {
      ssl SSLContexts.sslContext(HttpsSpec.getResource("dummy.keystore"), "password")
    }

    and:
    def staticFile = file "public/static.text", "hello!"

    and:
    handlers {
      assets("public")
      get {
        response.send "trust no one"
      }

      get("handler") {
        response.send staticFile.bytes
      }

      get("file") {
        render file("public/static.text")
      }
    }

    expect:
    def address = applicationUnderTest.address
    address.scheme == "https"

    and:
    address.toURL().text == "trust no one"

    and:
    URI handler = new URI(address.scheme,
      address.userInfo,
      address.host,
      address.port,
      "/handler",
      null,
      null)
    handler.toURL().text == "hello!"

    and:
    URI staticHandler = new URI(address.scheme,
      address.userInfo,
      address.host,
      address.port,
      "/file",
      null,
      null)
    staticHandler.toURL().text == "hello!"

    and:
    URI staticUri = new URI(address.scheme,
      address.userInfo,
      address.host,
      address.port,
      "/static.text",
      null,
      null)
    staticUri.toURL().text == "hello!"
  }

}


