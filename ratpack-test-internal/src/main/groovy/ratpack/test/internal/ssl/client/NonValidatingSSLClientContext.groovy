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

package ratpack.test.internal.ssl.client

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import javax.net.ssl.*
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate

/**
 * A rule that overrides the client SSL groovyContext with one that does not validate certificates or hostnames.
 * That means that any HTTPS connections made by the test are unverified and will work against HTTPS servers
 * running on localhost and/or with invalid certificates.
 */
class NonValidatingSSLClientContext implements TestRule {

  @Override
  Statement apply(Statement base, Description description) {
    { ->
      final originalSocketFactoryProvider = Security.getProperty("ssl.SocketFactory.provider")
      final originalHostnameVerifier = HttpsURLConnection.defaultHostnameVerifier
      Security.setProperty "ssl.SocketFactory.provider", DummySSLSocketFactory.name
      HttpsURLConnection.defaultHostnameVerifier = ALLOW_ALL_HOSTNAME_VERIFIER
      try {
        base.evaluate()
      } finally {
        Security.setProperty "ssl.SocketFactory.provider", originalSocketFactoryProvider ?: ""
        HttpsURLConnection.defaultHostnameVerifier = originalHostnameVerifier
      }
    } as Statement
  }

  static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = new HostnameVerifier() {
    @Override
    boolean verify(String s, SSLSession sslSession) {
      return true
    }
  }

  static final TrustManager TRUST_ALL_TRUST_MANAGER = new X509TrustManager() {
    @Override
    void checkClientTrusted(X509Certificate[] chain, String authType) {}

    @Override
    void checkServerTrusted(X509Certificate[] chain, String authType) {}

    @Override
    X509Certificate[] getAcceptedIssuers() {
      [] as X509Certificate[]
    }
  }

  static class DummySSLSocketFactory extends SSLSocketFactory {

    private final SSLContext sslContext = SSLContext.getInstance("TLS")
    private final SSLSocketFactory factory

    DummySSLSocketFactory() {
      sslContext.init(null, [TRUST_ALL_TRUST_MANAGER] as TrustManager[], new SecureRandom())
      factory = sslContext.socketFactory
    }

    @Override
    Socket createSocket() {
      factory.createSocket()
    }

    @Override
    Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
      factory.createSocket address, port, localAddress, localPort
    }

    @Override
    Socket createSocket(InetAddress host, int port) {
      factory.createSocket host, port
    }

    @Override
    Socket createSocket(Socket s, String host, int port, boolean autoClose) {
      factory.createSocket(s, host, port, autoClose)
    }

    @Override
    Socket createSocket(String host, int port, InetAddress localHost, int localPort) {
      factory.createSocket(host, port, localHost, localPort)
    }

    @Override
    Socket createSocket(String host, int port) {
      factory.createSocket(host, port)
    }

    @Override
    String[] getDefaultCipherSuites() {
      factory.defaultCipherSuites
    }

    @Override
    String[] getSupportedCipherSuites() {
      factory.supportedCipherSuites
    }
  }
}
