/*
 * Copyright 2017 the original author or authors.
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

package ratpack.core.ssl

import com.google.common.base.Throwables
import io.netty.handler.codec.PrematureChannelClosureException
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.WriterAppender
import ratpack.core.server.internal.NettyHandlerAdapter
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Shared
import sun.security.provider.certpath.SunCertPathBuilderException

import javax.net.ssl.SNIHostName
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class HttpsSpec extends RatpackGroovyDslSpec {

  @Shared
  nonLocalHostCert = new SelfSignedCertificate("barbar")

  @Shared
  localHostCert = new SelfSignedCertificate("localhost")

  def "client requires verified domain name in cert - #valid"() {
    given:
    SslContext serverContext = SslContextBuilder.forServer(c.certificate(), c.privateKey())
      .build()

    SslContext clientContext = SslContextBuilder.forClient()
      .trustManager(c.cert())
      .build()

    serverConfig {
      ssl serverContext
    }

    when:
    handlers {
      get {
        render "ok"
      }
    }

    then:
    requestSpec { it.sslContext(clientContext) }

    if (valid) {
      assert text == "ok"
    } else {
      try {
        getText()
        throw new IllegalStateException("should have failed")
      } catch (UncheckedIOException e) {
        def root = Throwables.getRootCause(e)
        assert root instanceof CertificateException
        assert root.message == "No name matching localhost found"
      }
    }

    where:
    c                | valid
    nonLocalHostCert | false
    localHostCert    | true
  }

  def cleanupSpec() {
    nonLocalHostCert.delete()
    localHostCert.delete()
  }

  def "client requires trusted certificate"() {
    given:
    SslContext serverContext = SslContextBuilder.forServer(localHostCert.certificate(), localHostCert.privateKey())
      .build()

    serverConfig {
      ssl serverContext
    }

    when:
    handlers {
      get {
        render "ok"
      }
    }

    then:
    try {
      getText()
      throw new IllegalStateException("should have failed")
    } catch (UncheckedIOException e) {
      def root = Throwables.getRootCause(e)
      assert root instanceof SunCertPathBuilderException
      assert root.message == "unable to find valid certification path to requested target"
    }

  }

  def "can obtain clients ID"() {
    given:
    SslContext serverContext = SslContextBuilder.forServer(localHostCert.certificate(), localHostCert.privateKey())
      .trustManager(nonLocalHostCert.cert())
      .clientAuth(ClientAuth.REQUIRE)
      .build()

    SslContext clientContext = SslContextBuilder.forClient()
      .keyManager(nonLocalHostCert.key(), nonLocalHostCert.cert())
      .trustManager(localHostCert.cert())
      .build()

    serverConfig {
      ssl serverContext
    }

    when:
    handlers {
      get {
        X509Certificate cert = request.sslSession.get().peerCertificates[0]
        render cert.subjectDN.name
      }
    }

    then:
    requestSpec { it.sslContext(clientContext) }.text == nonLocalHostCert.cert().subjectDN.name
  }

  def "suppresses exceptions from non-HTTPS request"() {
    given:
    // Look up and modifying the underlying log4j configuration
    // so we can capture the log output in this test
    Logger logs = (Logger) LogManager.getLogger(NettyHandlerAdapter)
    def sw = new StringWriter()

    def builder = WriterAppender.newBuilder()
    builder.setTarget(sw)
    builder.setName("testCapture")
    def testAppender = builder.build()
    testAppender.start()

    logs.setAdditive(true)
    logs.addAppender(testAppender)

    SslContext localhostContext = SslContextBuilder.forServer(localHostCert.certificate(), localHostCert.privateKey()).build()
    serverConfig {
      ssl(localhostContext)
    }

    when:
    handlers {
      get {
        render "ok"
      }
    }

    then:
    try {
      getText("http://localhost:${applicationUnderTest.address.port}")
    } catch(PrematureChannelClosureException e) {
      assert e.getMessage().contains("closed the connection prematurely")
    }

    and:
    def lines = sw.toString().trim().split("\n")
    lines.size() == 1
    lines[0].startsWith("not an SSL/TLS record") || // This is the error message if using dynamic linking to OpenSSL
      lines[0].startsWith("error:1000009c:SSL routines:OPENSSL_internal:HTTP_REQUEST") // This is th error message if using static linking to BoringSSL

    cleanup:
    testAppender.stop()
    logs.removeAppender(testAppender)
    logs.setAdditive(false)
  }

  def "can serve multiple certificates using sni"() {
    given:
    def ratpackCert = new SelfSignedCertificate("*.ratpack.io")
    SslContext localhostContext = SslContextBuilder.forServer(localHostCert.certificate(), localHostCert.privateKey()).build()
    SslContext ratpackDomainContext = SslContextBuilder.forServer(ratpackCert.certificate(), ratpackCert.privateKey()).build()

    SslContext clientContext = SslContextBuilder.forClient()
      .trustManager(localHostCert.cert(), ratpackCert.cert())
      .build()

    serverConfig {
      ssl(localhostContext) { b ->
        b.add("*.ratpack.io", ratpackDomainContext)
      }
    }

    when:
    handlers {
      get {
        render "ok"
      }
    }

    then: "Requested using localhost with a trusted cert"
    requestSpec {
      it.sslContext(clientContext)
    }.getText()

    and: "Request fails if cert host isn't trusted"
    try {
      requestSpec {
        it.sslContext(clientContext)
      }.getText("https://127.0.0.1:${applicationUnderTest.address.port}/")
    } catch (UncheckedIOException e) {
      def root = Throwables.getRootCause(e)
      assert root instanceof CertificateException
      assert root.message == "No subject alternative names present"
    }

    and: "Request succeeds is provided a trusted alternative SNI name for the request"
    requestSpec {
      it.sslContext(clientContext)
      it.sslParams { p ->
        p.setServerNames([new SNIHostName("api.ratpack.io")])
      }
    }.getText("https://127.0.0.1:${applicationUnderTest.address.port}/")
  }
}
