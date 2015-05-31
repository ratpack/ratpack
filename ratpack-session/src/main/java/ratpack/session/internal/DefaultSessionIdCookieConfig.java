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

package ratpack.session.internal;

import ratpack.session.SessionIdCookieConfig;

import java.time.Duration;

public class DefaultSessionIdCookieConfig implements SessionIdCookieConfig {

  private Duration expires;
  private String domain;
  private String path;
  private String name = "JSESSIONID";

  @Override
  public Duration getExpires() {
    return expires;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setExpires(Duration expires) {
    this.expires = expires;
  }

  @Override
  public void setDomain(String domain) {
    this.domain = domain;
  }

  @Override
  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public SessionIdCookieConfig expires(Duration expiresDuration) {
    this.expires = expiresDuration;
    return this;
  }

  @Override
  public SessionIdCookieConfig domain(String domain) {
    this.domain = domain;
    return this;
  }

  @Override
  public SessionIdCookieConfig path(String path) {
    this.path = path;
    return this;
  }

  @Override
  public SessionIdCookieConfig name(String name) {
    this.name = name;
    return this;
  }

}
