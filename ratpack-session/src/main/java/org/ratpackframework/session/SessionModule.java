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

package org.ratpackframework.session;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.ratpackframework.guice.HandlerDecoratingModule;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.session.internal.DefaultSessionIdGenerator;
import org.ratpackframework.session.internal.DefaultSessionManager;
import org.ratpackframework.session.internal.SessionBindingHandler;

public class SessionModule extends AbstractModule implements HandlerDecoratingModule {

  private final SessionCookieConfig sessionCookieConfig;

  public SessionModule() {
    this(new SessionCookieConfig());
  }

  public SessionModule(SessionCookieConfig sessionCookieConfig) {
    this.sessionCookieConfig = sessionCookieConfig;
  }

  public SessionCookieConfig getSessionCookieConfig() {
    return sessionCookieConfig;
  }

  @Override
  protected void configure() {
    bind(SessionIdGenerator.class).to(DefaultSessionIdGenerator.class).in(Singleton.class);
    bind(SessionManager.class).to(DefaultSessionManager.class).in(Singleton.class);
    bind(SessionCookieConfig.class).toInstance(sessionCookieConfig);
  }

  public Handler decorate(Injector injector, Handler handler) {
    return new SessionBindingHandler(handler);
  }

}
