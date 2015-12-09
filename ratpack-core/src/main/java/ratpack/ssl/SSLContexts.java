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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import javax.net.ssl.SSLException;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Factory methods for initializing an {@link SslContext}.
 */
public class SSLContexts {

  /**
   * Creates an SSL context using a password-protected private key.
   *
   * @param certificate a <code>file://</code> URL referencing a certificate chain file
   * @param privateKey a <code>file://</code> URL referencing a private key file
   * @param password the password for the private key file
   * @return A newly created ssl context
   * @throws SSLException in case of any ssl related error.
   */
  public static SslContext create(URL certificate, URL privateKey, String password) throws SSLException, URISyntaxException {
    return create(new File(certificate.toURI()), new File(privateKey.toURI()), password);
  }

  /**
   * Creates an SSL context.
   *
   * @param certificate a <code>file://</code> URL referencing a certificate chain file
   * @param privateKey a <code>file://</code> URL referencing a private key file
   * @return A newly created ssl context
   * @throws SSLException in case of any ssl related error.
   */
  public static SslContext create(URL certificate, URL privateKey) throws SSLException, URISyntaxException {
    return create(new File(certificate.toURI()), new File(privateKey.toURI()));
  }

  /**
   * Creates an SSL context using a password-protected private key.
   *
   * @param certificate a <code>file://</code> URI referencing a certificate chain file
   * @param privateKey a <code>file://</code> URI referencing a private key file
   * @param password the password for the private key file
   * @return A newly created ssl context
   * @throws SSLException in case of any ssl related error.
   */
  public static SslContext create(URI certificate, URI privateKey, String password) throws SSLException{
    return create(new File(certificate), new File(privateKey), password);
  }

  /**
   * Creates an SSL context.
   *
   * @param certificate a <code>file://</code> URI referencing a certificate chain file
   * @param privateKey a <code>file://</code> URI referencing a private key file
   * @return A newly created ssl context
   * @throws SSLException in case of any ssl related error.
   */
  public static SslContext create(URI certificate, URI privateKey) throws SSLException{
    return create(new File(certificate), new File(privateKey));
  }

  /**
   * Creates an SSL context using a password-protected private key.
   *
   * @param certificate a certificate file
   * @param privateKey a private key file
   * @param password the password for the private key file
   * @return A newly created ssl context
   * @throws SSLException in case of any ssl related error.
   */
  public static SslContext create(Path certificate, Path privateKey, String password) throws SSLException {
    return create(certificate.toFile(), privateKey.toFile(), password);
  }

  /**
   * Creates an SSL context.
   *
   * @param certificate a certificate file
   * @param privateKey a private key file
   * @return A newly created ssl context
   * @throws SSLException in case of any ssl related error.
   */
  public static SslContext create(File certificate, File privateKey) throws SSLException {
    return SslContextBuilder.forServer(certificate, privateKey).build();
  }

  /**
   * Creates an SSL context using a password-protected private key.
   *
   * @param certificate a certificate file
   * @param privateKey a private key file
   * @param password the password for the private key file
   * @return A newly created ssl context
   * @throws SSLException in case of any ssl related error.
   */
  public static SslContext create(File certificate, File privateKey, String password) throws SSLException {
    return SslContextBuilder.forServer(certificate, privateKey, password).build();
  }

  private SSLContexts() {
  }
}
