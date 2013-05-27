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

package org.ratpackframework.session.internal;

import org.ratpackframework.session.SessionCookieConfig;

public class DefaultSessionCookieConfig implements SessionCookieConfig {

  private final int expiresMins;
  private final String domain;
  private final String path;

  public DefaultSessionCookieConfig(int expiresMins, String domain, String path) {
    this.expiresMins = expiresMins;
    this.domain = domain;
    this.path = path;
  }

  public int getExpiresMins() {
    return expiresMins;
  }

  public String getDomain() {
    return domain;
  }

  public String getPath() {
    return path;
  }

}
