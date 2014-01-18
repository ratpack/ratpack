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

package ratpack.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;

/**
 * Factory methods for initializing an {@link SSLContext}.
 */
public class SSLContexts {

  /**
   * Creates an SSL context using a password-protected keystore file.
   *
   * @param keyStoreFile a <code>file://</code> URL referencing a keystore file
   * @param password the password for the keystore file
   * @return A newly created ssl context
   * @throws GeneralSecurityException if the keystore is invalid, or the password is incorrect
   * @throws IOException if the url cannot be read
   */
  public static SSLContext sslContext(URL keyStoreFile, String password) throws GeneralSecurityException, IOException {
    try (InputStream stream = keyStoreFile.openStream()) {
      return sslContext(stream, password);
    }
  }

  /**
   * Creates an SSL context using a password-protected keystore file.
   *
   * @param keyStoreFile a keystore file
   * @param password the password for the keystore file
   * @return A newly created ssl context
   * @throws GeneralSecurityException if the keystore is invalid, or the password is incorrect
   * @throws IOException if the url cannot be read
   */
  public static SSLContext sslContext(File keyStoreFile, String password) throws GeneralSecurityException, IOException {
    try (InputStream stream = new FileInputStream(keyStoreFile)) {
      return sslContext(stream, password);
    }
  }

  /**
   * Creates an SSL context using a password-protected keystore file.
   *
   * @param keyStoreStream an input stream reading a keystore file
   * @param password the password for the keystore file
   * @return A newly created ssl context
   * @throws GeneralSecurityException if the keystore is invalid, or the password is incorrect
   * @throws IOException if the url cannot be read
   */
  public static SSLContext sslContext(InputStream keyStoreStream, String password) throws GeneralSecurityException, IOException {
    return sslContext(keyStoreStream, password.toCharArray());
  }

  private static SSLContext sslContext(InputStream keyStoreStream, char[] password) throws GeneralSecurityException, IOException {
    SSLContext sslContext = SSLContext.getInstance("TLS");

    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(keyStoreStream, password);

    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }
    KeyManagerFactory factory = KeyManagerFactory.getInstance(algorithm);
    factory.init(keyStore, password);

    sslContext.init(factory.getKeyManagers(), null, null);

    return sslContext;
  }

  private SSLContexts() {
  }
}
