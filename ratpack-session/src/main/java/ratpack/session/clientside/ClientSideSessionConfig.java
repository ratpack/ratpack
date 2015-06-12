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
public class ClientSideSessionConfig {

  private static final String LAST_ACCESS_TIME_TOKEN = "ratpack_lat";

  private String sessionCookieName = "ratpack_session";
  private String secretToken = Long.toString(System.currentTimeMillis() / 10000);
  private String macAlgorithm = "HmacSHA1";
  private String secretKey;
  private String cipherAlgorithm = "AES/CBC/PKCS5Padding";
  private int maxSessionCookieSize = 1932;
  private Duration maxInactivityInterval = Duration.ofHours(24);

  /**
   * The name of the {@code cookie} used to store serialized and encrypted session data.
   * <p>
   * If length of the serialized session is greater than {@link #getMaxSessionCookieSize()} it is partioned into more
   * cookies. Every session cookie has a postfix {@code _index}, where {@code index} is the partition number.
   * <p>
   * <b>Defaults to: </b> {@code ratpack_session}
   * @return the name of the {@code cookie} used to store session data.
   */
  public String getSessionCookieName() {
    return sessionCookieName;
  }

  /**
   * Set the {@code cookie} name used to store session data.
   *
   * @param sessionCookieName a {@code cookie} name used to store session data
   */
  public void setSessionCookieName(String sessionCookieName) {
    this.sessionCookieName = sessionCookieName;
  }

  /**
   * The name of the {@code cookie} used to store session's last access time.
   * <p>
   * Last access time is updated on every session load or store
   * @return the name of the {@code cookie} with session's last access time
   */
  public String getLastAccessTimeCookieName() {
    return LAST_ACCESS_TIME_TOKEN;
  }

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
  public String getSecretToken() {
    return secretToken;
  }

  /**
   * Set the {code secretToken} used to sign the serialized and encrypted session data.
   *
   * @param secretToken a token used to sign the serialized and encrypted session data.
   */
  public void setSecretToken(String secretToken) {
    this.secretToken = secretToken;
  }

  /**
   * The {@link javax.crypto.Mac} algorithm used to sign the serialized session with the <strong>secretToken</strong>.
   *
   * @return the mac algorithm used to sign serialized and encrypted session data.
   */
  public String getMacAlgorithm() {
    return macAlgorithm;
  }

  /**
   * Set mac algorithm used to sign the serialized and encrypted session data.
   *
   * @param macAlgorithm the name of mac algorithm
   */
  public void setMacAlgorithm(String macAlgorithm) {
    this.macAlgorithm = macAlgorithm;
  }

  /**
   * The secret key used in the symmetric-key encyrption/decryption of the serialized session.
   *
   * @return the secret key used in encryption/decryption of the serialized session data.
   */
  public String getSecretKey() {
    return secretKey;
  }

  /**
   * Set the secret key used in the symmetric-key encryption/decryption of the serialized session data.
   * @param secretKey a secret key
   */
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  /**
   * The {@link javax.crypto.Cipher} algorithm used to encrypt/decrypt the serialized session
   * <p>
   * e.g. <strong>AES/CBC/PKCS5Padding</strong> which is also the default value.
   *
   * @return the algorithm used to encrypt/decrypt the serialized session.
   */
  public String getCipherAlgorithm() {
    return cipherAlgorithm;
  }

  /**
   * Set the cipher algorithm used to encrypt/decrypt the serialized session data.
   *
   * @param cipherAlgorithm a cipher algorithm
   */
  public void setCipherAlgorithm(String cipherAlgorithm) {
    this.cipherAlgorithm = cipherAlgorithm;
  }

    /**
   * Maximum size of the session cookie. If encrypted cookie exceeds it, it will be partitioned.
   * <p>
   * According to the <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a> web cookies should be at least
   * 4096 bytes per cookie and at least 20 cookies per domain should be supported.
   * <p>
   * <b>Defaults to: </b> {@code 1932}.
   * @return the maximum size of the cookie session.
   */
  public int getMaxSessionCookieSize() {
    return maxSessionCookieSize;
  }

  /**
   * Set maximum size of the session cookie. If encrypted cookie session exceeds it, it wil be partitioned.
   * <p>
   * If it is less than {@code 1024} or greater than {@code 4096} default value will be used.
   *
   * @param maxSessionCookieSize a maximum size of one session cookie.
   */
  public void setMaxSessionCookieSize(int maxSessionCookieSize) {
    if (maxSessionCookieSize < 1024 || maxSessionCookieSize > 4096) {
      this.maxSessionCookieSize = 2048;
    } else {
      this.maxSessionCookieSize = maxSessionCookieSize;
    }
  }

  /**
   * Maximum inactivity time (in units defined by {@link java.util.concurrent.TimeUnit}) after which session will be invalidated.
   * <p>
   * Defaults to: 24 hours.
   * If time between last access and current time is less than or equal to max inactive time, session will become valid.
   *
   * @return the maximum session inactivity time
   */
  public Duration getMaxInactivityInterval() {
    return maxInactivityInterval;
  }

  /**
   * Set maximum inactivity time (in seconds) of the cookie session.
   *
   * @param maxInactivityInterval a maximum inactivity time of the cookie session
   */
  public void setMaxInactivityInterval(Duration maxInactivityInterval) {
    this.maxInactivityInterval = maxInactivityInterval;
  }
}
