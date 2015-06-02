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

public class SessionIdCookieConfig {

  private Duration expires;
  private String domain;
  private String path;
  private String name = "JSESSIONID";
  private boolean httpOnly = true;
  private boolean secure;

  public Duration getExpires() {
    return expires;
  }

  public String getDomain() {
    return domain;
  }

  public String getPath() {
    return path;
  }

  public String getName() {
    return name;
  }

  public boolean isHttpOnly() {
    return httpOnly;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setExpires(Duration expires) {
    this.expires = expires;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public SessionIdCookieConfig expires(Duration expiresDuration) {
    this.expires = expiresDuration;
    return this;
  }

  public SessionIdCookieConfig domain(String domain) {
    this.domain = domain;
    return this;
  }

  public SessionIdCookieConfig path(String path) {
    this.path = path;
    return this;
  }

  public SessionIdCookieConfig name(String name) {
    this.name = name;
    return this;
  }

  public SessionIdCookieConfig httpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
    return this;
  }

  public SessionIdCookieConfig secure(boolean secure) {
    this.secure = secure;
    return this;
  }
}
