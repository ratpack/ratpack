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

import org.ratpackframework.util.Factory;
import org.ratpackframework.context.Context;
import org.ratpackframework.context.internal.LazyHierarchicalContext;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.session.Session;
import org.ratpackframework.session.store.MapSessionStore;
import org.ratpackframework.session.store.SessionStorage;

public class SessionStorageBindingHandler implements Handler {

  private final Handler handler;

  public SessionStorageBindingHandler(Handler handler) {
    this.handler = handler;
  }

  public void handle(Exchange exchange) {
    final MapSessionStore mapSessionStore = exchange.get(MapSessionStore.class);

    Session session = exchange.get(Session.class);
    final String id = session.getId();
    Context sessionContext = new LazyHierarchicalContext(exchange.getContext(), SessionStorage.class, new Factory<SessionStorage>() {
      public SessionStorage create() {
        return mapSessionStore.get(id);
      }
    });
    exchange.nextWithContext(sessionContext, handler);
  }

}