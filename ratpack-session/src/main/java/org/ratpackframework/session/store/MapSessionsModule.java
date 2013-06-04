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
 * <h5>Provides</h5>
 * <ul>
 * <li>{@link SessionStorage} - using an in-memory (i.e. non persistent) cache</li>
 * </ul>
 * <h5>Prerequisites:</h5>
 * <ul>
 * <li>{@link SessionManager} (can be provided by {@link org.ratpackframework.session.SessionModule})</li>
 * </ul>
 * <h5>Getting the session storage</h5>
 * <p>
 * This module {@linkplain #decorate(com.google.inject.Injector, org.ratpackframework.handling.Handler) decorates the handler} to make
 * the {@link SessionStorage} available during request processing.
 * <pre class="tested">
 * import org.ratpackframework.handling.*;
 * import org.ratpackframework.session.store.SessionStorage;
 *
 * class MyHandler implements Handler {
 *   void handle(Exchange exchange) {
 *     SessionStorage session = exchange.get(SessionStorage.class);
 *   }
 * }
 * </pre>
 * </p>
 */
public class MapSessionsModule extends AbstractModule implements HandlerDecoratingModule {

  private final int maxEntries;
  private final int idleTimeoutMinutes;

  /**
   * Creates a new module with the given configuration for session storage.
   *
   * @param maxEntries The number of maximum sessions to store (old sessions are evicted according to LRU)
   * @param idleTimeoutMinutes How long a session can be idle before its considered inactive and able to be evicted
   */
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

  /**
   * Makes {@link SessionStorage} available in the exchange context.
   *
   * @param injector The injector created from all the application modules
   * @param handler The application handler
   * @return A handler that provides a {@link SessionStorage} impl in the exchange context
   */
  public Handler decorate(Injector injector, Handler handler) {
    return new SessionStorageBindingHandler(handler);
  }
}
