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

import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.session.Session;
import ratpack.session.store.SessionStorage;
import ratpack.session.store.SessionStore;
import ratpack.func.Factory;

public class SessionStorageBindingHandler implements Handler {

  private final Handler handler;

  public SessionStorageBindingHandler(Handler handler) {
    this.handler = handler;
  }

  public void handle(final Context context) {
    Registry registry = Registries.registry(SessionStorage.class, new Factory<SessionStorage>() {
      public SessionStorage create() {
        Session session = context.get(Session.class);
        String id = session.getId();
        SessionStore sessionStore = context.get(SessionStore.class);
        return sessionStore.get(id);
      }
    });

    context.insert(registry, handler);
  }

}