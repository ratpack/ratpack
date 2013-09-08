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

package org.ratpackframework.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;

/**
 * A default implementation of {@link SSLContextFactory} that initializes the
 * SSL context using a password-protected keystore file.
 */
public class DefaultSSLContextFactory implements SSLContextFactory {

  private final File keyStoreFile;
  private final char[] password;

  public DefaultSSLContextFactory(File keyStoreFile, String password) {
    this.keyStoreFile = keyStoreFile;
    this.password = password.toCharArray();
  }

  public DefaultSSLContextFactory(URL keyStoreFile, String password) throws URISyntaxException {
    this(new File(keyStoreFile.toURI()), password);
  }

  @Override
  public SSLContext createServerContext() throws GeneralSecurityException, IOException {
    SSLContext sslContext = SSLContext.getInstance("TLS");

    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream stream = new FileInputStream(keyStoreFile)) {
      keyStore.load(stream, password);
    }

    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "SunX509";
    }
    KeyManagerFactory factory = KeyManagerFactory.getInstance(algorithm);
    factory.init(keyStore, password);

    sslContext.init(factory.getKeyManagers(), null, null);

    return sslContext;
  }
}
