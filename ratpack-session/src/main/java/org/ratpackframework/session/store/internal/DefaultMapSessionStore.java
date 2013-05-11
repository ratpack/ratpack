package org.ratpackframework.session.store.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ratpackframework.session.SessionListener;
import org.ratpackframework.session.store.DefaultSessionStorage;
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

  @Override
  public void sessionInitiated(String id) {
  }

  @Override
  public void sessionTerminated(String id) {
    storage.invalidate(id);
  }

  @Override
  public SessionStorage get(String sessionId) {
    try {
      return storage.get(sessionId, new Callable<SessionStorage>() {
        @Override
        public SessionStorage call() throws Exception {
          return new DefaultSessionStorage(new ConcurrentHashMap<String, Object>());
        }
      });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long size() {
    return storage.size();
  }
}
