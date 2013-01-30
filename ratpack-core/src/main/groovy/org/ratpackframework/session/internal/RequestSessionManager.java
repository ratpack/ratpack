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
import org.ratpackframework.util.HttpDateUtil;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RequestSessionManager {

  private static final String COOKIE_NAME = "JSESSIONID";

  private final HttpServerRequest request;
  private final SessionManager sessionManager;
  private final String domain;
  private final String path;
  private final int expiresMin;

  private String cookieSessionId;
  private Session session;

  public RequestSessionManager(SessionManager sessionManager, HttpServerRequest request, String domain, String path, int expiresMin) {
    this.sessionManager = sessionManager;
    this.request = request;
    this.domain = domain;
    this.path = path;
    this.expiresMin = expiresMin;
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
    if (session == null) {
      session = new DelegatingSession(getStoredSession(), sessionManager);
    }

    return session;
  }

  private StoredSession getStoredSession() {
    String cookieSessionId = getCookieSessionId();
    if (cookieSessionId != null) {
      StoredSession storedSession = sessionManager.getSessionIfExists(cookieSessionId);
      if (storedSession != null) {
        return storedSession;
      }
    }

    StoredSession storedSession = sessionManager.createNewSession();
    StringBuilder setCookieValue = new StringBuilder(COOKIE_NAME).append("=").append(storedSession.getId())
      .append(";").append("path=").append(path);

    if (!domain.equals("localhost")) {
      setCookieValue.append(";domain=.").append(domain);
    }

    if (expiresMin > 0) {
      Calendar expires = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      expires.add(Calendar.MINUTE, expiresMin);
      String expiresString = HttpDateUtil.formatDate(expires.getTime(), "EEE, dd-MMM-yyyy HH:mm:ss zzz");
      setCookieValue.append(";").append("expires=").append(expiresString);
    }

    request.response.putHeader("Set-Cookie", setCookieValue.toString());
    return storedSession;
  }

}
