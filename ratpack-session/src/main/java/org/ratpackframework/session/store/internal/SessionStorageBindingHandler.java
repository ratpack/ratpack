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

package org.ratpackframework.session.store.internal;

import com.google.common.collect.ImmutableList;
import org.ratpackframework.handling.Context;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.LazyHierarchicalRegistry;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.session.Session;
import org.ratpackframework.session.store.SessionStore;
import org.ratpackframework.session.store.SessionStorage;
import org.ratpackframework.util.internal.Factory;

import java.util.List;

public class SessionStorageBindingHandler implements Handler {

  private final List<Handler> handler;

  public SessionStorageBindingHandler(Handler handler) {
    this.handler = ImmutableList.of(handler);
  }

  public void handle(final Context context) {
    final SessionStore sessionStore = context.get(SessionStore.class);
    Registry sessionRegistry = new LazyHierarchicalRegistry(context, SessionStorage.class, new Factory<SessionStorage>() {
      public SessionStorage create() {
        Session session = context.get(Session.class);
        final String id = session.getId();
        return sessionStore.get(id);
      }
    });
    context.insert(sessionRegistry, handler);
  }

}