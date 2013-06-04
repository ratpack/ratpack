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

package org.ratpackframework.session.store;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.ratpackframework.guice.HandlerDecoratingModule;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.session.SessionManager;
import org.ratpackframework.session.store.internal.DefaultSessionStore;
import org.ratpackframework.session.store.internal.SessionStorageBindingHandler;

import javax.inject.Singleton;

/**
 * An extension module that provides an in memory map store for sessions, {@link SessionStore}.
 */
public class MapSessionsModule extends AbstractModule implements HandlerDecoratingModule {

  public static final int DEFAULT_MAX_ENTRIES = 100;
  public static final int DEFAULT_MAX_IDLE_TIMEOUT_MINUTES = 60;

  private final int maxEntries;
  private final int idleTimeoutMinutes;

  @SuppressWarnings("UnusedDeclaration")
  public MapSessionsModule() {
    this(DEFAULT_MAX_ENTRIES, DEFAULT_MAX_IDLE_TIMEOUT_MINUTES);
  }

  public MapSessionsModule(int maxEntries, int idleTimeoutMinutes) {
    this.maxEntries = maxEntries;
    this.idleTimeoutMinutes = idleTimeoutMinutes;
  }

  @Override
  protected void configure() {}

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  SessionStore provideMapSessionStore(SessionManager sessionManager) {
    DefaultSessionStore defaultMapSessionStore = new DefaultSessionStore(maxEntries, idleTimeoutMinutes);
    sessionManager.addSessionListener(defaultMapSessionStore);
    return defaultMapSessionStore;
  }

  public Handler decorate(Injector injector, Handler handler) {
    return new SessionStorageBindingHandler(handler);
  }
}
