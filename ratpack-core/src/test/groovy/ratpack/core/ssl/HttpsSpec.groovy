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
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.PrematureChannelClosureException
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Shared
import sun.security.provider.certpath.SunCertPathBuilderException

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
      } catch (DecoderException e) {
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

    and:
    getText()

    then:
    def e = thrown(DecoderException)
    def root = Throwables.getRootCause(e)
    root instanceof SunCertPathBuilderException
    root.message == "unable to find valid certification path to requested target"
  }


  def "handles early ssl terminate"() {
    given:
    def serverSocket = new ServerSocket(0, 1)
    Thread.start {
      serverSocket.accept().close()
    }

    when:
    handlers {
      get {
        render "ok"
      }
    }

    and:
    getText("https://localhost:$serverSocket.localPort")

    then:
    def e = thrown(Exception)
    e instanceof PrematureChannelClosureException || Throwables.getRootCause(e) instanceof IOException
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
}
