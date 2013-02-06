package org.ratpackframework.session.store;

import com.google.inject.AbstractModule;
import org.ratpackframework.session.SessionListener;
import org.ratpackframework.session.store.internal.DefaultMapSessionStore;

/**
 * An extension module that provides an in memory map store for sessions, {@link MapSessionStore}.
 */
public class MapSessionsModule extends AbstractModule {

  private final DefaultMapSessionStore store;

  public MapSessionsModule(int maxEntries, int ttlMinutes) {
    store = new DefaultMapSessionStore(maxEntries, ttlMinutes);
  }

  @Override
  protected void configure() {
    bind(MapSessionStore.class).toInstance(store);
    bind(SessionListener.class).toInstance(store);
  }

}
