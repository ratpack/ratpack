/*
 * Copyright 2014 the original author or authors.
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
import com.google.inject.Injector;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;
import ratpack.session.Crypto;
import ratpack.session.internal.DefaultCrypto;
import ratpack.session.store.internal.CookieBasedSessionStorageBindingHandler;

import javax.inject.Singleton;

/**
 * An extension module that provides a cookie based store for sessions.
 * <h3>Provides</h3>
 * <ul>
 * <li>{@link SessionStorage} - deserialized from the client's cookie</li>
 * </ul>
 * <h3>Getting the session storage</h3>
 * <p>
 * This module {@linkplain #decorate(com.google.inject.Injector, ratpack.handling.Handler) decorates the handler} to make
 * the {@link SessionStorage} available during request processing.
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.session.store.SessionStorage;
 *
 * class MyHandler implements Handler {
 *   void handle(Context ctx) {
 *     SessionStorage session = ctx.get(SessionStorage.class);
 *   }
 * }
 * </pre>
 */
public class CookieBasedSessionsModule extends AbstractModule implements HandlerDecoratingModule {

  @Override
  protected void configure() {
    bind(Crypto.class).to(DefaultCrypto.class).in(Singleton.class);
  }

  /**
   * Makes {@link ratpack.session.store.SessionStorage} available in the context registry.
   *
   * @param injector The injector created from all the application modules
   * @param handler The application handler
   * @return A handler that provides a {@link ratpack.session.store.SessionStorage} impl in the context registry
   */
  public Handler decorate(Injector injector, Handler handler) {
    return new CookieBasedSessionStorageBindingHandler(handler);
  }
}
