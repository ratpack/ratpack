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

  private Duration expiresDuration;
  private String domain;
  private String path;

  @Override
  public Duration getExpiresDuration() {
    return expiresDuration;
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
  public void setExpiresDuration(Duration expiresDuration) {
    this.expiresDuration = expiresDuration;
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
  public SessionIdCookieConfig expiresDuration(Duration expiresDuration) {
    this.expiresDuration = expiresDuration;
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

}
