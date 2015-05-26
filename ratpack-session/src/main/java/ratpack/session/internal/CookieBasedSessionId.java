/*
 * Copyright 2015 the original author or authors.
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

package ratpack.session.internal;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.session.SessionIdCookieConfig;
import ratpack.session.SessionIdGenerator;

import java.util.Optional;

public class CookieBasedSessionId implements SessionId {

  private static final String COOKIE_NAME = "JSESSIONID";

  private final Request request;
  private final Response response;
  private final SessionIdGenerator sessionIdGenerator;
  private final SessionIdCookieConfig cookieConfig;

  private String assignedCookieId;
  private Optional<String> cookieSessionId;

  public CookieBasedSessionId(Request request, Response response, SessionIdGenerator sessionIdGenerator, SessionIdCookieConfig cookieConfig) {
    this.request = request;
    this.response = response;
    this.sessionIdGenerator = sessionIdGenerator;
    this.cookieConfig = cookieConfig;
  }

  @Override
  public String getValue() {
    if (assignedCookieId != null) {
      return assignedCookieId;
    }

    return getCookieSessionId().orElseGet(() -> {
      assignedCookieId = assignId();
      return assignedCookieId;
    });
  }

  private Optional<String> getCookieSessionId() {
    if (cookieSessionId == null) {
      Cookie match = null;

      for (Cookie cookie : request.getCookies()) {
        if (cookie.name().equals(COOKIE_NAME)) {
          match = cookie;
          break;
        }
      }

      cookieSessionId = match == null ? Optional.empty() : Optional.of(match.value());
    }

    return cookieSessionId;
  }

  private String assignId() {
    String id = sessionIdGenerator.generateSessionId();
    setCookie(id, cookieConfig.getExpiresMins());
    return id;
  }

  private void setCookie(String value, int expiryMins) {
    DefaultCookie cookie = new DefaultCookie(COOKIE_NAME, value);

    String cookieDomain = cookieConfig.getDomain();
    if (cookieDomain != null) {
      cookie.setDomain(cookieDomain);
    }

    String cookiePath = cookieConfig.getPath();
    if (cookiePath != null) {
      cookie.setPath(cookiePath);
    }

    if (expiryMins > 0) {
      cookie.setMaxAge(expiryMins * 60);
    }

    response.getCookies().add(cookie);
  }

  @Override
  public void terminate() {
    setCookie("", 0);
    cookieSessionId = null;
    assignedCookieId = null;
  }
}
