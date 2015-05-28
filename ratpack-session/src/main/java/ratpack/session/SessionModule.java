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

package ratpack.session;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import ratpack.handling.HandlerDecorator;
import ratpack.session.internal.DefaultSessionIdCookieConfig;
import ratpack.session.internal.DefaultSessionIdGenerator;
import ratpack.session.internal.DefaultSessionManager;
import ratpack.session.internal.RequestSessionManager;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SuppressWarnings("UnusedDeclaration")
public class SessionModule extends AbstractModule {

  private Duration cookieExpiration = ChronoUnit.YEARS.getDuration();

  private String cookieDomain;
  private String cookiePath = "/";

  public Duration getCookieExpiration() {
    return cookieExpiration;
  }

  public void setCookieExpiration(Duration cookieExpiration) {
    this.cookieExpiration = cookieExpiration;
  }

  public String getCookieDomain() {
    return cookieDomain;
  }

  public void setCookieDomain(String cookieDomain) {
    this.cookieDomain = cookieDomain;
  }

  public String getCookiePath() {
    return cookiePath;
  }

  public void setCookiePath(String cookiePath) {
    this.cookiePath = cookiePath;
  }

  @Override
  protected void configure() {
    bind(SessionIdGenerator.class).to(DefaultSessionIdGenerator.class).in(Singleton.class);
    bind(SessionManager.class).to(DefaultSessionManager.class);
    bind(SessionIdCookieConfig.class).toInstance(new DefaultSessionIdCookieConfig().expiresDuration(cookieExpiration).domain(cookieDomain).path(cookiePath));

    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toInstance(HandlerDecorator.prepend(ctx -> {
      ctx.getRequest().addLazy(Session.class, () -> {
        SessionManager sessionManager = ctx.get(SessionManager.class);
        final RequestSessionManager requestSessionManager = new RequestSessionManager(ctx, sessionManager);
        return requestSessionManager.getSession();
      });
      ctx.next();
    }));
  }

}
