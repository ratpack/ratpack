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

package ratpack.ssl

import com.google.common.base.Throwables
import io.netty.handler.ssl.JdkSslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.util.SelfSignedCertificate
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Shared
import sun.security.provider.certpath.SunCertPathBuilderException

import java.security.cert.CertificateException

class HttpsSpec extends RatpackGroovyDslSpec {

  @Shared
    badDomainCert = new SelfSignedCertificate("barbar")
  @Shared
    localHostCert = new SelfSignedCertificate("localhost")

  def "client requires verified domain name in cert - #valid"() {
    given:
    JdkSslContext serverContext = SslContextBuilder.forServer(c.certificate(), c.privateKey())
      .sslProvider(SslProvider.JDK)
      .build() as JdkSslContext

    JdkSslContext clientContext = SslContextBuilder.forClient()
      .trustManager(c.cert())
      .sslProvider(SslProvider.JDK)
      .build() as JdkSslContext

    serverConfig {
      ssl serverContext.context()
    }

    when:
    handlers {
      get {
        render "ok"
      }
    }

    then:
    requestSpec { it.sslContext(clientContext.context()) }

    if (valid) {
      assert text == "ok"
    } else {
      try {
        getText()
      } catch (UncheckedIOException e) {
        def root = Throwables.getRootCause(e)
        assert root instanceof CertificateException
        root.message == "No name matching localhost found"
      }
    }

    where:
    c             | valid
    badDomainCert | false
    localHostCert | true
  }

  def cleanupSpec() {
    badDomainCert.delete()
    localHostCert.delete()
  }

  def "client requires trusted certificate"() {
    given:
    JdkSslContext serverContext = SslContextBuilder.forServer(localHostCert.certificate(), localHostCert.privateKey())
      .sslProvider(SslProvider.JDK)
      .build() as JdkSslContext

    serverConfig {
      ssl serverContext.context()
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
    } catch (UncheckedIOException e) {
      def root = Throwables.getRootCause(e)
      assert root instanceof SunCertPathBuilderException
      root.message == "unable to find valid certification path to requested target"
    }

  }

}
