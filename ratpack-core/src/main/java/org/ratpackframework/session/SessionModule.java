package org.ratpackframework.session;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.ratpackframework.session.internal.DefaultSessionManager;

public class SessionModule extends AbstractModule {

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

}
