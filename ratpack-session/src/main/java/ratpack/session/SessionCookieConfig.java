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

package ratpack.session;

import java.time.Duration;

/**
 * Basic configuration for cookies related to session management.
 * <p>
 * Attributes are shared with {@link ratpack.session.clientside.ClientSideSessionConfig} that is used by client side session (cookie based).
 *
 * @see ratpack.session.clientside.ClientSideSessionConfig
 */
public class SessionCookieConfig {

  private Duration expires;
  private String domain;
  private String path = "/";
  private String idName = "JSESSIONID";
  private boolean httpOnly = true;
  private boolean secure;

  /**
   * Cookie's max age.
   *
   * @return the max age of the session related cookies
   */
  public Duration getExpires() {
    return expires;
  }

  /**
   * Use the session cookie only when requesting from the {@code domain}.
   * <p>
   * Define the scope for the cookie.
   *
   * @return the URI domain to which session cookie will be attached to.
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Use the session cookie only when requesting from the {@code path}.
   * <p>
   * Define the scope of the cookie.
   * <p>
   * Session should be send for every request. The {@code path} of value {@code "/"} does this.
   * @return the URI path to which session cookie will be attached to.
   */
  public String getPath() {
    return path;
  }

  /**
   * The name of the cookie for session id.
   * <p>
   * Defaults to: {@code JSESSIONID}
   *
   * @return the name of the cookie for session id
   */
  public String getIdName() {
    return idName;
  }

  /**
   * {@code HttpOnly} cookies can only be used when transmitted via {@code HTTP/HTTPS}. They are not accessible for {@code JavaScript}.
   * <p>
   * Http only cookies have to be supported by the browser.
   *
   * @return true if client side session cookies are {@code HttpOnly}
   */
  public boolean isHttpOnly() {
    return httpOnly;
  }

  /**
   * {@code Secure} cookies can only be transmitted over encrypted connection like {@code HTTPS}.
   *
   * @return true if session cookies are {@code Secure}
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * Set cookie's max age.
   *
   * @param expires the duration after cookie expires
   */
  public void setExpires(Duration expires) {
    this.expires = expires;
  }

  /**
   * Set the {@code domain} for session cookie.
   * <p>
   * Define the scope of the cookie
   *
   * @param domain a domain to which session cokkie will be attached to
   */
  public void setDomain(String domain) {
    this.domain = domain;
  }

  /**
   * Set the {@code path} for session cookie.
   * <p>
   * Define the scope of the cookie.
   *
   * @param path a path to which session cookie will be attached to
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Set the name of the cookie for session id.
   *
   * @param idName the name of the cookie for session id
   */
  public void setIdName(String idName) {
    this.idName = idName;
  }

  /**
   * Set session cookies attribute {@code HttpOnly}.
   *
   * @param httpOnly if true client side session cookies are {@code HttpOnly}
   */
  public void setHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  /**
   * Set session cookies attribute {@code Secure}.
   *
   * @param secure if true client side session cookies can be transmitted only over encrypted connection
   */
  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  /**
   * Set max age of the cookies related to session management.
   *
   * @param expiresDuration the duration, max age, of the cookies related to session management
   * @return the config
   */
  public SessionCookieConfig expires(Duration expiresDuration) {
    this.expires = expiresDuration;
    return this;
  }

  /**
   * Set the {@code domain} for session cookie.
   * <p>
   * Define the scope of the cookie
   *
   * @param domain a domain to which session cokkie will be attached to
   * @return the config
   */
  public SessionCookieConfig domain(String domain) {
    this.domain = domain;
    return this;
  }

  /**
   * Set the {@code path} for session cookie.
   * <p>
   * Define the scope of the cookie.
   *
   * @param path a path to which session cookie will be attached to
   * @return the config
   */
  public SessionCookieConfig path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Set the name of the cookie for session id.
   *
   * @param idName the name of the cookie for session id
   * @return the config
   */
  public SessionCookieConfig idName(String idName) {
    this.idName = idName;
    return this;
  }

  /**
   * Set session cookies attribute {@code HttpOnly}.
   *
   * @param httpOnly if true client side session cookies are {@code HttpOnly}
   * @return the config
   */
  public SessionCookieConfig httpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
    return this;
  }

  /**
   * Set session cookies attribute {@code Secure}.
   *
   * @param secure if true client side session cookies can be transmitted only over encrypted connection
   * @return the config
   */
  public SessionCookieConfig secure(boolean secure) {
    this.secure = secure;
    return this;
  }
}
