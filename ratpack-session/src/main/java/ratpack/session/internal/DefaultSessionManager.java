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

package ratpack.session.internal;

import com.google.inject.Singleton;
import ratpack.session.SessionIdCookieConfig;
import ratpack.session.SessionIdGenerator;
import ratpack.session.SessionListener;
import ratpack.session.SessionManager;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DefaultSessionManager implements SessionManager {

  private final SessionIdGenerator idGenerator;
  private final List<SessionListener> sessionListeners = new ArrayList<SessionListener>(1);
  private final SessionIdCookieConfig sessionIdCookieConfig;

  @Inject
  public DefaultSessionManager(SessionIdGenerator idGenerator, SessionIdCookieConfig sessionIdCookieConfig) {
    this.idGenerator = idGenerator;
    this.sessionIdCookieConfig = sessionIdCookieConfig;
  }

  public String getCookieDomain() {
    return sessionIdCookieConfig.getDomain();
  }

  public String getCookiePath() {
    return sessionIdCookieConfig.getPath();
  }

  public Duration getCookieExpiry() {
    return sessionIdCookieConfig.getExpiresDuration();
  }

  public SessionIdGenerator getIdGenerator() {
    return idGenerator;
  }

  public void addSessionListener(SessionListener sessionListener) {
    sessionListeners.add(sessionListener);
  }

  public void notifySessionInitiated(String sessionId) {
    for (SessionListener sessionListener : sessionListeners) {
      sessionListener.sessionInitiated(sessionId);
    }
  }

  public void notifySessionTerminated(String sessionId) {
    for (SessionListener sessionListener : sessionListeners) {
      sessionListener.sessionTerminated(sessionId);
    }
  }

}
