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

package org.ratpackframework.session.internal;

import com.google.common.collect.ImmutableList;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.session.Session;
import org.ratpackframework.session.SessionManager;
import org.ratpackframework.util.Factory;

import java.util.List;

public class SessionBindingHandler implements Handler {

  private final List<Handler> delegate;

  public SessionBindingHandler(Handler delegate) {
    this.delegate = ImmutableList.of(delegate);
  }

  public void handle(final Context context) {
    context.insert(delegate, Session.class, new Factory<Session>() {
      public Session create() {
        SessionManager sessionManager = context.get(SessionManager.class);
        final RequestSessionManager requestSessionManager = new RequestSessionManager(context, sessionManager);
        return requestSessionManager.getSession();
      }
    });
  }

}
