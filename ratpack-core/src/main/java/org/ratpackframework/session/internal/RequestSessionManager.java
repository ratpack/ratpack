/*
 * Copyright 2012 the original author or authors.
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

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.ratpackframework.http.Exchange;
import org.ratpackframework.session.Session;
import org.ratpackframework.session.SessionManager;

public class RequestSessionManager {

  private static final String COOKIE_NAME = "JSESSIONID";

  private final Exchange exchange;
  private final SessionManager sessionManager;

  private String cookieSessionId;
  private String assignedCookieId;

  public RequestSessionManager(Exchange exchange, SessionManager sessionManager) {
    this.exchange = exchange;
    this.sessionManager = sessionManager;
  }

  private String getCookieSessionId() {
    if (cookieSessionId == null) {
      Cookie match = null;

      for (Cookie cookie : exchange.getRequest().getCookies()) {
        if (cookie.getName().equals(COOKIE_NAME)) {
          match = cookie;
          break;
        }
      }

      if (match != null) {
        cookieSessionId = match.getValue();
      } else {
        cookieSessionId = "";
      }

    }

    return cookieSessionId.equals("") ? null : cookieSessionId;
  }

  public Session getSession() {
    return new Session() {
      @Override
      public String getId() {
        return getSessionIdOrInit();
      }

      @Override
      public String getExistingId() {
        return getCookieSessionId();
      }

      @Override
      public String regen() {
        String existingId = getExistingId();
        if (existingId != null) {
          sessionManager.notifySessionTerminated(existingId);
        }
        return assignId();

      }

      @Override
      public void terminate() {
        String existingId = getExistingId();
        if (existingId == null) {
          throw new IllegalStateException("Cannot terminate inactive session");
        }
        sessionManager.notifySessionTerminated(existingId);
        setCookie("", 0);
      }
    };
  }

  private String getSessionIdOrInit() {
    if (assignedCookieId != null) {
      return assignedCookieId;
    }

    String cookieSessionId = getCookieSessionId();
    if (cookieSessionId != null) {
      return cookieSessionId;
    }

    assignedCookieId = assignId();
    return assignedCookieId;
  }

  private String assignId() {
    String id = sessionManager.getIdGenerator().generateSessionId(exchange.getRequest());
    setCookie(id, sessionManager.getCookieExpiryMins());


    sessionManager.notifySessionInitiated(id);
    return id;
  }

  private void setCookie(String value, int expiryMins) {
    DefaultCookie cookie = new DefaultCookie(COOKIE_NAME, value);

    String cookieDomain = sessionManager.getCookieDomain();
    if (cookieDomain != null) {
      cookie.setDomain(cookieDomain);
    }

    String cookiePath = sessionManager.getCookiePath();
    if (cookiePath != null) {
      cookie.setPath(cookiePath);
    }

    if (expiryMins > 0) {
      cookie.setMaxAge(expiryMins * 60);
    }

    exchange.getResponse().getCookies().add(cookie);
  }

}
