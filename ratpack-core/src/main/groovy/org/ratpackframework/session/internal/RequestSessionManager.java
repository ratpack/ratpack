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

import org.ratpackframework.session.Session;
import org.ratpackframework.session.SessionConfig;
import org.ratpackframework.util.HttpDateUtil;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class RequestSessionManager {

  private static final String COOKIE_NAME = "JSESSIONID";

  private final HttpServerRequest request;
  private final SessionConfig sessionConfig;

  private String cookieSessionId;
  private String assignedCookieId;
  private boolean alreadySetCookie;

  public RequestSessionManager(HttpServerRequest request, SessionConfig sessionConfig) {
    this.request = request;
    this.sessionConfig = sessionConfig;
  }

  private String getCookieSessionId() {
    if (cookieSessionId == null) {
      String cookieHeader = request.headers().get("cookie");
      if (cookieHeader != null) {
        CookieTokenizer tokenizer = new CookieTokenizer();
        int num = tokenizer.tokenize(cookieHeader);
        for (int i = 0; i < num; i += 2) {
          String token = tokenizer.tokenAt(i);
          if (token.equals(COOKIE_NAME) && i < (num - 1)) {
            cookieSessionId = tokenizer.tokenAt(i + 1);
            break;
          }
        }
      }
    }

    return cookieSessionId;
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
          sessionConfig.getSessionListener().sessionTerminated(existingId);
        }
        return assignId();

      }

      @Override
      public void terminate() {
        String existingId = getExistingId();
        if (existingId == null) {
          throw new IllegalStateException("Cannot terminate inactive session");
        }
        sessionConfig.getSessionListener().sessionTerminated(existingId);
        setCookie("", new Date(0));
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
    String id = sessionConfig.getIdGenerator().generateSessionId(request);
    int expiryMins = sessionConfig.getCookieExpiryMins();
    if (expiryMins > 0) {
      Calendar expires = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      expires.add(Calendar.MINUTE, expiryMins);
      setCookie(id, expires.getTime());
    } else {
      setCookie(id, null);
    }

    sessionConfig.getSessionListener().sessionInitiated(id);
    return id;
  }

  private void setCookie(String value, Date expires) {
    if (alreadySetCookie) {
      throw new IllegalStateException("Cannot set cookie twice for the one request");
    }
    alreadySetCookie = true;
    StringBuilder setCookieValue = new StringBuilder(COOKIE_NAME).append("=").append(value)
        .append(";").append("path=").append(sessionConfig.getCookiePath());

    String cookieDomain = sessionConfig.getCookieDomain();
    if (!cookieDomain.equals("localhost")) {
      setCookieValue.append(";domain=.").append(cookieDomain);
    }

    if (expires != null) {
      String expiresString = HttpDateUtil.formatDate(expires, "EEE, dd-MMM-yyyy HH:mm:ss zzz");
      setCookieValue.append(";").append("expires=").append(expiresString);
    }

    request.response.putHeader("Set-Cookie", setCookieValue.toString());
  }

}
