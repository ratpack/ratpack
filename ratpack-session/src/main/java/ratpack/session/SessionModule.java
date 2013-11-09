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
import com.google.inject.Injector;
import com.google.inject.Singleton;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;
import ratpack.session.internal.DefaultSessionCookieConfig;
import ratpack.session.internal.DefaultSessionIdGenerator;
import ratpack.session.internal.DefaultSessionManager;
import ratpack.session.internal.SessionBindingHandler;

@SuppressWarnings("UnusedDeclaration")
public class SessionModule extends AbstractModule implements HandlerDecoratingModule {

  private int cookieExpiresMins = 60 * 60 * 24 * 365; // 1 year
  private String cookieDomain;
  private String cookiePath = "/";

  public int getCookieExpiresMins() {
    return cookieExpiresMins;
  }

  public void setCookieExpiresMins(int cookieExpiresMins) {
    this.cookieExpiresMins = cookieExpiresMins;
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
    bind(SessionManager.class).to(DefaultSessionManager.class).in(Singleton.class);
    bind(SessionCookieConfig.class).toInstance(new DefaultSessionCookieConfig(cookieExpiresMins, cookieDomain, cookiePath));
  }

  public Handler decorate(Injector injector, Handler handler) {
    return new SessionBindingHandler(handler);
  }

}
