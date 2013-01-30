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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.concurrent.TimeUnit;

public class SessionManager {

  private final SessionIdGenerator idGenerator;
  private final Cache<String, StoredSession> storage;
  private final String host;
  private final String path;
  private final int cookieExpiryMins;

  public SessionManager(SessionIdGenerator idGenerator, int maxEntries, int ttlMinutes, int cookieExpiryMins, String host, String path) {
    this.idGenerator = idGenerator;
    this.cookieExpiryMins = cookieExpiryMins;
    this.host = host;
    this.path = path;

    this.storage = CacheBuilder.<String, StoredSession>newBuilder()
        .maximumSize(maxEntries)
        .expireAfterAccess(ttlMinutes, TimeUnit.MINUTES)
        .build();
  }

  public StoredSession getSessionIfExists(String id) {
    storage.cleanUp();
    return storage.getIfPresent(id);
  }

  public StoredSession createNewSession() {
    storage.cleanUp();
    String id = idGenerator.generateSessionId();
    DefaultStoredSession storedSession = new DefaultStoredSession(id);
    storage.put(id, storedSession);
    return storedSession;
  }

  public void clearSession(String id) {
    storage.invalidate(id);
    storage.cleanUp();
  }

  public RequestSessionManager getRequestSessionManager(HttpServerRequest request) {
    return new RequestSessionManager(this, request, host, path, cookieExpiryMins);
  }
}
