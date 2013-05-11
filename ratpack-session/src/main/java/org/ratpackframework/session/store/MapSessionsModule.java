package org.ratpackframework.session.store;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.ratpackframework.session.SessionListener;
import org.ratpackframework.session.SessionManager;
import org.ratpackframework.session.store.internal.DefaultMapSessionStore;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * An extension module that provides an in memory map store for sessions, {@link MapSessionStore}.
 */
public class MapSessionsModule extends AbstractModule {

  private final int maxEntries;
  private final int ttlMinutes;

  public MapSessionsModule(int maxEntries, int ttlMinutes) {
    this.maxEntries = maxEntries;
    this.ttlMinutes = ttlMinutes;
  }

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  MapSessionStore provideMapSessionStore(SessionManager sessionManager) {
    DefaultMapSessionStore defaultMapSessionStore = new DefaultMapSessionStore(maxEntries, ttlMinutes);
    sessionManager.addSessionListener(defaultMapSessionStore);
    return defaultMapSessionStore;
  }

}
