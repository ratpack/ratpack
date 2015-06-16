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

import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.IgnoreRest

import javax.net.ssl.SSLProtocolException
import java.nio.channels.ClosedChannelException

class HttpsTruststoreSpec extends RatpackGroovyDslSpec {

  def "can serve content over HTTPS with client SSL authentication"() {
    given:
    serverConfig {
      ssl SSLContexts.sslContext(
        HttpsTruststoreSpec.getResource("server_dummy.keystore"), "password",
        HttpsTruststoreSpec.getResource("server_dummy.truststore"), "password")
    }
    file "public/static.text", "SSL VERIFIED"

    handlers {
      files {
        dir "public"
      }
      get("file") {
        render file("public/static.text")
      }
    }

    when:
    requestSpec { RequestSpec rs ->
      rs.sslContext {
        return SSLContexts.sslContext(
          HttpsTruststoreSpec.getResource("dummy.keystore"), "password",
          HttpsTruststoreSpec.getResource("client_dummy.truststore"), "password")
      }
    }

    then:
    def address = applicationUnderTest.address
    address.scheme == "https"
    getText("file") == "SSL VERIFIED"
  }

  @IgnoreRest
  def "throw handshake exception when client provides no or wrong certificate"() {
    given:
    serverConfig {
      ssl SSLContexts.sslContext(
        HttpsTruststoreSpec.getResource("server_dummy.keystore"), "password",
        HttpsTruststoreSpec.getResource("server_dummy.truststore"), "password")
      sslClientAuth true  // require client ssl authentication
    }
    file "public/static.text", "SSL VERIFIED"

    handlers {
      files {
        dir "public"
      }
      get("file") {
        render file("public/static.text")
      }
    }

    when: "client does not provide valid certificate"
    requestSpec { RequestSpec rs ->
      rs.sslContext {
        return SSLContexts.sslContext(
          HttpsTruststoreSpec.getResource("dummy.keystore"), "password",
          HttpsTruststoreSpec.getResource("client_dummy.truststore"), "password")
      }
    }
    get("file")

    then:
    UncheckedIOException ex = thrown()
    ex.getCause() instanceof SSLProtocolException || ex.getCause() instanceof ClosedChannelException

  }
}
