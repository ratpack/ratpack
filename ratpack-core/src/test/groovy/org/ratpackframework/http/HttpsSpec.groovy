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

package org.ratpackframework.http

import groovy.transform.CompileStatic
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.ratpackframework.test.internal.RatpackGroovyDslSpec

import javax.net.ssl.*
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate

class HttpsSpec extends RatpackGroovyDslSpec {

  @Rule
  DummySSLContext clientContext = new DummySSLContext()

  /**
   * This is a keystore with a self-signed certificate.
   */
  private static
  final String KEYSTORE = "/u3+7QAAAAIAAAABAAAAAQABMQAAATLccCWGAAABmTCCAZUwDgYKKwYBBAEqAhEBAQUABIIBgRRJBpHdCovt86mrLk8fwiBo02r5dt3cgbdN5XSuOzZIpHelji7YUbP3qYkGr87KCv+QLfWfxxoUAp0wuWJsHvBRNhHn9mQLE3URyzwlR5a+sBFrf0TFc1OrS0Kb4pFBjqfQ60MjGURVuhd6dvc1U2q88zsUgtlO0IhnzwjBWuOOugBV5JyTTsIiy04S3K6kO7TCRGliENniTgBZpBuRvJVTd3144o1c/x31QeanY5xb9Q4Cm2QqyzQ/mWH7klUyQD9S3/r+oouTfZOoWYPgvSIw1hhSj/OMV9ukqECHTb7yOFdOZ0j154inPTolJCSoEKo3/gkqHx1YCeUBCbchKRX6IMyT5kFLXUqZfRv+ZCu/GY+26B2XMDm2xUUNoJmlcs+X6yY8Nur+VFf/PqVJ8RXz7yAgLsQY3FsoztwJM0cLH7j6OZPerfuK2HVs1+m+luzWrHjU3SD+BdSkwbY45lKVTJ0J6iZSVpNHH5rR22/o9SCtFy5UMXLXUpou5j9twIsAAAABAAVYLjUwOQAAAmcwggJjMIICDaADAgECAgkAmA98MmCqaaAwDQYJKoZIhvcNAQEFBQAwVzELMAkGA1UEBhMCR0IxEzARBgNVBAgTClNvbWUtU3RhdGUxDzANBgNVBAcTBkxvbmRvbjEQMA4GA1UEChMHQmV0YW1heDEQMA4GA1UEAxMHQmV0YW1heDAeFw0xMTEwMDcwMzMwMjZaFw0xMTExMDYwMzMwMjZaMFcxCzAJBgNVBAYTAkdCMRMwEQYDVQQIEwpTb21lLVN0YXRlMQ8wDQYDVQQHEwZMb25kb24xEDAOBgNVBAoTB0JldGFtYXgxEDAOBgNVBAMTB0JldGFtYXgwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAzI6qkBB90NRSsZreTazmrr/Rst58+SYyVuN2PgMkgDiQdz0aVChgZjhWnSMrg4ZyfY6fTUVL2DI0V6FV63WA2QIDAQABo4G7MIG4MB0GA1UdDgQWBBSdovSFYX8D+RvUCq6vJyGFFWAj7TCBiAYDVR0jBIGAMH6AFJ2i9IVhfwP5G9QKrq8nIYUVYCPtoVukWTBXMQswCQYDVQQGEwJHQjETMBEGA1UECBMKU29tZS1TdGF0ZTEPMA0GA1UEBxMGTG9uZG9uMRAwDgYDVQQKEwdCZXRhbWF4MRAwDgYDVQQDEwdCZXRhbWF4ggkAmA98MmCqaaAwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQUFAANBALmrm7MTl67KXaCQQmq4073m3X7hIsjsbWlPxh4MJ+/ankAqMO0OziaWdazJSj4L3uKAPLNcTvJ1KVrLqB6ArSDxBZYDmDSI0ZxUGN9eeo0FY/jWNw=="
  private static final String PASSWORD = "password"

  private static SSLContext createServerContext() {
    def sslContext = SSLContext.getInstance("TLS")

    def keyStore = KeyStore.getInstance("JKS")
    keyStore.load(new ByteArrayInputStream(KEYSTORE.decodeBase64()), PASSWORD.toCharArray())

    def algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm") ?: "SunX509"
    def factory = KeyManagerFactory.getInstance(algorithm)
    factory.init keyStore, PASSWORD.toCharArray()

    sslContext.init factory.getKeyManagers(), null, null

    return sslContext
  }

  def "can serve content over HTTPS"() {
    given:
    launchConfig {
      sslContext createServerContext()
    }

    and:
    app {
      handlers {
        get {
          response.send "trust no one"
        }
      }
    }

    expect:
    def address = applicationUnderTest.address
    address.scheme == "https"

    and:
    address.toURL().text == "trust no one"
  }
}

/**
 * A rule that overrides the client SSL context with one that does not validate certificates or hostnames.
 * That means that any HTTPS connections made by the test are unverified and will work against HTTPS servers
 * running on localhost and/or with invalid certificates.
 */
@CompileStatic
class DummySSLContext implements TestRule {

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

  public static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = new HostnameVerifier() {
    @Override
    boolean verify(String s, SSLSession sslSession) {
      return true
    }
  }

  public static final TrustManager TRUST_ALL_TRUST_MANAGER = new X509TrustManager() {
    @Override
    void checkClientTrusted(X509Certificate[] chain, String authType) {}

    @Override
    void checkServerTrusted(X509Certificate[] chain, String authType) {}

    @Override
    X509Certificate[] getAcceptedIssuers() {
      null
    }
  }

  public static class DummySSLSocketFactory extends SSLSocketFactory {

    private final SSLContext sslContext = SSLContext.getInstance("TLS")
    private final SSLSocketFactory factory

    DummySSLSocketFactory() {
      sslContext.init null, [TRUST_ALL_TRUST_MANAGER] as TrustManager[], new SecureRandom()
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
