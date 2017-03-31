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

  private URI uriWithPath(URI uri, String path) {
    new URI(uri.scheme,
      uri.userInfo,
      uri.host,
      uri.port,
      path,
      null,
      null)
  }

  def "#path yields #responseBody"() {
    given:
    serverConfig {
      ssl SSLContexts.sslContext(HttpsSpec.getResource("dummy.keystore"), "password")
    }

    and:
    def staticFile = write "public/static.text", "trust no file"

    and:
    handlers {
      files { dir "public" }
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
    uriWithPath(address, path).toURL().text == responseBody

    where:
    path           | responseBody
    "/"            | "trust no one"
    "/handler"     | "trust no file"
    "/file"        | "trust no file"
    "/static.text" | "trust no file"

  }

}


