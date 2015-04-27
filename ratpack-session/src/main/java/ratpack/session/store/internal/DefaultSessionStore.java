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

package ratpack.session.store.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.exec.ExecControl;
import ratpack.session.SessionListener;
import ratpack.session.store.SessionStorage;
import ratpack.session.store.SessionStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static ratpack.util.Exceptions.toException;
import static ratpack.util.Exceptions.uncheck;

public class DefaultSessionStore implements SessionStore, SessionListener {

  private final Cache<String, SessionStorage> storage;

  public DefaultSessionStore(int maxEntries, int ttlMinutes) {
    storage = CacheBuilder.newBuilder()
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
      return storage.get(sessionId, () -> new DefaultSessionStorage(new ConcurrentHashMap<>(), ExecControl.current()));
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  public long size() {
    return storage.size();
  }
}
