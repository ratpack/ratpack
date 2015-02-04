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

package ratpack.session.store;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import ratpack.handling.HandlerDecorator;
import ratpack.session.Session;
import ratpack.session.SessionManager;
import ratpack.session.store.internal.DefaultSessionStore;

import javax.inject.Singleton;

/**
 * An extension module that provides an in memory map store for sessions, {@link SessionStore}.
 * <h3>Provides</h3>
 * <ul>
 * <li>{@link SessionStorage} - using an in-memory (i.e. non persistent) cache</li>
 * </ul>
 * <h3>Prerequisites:</h3>
 * <ul>
 * <li>{@link SessionManager} (can be provided by {@link ratpack.session.SessionModule})</li>
 * </ul>
 * <h3>Getting the session storage</h3>
 * <p>
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.session.store.SessionStorage;
 *
 * class MyHandler implements Handler {
 *   void handle(Context ctx) {
 *     SessionStorage session = ctx.getRequest().get(SessionStorage.class);
 *   }
 * }
 * </pre>
 */
public class MapSessionsModule extends AbstractModule {

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
  protected void configure() {
    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toInstance(HandlerDecorator.prepend(ctx -> {
        ctx.getRequest().addLazy(SessionStorage.class, () -> {
          Session session = ctx.getRequest().get(Session.class);
          String id = session.getId();
          SessionStore sessionStore = ctx.get(SessionStore.class);
          return sessionStore.get(id);
        });

        ctx.next();
      }
    ));
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  SessionStore provideMapSessionStore(SessionManager sessionManager) {
    DefaultSessionStore defaultMapSessionStore = new DefaultSessionStore(maxEntries, idleTimeoutMinutes);
    sessionManager.addSessionListener(defaultMapSessionStore);
    return defaultMapSessionStore;
  }

}
