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

import ratpack.ssl.internal.SslContexts;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

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
   * Creates an SSL context using password protected keystore as well as password protected truststore file.
   *
   * @param keyStoreFile a <code>file://</code> URL referencing a keystore file
   * @param keyStorePassword the password for the keystore file
   * @param trustStoreFile a <code>file://</code> URL referencing a truststore file
   * @param trustStorePassword the password for the truststore file
   * @return A newly created ssl context
   * @throws GeneralSecurityException if either the keystore or truststore is invalid, or the password is incorrect
   * @throws IOException if any of the urls cannot be read
   */
  public static SSLContext sslContext(URL keyStoreFile, String keyStorePassword, URL trustStoreFile, String trustStorePassword) throws GeneralSecurityException, IOException {
    try (InputStream keyStoreStream = keyStoreFile.openStream(); InputStream trustStoreStream = trustStoreFile.openStream()) {
      return sslContext(keyStoreStream, keyStorePassword, trustStoreStream, trustStorePassword);
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
   * Creates an SSL context using password protected keystore as well as password protected truststore file.
   *
   * @param keyStoreFile a keystore file
   * @param keyStorePassword the pasword for the keystore file
   * @param trustStoreFile a truststore file
   * @param trustStorePassword the password for the truststore file
   * @return A newly created ssl context
   * @throws GeneralSecurityException if either the keystore or truststore is invalid, or the password is incorrect
   * @throws IOException if any of the urls cannot be read
   */
  public static SSLContext sslContext(File keyStoreFile, String keyStorePassword, File trustStoreFile, String trustStorePassword) throws GeneralSecurityException, IOException {
    try (InputStream keyStoreStream = new FileInputStream(keyStoreFile); InputStream trustStoreStream = new FileInputStream(trustStoreFile)) {
      return sslContext(keyStoreStream, keyStorePassword, trustStoreStream, trustStorePassword);
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
  public static SSLContext sslContext(Path keyStoreFile, String password) throws GeneralSecurityException, IOException {
    try (InputStream stream = Files.newInputStream(keyStoreFile)) {
      return sslContext(stream, password);
    }
  }

  /**
   * Creates an SSL context using password protected keystore as well as password protected truststore file.
   *
   * @param keyStoreFile a keystore file
   * @param keyStorePassword the password for the keystore file
   * @param trustStoreFile a truststore file
   * @param trustStorePassword the password for the truststore file
   * @return A newly created ssl context
   * @throws GeneralSecurityException if either the keystore or the truststore is invalid, or the password is incorrect
   * @throws IOException if any of the urls cannot be read
   */
  public static SSLContext sslContext(Path keyStoreFile, String keyStorePassword, Path trustStoreFile, String trustStorePassword) throws GeneralSecurityException, IOException {
    try (InputStream keyStoreStream = Files.newInputStream(keyStoreFile); InputStream trustStoreStream = Files.newInputStream(trustStoreFile)) {
      return sslContext(keyStoreStream, keyStorePassword, trustStoreStream, trustStorePassword);
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
    return sslContext(keyStoreStream, password.toCharArray(), null, null);
  }

  /**
   * Creates an SSL context using password protected keystore as well as password protected truststore file.
   * <p>
   * In SSL handshake the purpose of of keystore is to provide credentials, while the purpose of truststore is to verify credentials.
   * <p>
   * Trustore stores certificates from thrid parties that application trusts or certificates signed by CA that can be used to identify third party.
   * Keystore stores private key and public key that are used to generate certificates exposed to clients or used in client SSL authentication.
   *
   * @param keyStoreStream an input stream reading keystore file
   * @param keyStorePassword the password for the keystore file
   * @param trustStoreStream an input stream reading truststore file
   * @param trustStorePassword the password for the truststore file
   * @return A newly created ssl context
   * @throws GeneralSecurityException if either the keystore or the truststore is invalid, or the password is incorrect
   * @throws IOException if any of the urls cannot be read
   */
  public static SSLContext sslContext(InputStream keyStoreStream, String keyStorePassword, InputStream trustStoreStream, String trustStorePassword) throws GeneralSecurityException, IOException {
    return sslContext(keyStoreStream, keyStorePassword.toCharArray(), trustStoreStream, trustStorePassword.toCharArray());
  }

  private static SSLContext sslContext(InputStream keyStoreStream, char[] keyStorePassword, InputStream trustStoreStream, char[] trustStorePassword) throws GeneralSecurityException, IOException {
    SSLContext sslContext = SSLContext.getInstance("TLS");

    KeyManagerFactory keyManagerFactory = null;

    if (keyStoreStream != null) {
      keyManagerFactory = SslContexts.keyManagerFactory(keyStoreStream, keyStorePassword);
    }

    TrustManagerFactory trustManagerFactory = null;
    if (trustStoreStream != null) {
      trustManagerFactory = SslContexts.trustManagerFactory(trustStoreStream, trustStorePassword);
    }

    sslContext.init(keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null, trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null, null);

    return sslContext;
  }

  private SSLContexts() {
  }
}
