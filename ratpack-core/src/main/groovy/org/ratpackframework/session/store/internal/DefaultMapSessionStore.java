package org.ratpackframework.session.store.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ratpackframework.Request;
import org.ratpackframework.session.SessionListener;
import org.ratpackframework.session.store.MapSessionStore;

import java.util.concurrent.*;

public class DefaultMapSessionStore implements MapSessionStore, SessionListener {

  private final Cache<String, ConcurrentMap<String, Object>> storage;

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
  public ConcurrentMap<String, Object> get(Request request) {
    try {
      return storage.get(request.getSession().getId(), new Callable<ConcurrentHashMap<String, Object>>() {
        @Override
        public ConcurrentHashMap<String, Object> call() throws Exception {
          return new ConcurrentHashMap<>();
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
