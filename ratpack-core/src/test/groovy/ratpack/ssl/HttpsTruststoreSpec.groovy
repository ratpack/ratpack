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

import ratpack.test.internal.RatpackGroovyDslSpec

import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException
import java.nio.channels.ClosedChannelException

class HttpsTruststoreSpec extends RatpackGroovyDslSpec {
  private def setupServerConfig(String keystore, String truststore) {
    serverConfig {
      if (keystore && truststore) {
        ssl SSLContexts.sslContext(
          HttpsTruststoreSpec.getResource(keystore), "password",
          HttpsTruststoreSpec.getResource(truststore), "password")
      } else if (keystore) {
        ssl SSLContexts.sslContext(HttpsTruststoreSpec.getResource(keystore), "password")
      }

      requireClientSslAuth true
    }
  }

  private def setupRequestSpec(String keystore, String truststore) {
    resetRequest()
    requestSpec {
      it.sslContext {
        if (keystore && truststore) {
          SSLContexts.sslContext(
            HttpsTruststoreSpec.getResource(keystore), "password",
            HttpsTruststoreSpec.getResource(truststore), "password")
        } else if (keystore) {
          SSLContexts.sslContext(HttpsTruststoreSpec.getResource(keystore), "password")
        }
      }
    }
  }

  private def setupHandlers() {
    handlers {
      get("foo") {
        render "SSL VERIFIED"
      }
    }
  }

  def "can serve content over HTTPS with client SSL authentication"() {
    given:
    setupServerConfig("server_dummy.keystore", "server_dummy.truststore")
    setupHandlers()

    when:
    setupRequestSpec("client_dummy.keystore", "client_dummy.truststore")

    then:
    def address = applicationUnderTest.address
    address.scheme == "https"
    getText("foo") == "SSL VERIFIED"
  }

  def "throw exception for [#clientKeystore, #clientTruststore, #serverKeystore, #serverTruststore]"() {
    given:
    setupServerConfig(serverKeystore, serverTruststore)
    setupHandlers()

    when:
    setupRequestSpec(clientKeystore, clientTruststore)
    get("foo")

    then:
    UncheckedIOException ex = thrown()
    ex.getCause() instanceof SSLHandshakeException || ex.getCause() instanceof ClosedChannelException || ex.getCause() instanceof SSLProtocolException || ex.getCause() instanceof SSLException


    where:
    clientKeystore          | clientTruststore          | serverKeystore          | serverTruststore
    "dummy.keystore"        | "client_dummy.truststore" | "server_dummy.keystore" | "server_dummy.truststore"
    "client_dummy.keystore" | "client_dummy.truststore" | "dummy.keystore"        | "server_dummy.truststore"
    "client_dummy.keystore" | "client_dummy.truststore" | "server_dummy.keystore" | null
  }
}
