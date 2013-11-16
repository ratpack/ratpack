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

import javax.net.SocketFactory;
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

  public static SocketFactory getDefault() {
    return SSLSocketFactory.getDefault();
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return new String[]{
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
      "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return delegate.getDefaultCipherSuites();
  }

  @Override
  public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
    return delegate.createSocket(socket, s, i, b);
  }

  @Override
  public Socket createSocket() throws IOException {
    return delegate.createSocket();
  }

  @Override
  public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
    return delegate.createSocket(s, i);
  }

  @Override
  public Socket createSocket(String s, int i, InetAddress inetAddress, int i2) throws IOException, UnknownHostException {
    return delegate.createSocket(s, i, inetAddress, i2);
  }

  @Override
  public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
    return delegate.createSocket(inetAddress, i);
  }

  @Override
  public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
    return delegate.createSocket(inetAddress, i, inetAddress2, i2);
  }
}
