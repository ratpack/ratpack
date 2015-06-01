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

package ratpack.session.clientside.internal;

import ratpack.session.clientside.ClientSideSessionConfig;

import java.time.Duration;

public class DefaultClientSideSessionConfig implements ClientSideSessionConfig {
  private static final String LAST_ACCESS_TIME_TOKEN = "ratpack_lat";

  private String sessionCookieName = "ratpack_session";
  private String secretToken = Long.toString(System.currentTimeMillis() / 10000);
  private String macAlgorithm = "HmacSHA1";
  private String secretKey;
  private String cipherAlgorithm = "AES/CBC/PKCS5Padding";
  private String path = "/";
  private String domain;
  private int maxSessionCookieSize = 1932;
  private Duration maxInactivityInterval = Duration.ofSeconds(120);


  @Override
  public String getSessionCookieName() {
    return sessionCookieName;
  }

  @Override
  public void setSessionCookieName(String sessionCookieName) {
    this.sessionCookieName = sessionCookieName;
  }

  @Override
  public String getLastAccessTimeCookieName() {
    return LAST_ACCESS_TIME_TOKEN;
  }

  @Override
  public String getSecretToken() {
    return secretToken;
  }

  @Override
  public void setSecretToken(String secretToken) {
    this.secretToken = secretToken;
  }

  @Override
  public String getMacAlgorithm() {
    return macAlgorithm;
  }

  @Override
  public void setMacAlgorithm(String macAlgorithm) {
    this.macAlgorithm = macAlgorithm;
  }

  @Override
  public String getSecretKey() {
    return secretKey;
  }

  @Override
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  public String getCipherAlgorithm() {
    return cipherAlgorithm;
  }

  @Override
  public void setCipherAlgorithm(String cipherAlgorithm) {
    this.cipherAlgorithm = cipherAlgorithm;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getDomain() {
    return this.domain;
  }

  @Override
  public void setDomain(String domain) {
    this.domain = domain;
  }

  @Override
  public int getMaxSessionCookieSize() {
    return maxSessionCookieSize;
  }

  @Override
  public void setMaxSessionCookieSize(int maxSessionCookieSize) {
    if (maxSessionCookieSize < 1024 || maxSessionCookieSize > 4096) {
      this.maxSessionCookieSize = 2048;
    } else {
      this.maxSessionCookieSize = maxSessionCookieSize;
    }
  }

  @Override
  public Duration getMaxInactivityInterval() {
    return maxInactivityInterval;
  }

  @Override
  public void setMaxInactivityInterval(Duration maxInactivityInterval) {
    this.maxInactivityInterval = maxInactivityInterval;
  }
}
