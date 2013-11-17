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

package ratpack.site.crawl;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class DelegatingSSLSocketFactory extends SSLSocketFactory {

  private final SSLSocketFactory delegate;

  public DelegatingSSLSocketFactory(SSLSocketFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return delegate.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return delegate.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
    return decorate(delegate.createSocket(socket, s, i, b));
  }

  @Override
  public Socket createSocket() throws IOException {
    return decorate(delegate.createSocket());
  }

  @Override
  public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
    return decorate(delegate.createSocket(s, i));
  }

  @Override
  public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException, UnknownHostException {
    return decorate(delegate.createSocket(s, i, inetAddress, i2));
  }

  @Override
  public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
    return decorate(delegate.createSocket(inetAddress, i));
  }

  @Override
  public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
    return decorate(delegate.createSocket(inetAddress, i, inetAddress2, i2));
  }

  protected Socket decorate(Socket socket) {
    ((SSLSocket) socket).setEnabledCipherSuites(new String[]{
      "SSL_RSA_WITH_RC4_128_MD5",
      "SSL_RSA_WITH_RC4_128_SHA",
      "TLS_RSA_WITH_AES_128_CBC_SHA",
      "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
      "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
      "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
      "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
      "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
      "SSL_RSA_WITH_DES_CBC_SHA",
      "SSL_DHE_RSA_WITH_DES_CBC_SHA",
      "SSL_DHE_DSS_WITH_DES_CBC_SHA",
      "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
      "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
      "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
      "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
      "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
    });

    return socket;
  }

}
