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

package ratpack.ssl.internal;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class SslContexts {

  public static KeyManagerFactory keyManagerFactory(InputStream keyStoreStream, char[] keyStorePassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(keyStoreStream, keyStorePassword);
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(getAlgorithm());
    keyManagerFactory.init(keyStore, keyStorePassword);
    return keyManagerFactory;
  }

  public static TrustManagerFactory trustManagerFactory(InputStream trustStoreStream, char[] trustStorePassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    TrustManagerFactory trustManagerFactory;
    KeyStore trustStore = KeyStore.getInstance("JKS");
    trustStore.load(trustStoreStream, trustStorePassword);
    trustManagerFactory = TrustManagerFactory.getInstance(getAlgorithm());
    trustManagerFactory.init(trustStore);
    return trustManagerFactory;
  }

  private static String getAlgorithm() {
    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }
    return algorithm;
  }

  private SslContexts() {
  }
}
