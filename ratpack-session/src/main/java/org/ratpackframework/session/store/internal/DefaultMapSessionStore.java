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

package org.ratpackframework.session.store.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ratpackframework.session.SessionListener;
import org.ratpackframework.session.store.MapSessionStore;
import org.ratpackframework.session.store.SessionStorage;

import java.util.concurrent.*;

public class DefaultMapSessionStore implements MapSessionStore, SessionListener {

  private final Cache<String, SessionStorage> storage;

  public DefaultMapSessionStore(int maxEntries, int ttlMinutes) {
    storage = CacheBuilder.<String, ConcurrentMap<String, Object>>newBuilder()
        .maximumSize(maxEntries)
        .expireAfterAccess(ttlMinutes, TimeUnit.MINUTES)
        .build();
  }

  public void sessionInitiated(String id) {
  }

  public void sessionTerminated(String id) {
    storage.invalidate(id);
  }

  public SessionStorage get(String sessionId) {
    try {
      return storage.get(sessionId, new Callable<SessionStorage>() {
        public SessionStorage call() throws Exception {
          return new DefaultSessionStorage(new ConcurrentHashMap<String, Object>());
        }
      });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public long size() {
    return storage.size();
  }
}
