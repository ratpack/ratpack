/*
 * Copyright 2015 the original author or authors.
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

package ratpack.session.clientside;

import java.time.Duration;

/**
 * Client side session configuration.
 */
public interface ClientSideSessionConfig {
  /**
   * The name of the {@code cookie} used to store serialized and encrypted session data.
   * <p>
   * If length of the serialized session is greater than {@link #getMaxSessionCookieSize()} it is partioned into more
   * cookies. Every session cookie has a postfix {@code _index}, where {@code index} is the partition number.
   * <p>
   * <b>Defaults to: </b> {@code ratpack_session}
   * @return the name of the {@code cookie} used to store session data.
   */
  String getSessionCookieName();

  /**
   * Set the {@code cookie} name used to store session data.
   *
   * @param sessionCookieName a {@code cookie} name used to store session data
   */
  void setSessionCookieName(String sessionCookieName);

  /**
   * The name of the {@code cookie} used to store session's last access time.
   * <p>
   * Last access time is updated on every session load or store
   * @return the name of the {@code cookie} with session's last access time
   */
  String getLastAccessTimeCookieName();

  /**
   * The token used to sign the serialized session to prevent tampering.
   * <p>
   * If not set, this is set to a time based value.
   * <p>
   * <b>Important: </b> if working with clustered sessions, not being tied to any ratpack app instance,
   * {@code secretToken} has to be the same in every ratpack instance configuration.
   *
   * @return the token used to sign the serialized and encrypted session.
   */
  String getSecretToken();

  /**
   * Set the {code secretToken} used to sign the serialized and encrypted session data.
   *
   * @param secretToken a token used to sign the serialized and encrypted session data.
   */
  void setSecretToken(String secretToken);

  /**
   * The {@link javax.crypto.Mac} algorithm used to sign the serialized session with the <strong>secretToken</strong>.
   *
   * @return the mac algorithm used to sign serialized and encrypted session data.
   */
  String getMacAlgorithm();

  /**
   * Set mac algorithm used to sign the serialized and encrypted session data.
   *
   * @param macAlgorithm the name of mac algorithm
   */
  void setMacAlgorithm(String macAlgorithm);

  /**
   * The secret key used in the symmetric-key encyrption/decryption of the serialized session.
   *
   * @return the secret key used in encryption/decryption of the serialized session data.
   */
  String getSecretKey();

  /**
   * Set the secret key used in the symmetric-key encryption/decryption of the serialized session data.
   * @param secretKey a secret key
   */
  void setSecretKey(String secretKey);

  /**
   * The {@link javax.crypto.Cipher} algorithm used to encrypt/decrypt the serialized session
   * <p>
   * e.g. <strong>AES/CBC/PKCS5Padding</strong> which is also the default value.
   *
   * @return the algorithm used to encrypt/decrypt the serialized session.
   */
  String getCipherAlgorithm();

  /**
   * Set the cipher algorithm used to encrypt/decrypt the serialized session data.
   *
   * @param cipherAlgorithm a cipher algorithm
   */
  void setCipherAlgorithm(String cipherAlgorithm);

  /**
   * Use the session cookie only when requesting from the {@code path}.
   * <p>
   * Define the scope of the cookie.
   * <p>
   * Session should be send for every request. The {@code path} of value {@code "/"} does this.
   * @return the URI path to which session cookie will be attached to.
   */
  String getPath();

  /**
   * Set the {@code path} for session cookie.
   * <p>
   * Define the scope of the cookie.
   *
   * @param path a path to which session cookie will be attached to
   */
  void setPath(String path);

  /**
   * Use the session cookie only when requesting from the {@code domain}.
   * <p>
   * Define the scope for the cookie.
   *
   * @return the URI domain to which session cookie will be attached to.
   */
  String getDomain();

  /**
   * Set the {@code domain} for session cookie.
   * <p>
   * Define the scope of the cookie
   *
   * @param domain a domain to which session cokkie will be attached to
   */
  void setDomain(String domain);

  /**
   * Maximum size of the session cookie. If encrypted cookie exceeds it, it will be partitioned.
   * <p>
   * According to the <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a> web cookies should be at least
   * 4096 bytes per cookie and at least 20 cookies per domain should be supported.
   * <p>
   * <b>Defaults to: </b> {@code 1932}.
   * @return the maximum size of the cookie session.
   */
  int getMaxSessionCookieSize();

  /**
   * Set maximum size of the session cookie. If encrypted cookie session exceeds it, it wil be partitioned.
   * <p>
   * If it is less than {@code 1024} or greater than {@code 4096} default value will be used.
   *
   * @param maxSessionCookieSize a maximum size of one session cookie.
   */
  void setMaxSessionCookieSize(int maxSessionCookieSize);

  /**
   * Maximum inactivity time (in units defined by {@link java.util.concurrent.TimeUnit}) after which session will be invalidated.
   * <p>
   * Defaults to: 120s.
   * If time between last access and current time is less than or equal to max inactive time, session will become valid.
   *
   * @return the maximum session inactivity time
   */
  Duration getMaxInactivityInterval();

  /**
   * Set maximum inactivity time (in seconds) of the cookie session.
   *
   * @param maxInactivityInterval a maximum inactivity time of the cookie session
   */
  void setMaxInactivityInterval(Duration maxInactivityInterval);
}
