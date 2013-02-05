/*
 * Copyright 2012 the original author or authors.
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

import org.ratpackframework.session.SessionConfig;
import org.ratpackframework.session.SessionIdGenerator;
import org.ratpackframework.session.SessionListener;

public class DefaultSessionConfig implements SessionConfig {

  private final SessionIdGenerator idGenerator;
  private final String cookieDomain;
  private final String cookiePath;
  private final int cookieExpiryMins;
  private final SessionListener sessionListener;

  public DefaultSessionConfig(SessionIdGenerator idGenerator, SessionListener sessionListener, int cookieExpiryMins, String cookieDomain, String cookiePath) {
    this.idGenerator = idGenerator;
    this.cookieExpiryMins = cookieExpiryMins;
    this.cookieDomain = cookieDomain;
    this.cookiePath = cookiePath;

    if (sessionListener == null) {
      this.sessionListener = new SessionListener() {
        @Override
        public void sessionInitiated(String id) {
        }

        @Override
        public void sessionTerminated(String id) {
        }
      };
    } else {
      this.sessionListener = sessionListener;
    }
  }

  @Override
  public String getCookieDomain() {
    return cookieDomain;
  }

  @Override
  public String getCookiePath() {
    return cookiePath;
  }

  @Override
  public int getCookieExpiryMins() {
    return cookieExpiryMins;
  }

  @Override
  public SessionIdGenerator getIdGenerator() {
    return idGenerator;
  }

  @Override
  public SessionListener getSessionListener() {
    return sessionListener;
  }
}
