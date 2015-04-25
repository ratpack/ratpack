/*
 * Copyright 2014 the original author or authors.
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

package ratpack.session.clientside.internal;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.Cookie;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.ResponseMetaData;
import ratpack.session.clientside.SessionService;
import ratpack.session.store.SessionStorage;
import ratpack.session.store.internal.DefaultSessionStorage;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CookieBasedSessionStorageBindingHandler implements Handler {

  private final SessionService sessionService;
  private final String sessionName;
  private final String path;
  private final String domain;
  private final int maxCookieSize;

  public CookieBasedSessionStorageBindingHandler(SessionService sessionService, String sessionName, String path, String domain, int maxCookieSize) {
    this.sessionService = sessionService;
    this.sessionName = sessionName;
    this.path = path;
    this.domain = domain;
    this.maxCookieSize = maxCookieSize;
  }

  public void handle(final Context context) {
    context.getRequest().addLazy(SessionStorage.class, () -> {
      Cookie[] sessionCookies = findSessionCookies(context.getRequest().getCookies());
      ConcurrentMap<String, Object> sessionMap = sessionService.deserializeSession(sessionCookies);
      DefaultSessionStorage storage = new DefaultSessionStorage(sessionMap);
      ConcurrentMap<String, Object> initialSessionMap = new ConcurrentHashMap<>(sessionMap);
      context.getRequest().add(InitialStorageContainer.class, new InitialStorageContainer(new DefaultSessionStorage(initialSessionMap)));
      return storage;
    });

    context.getResponse().beforeSend(responseMetaData -> {
      Optional<SessionStorage> storageOptional = context.getRequest().maybeGet(SessionStorage.class);
      if (storageOptional.isPresent()) {
        SessionStorage storage = storageOptional.get();
        boolean hasChanged = !context.getRequest().get(InitialStorageContainer.class).isSameAsInitial(storage);
        if (hasChanged) {
          int initialSessionCookieCount = findSessionCookies(context.getRequest().getCookies()).length;
          Set<Map.Entry<String, Object>> entries = storage.entrySet();
          int currentSessionCookieCount = 0;
          if (!entries.isEmpty()) {
            ByteBufAllocator bufferAllocator = context.get(ByteBufAllocator.class);
            String[] cookieValuePartitions = sessionService.serializeSession(bufferAllocator, entries, maxCookieSize);
            for (int i = 0; i < cookieValuePartitions.length; i++) {
              sessionCookie(responseMetaData, sessionName + "_" + i, cookieValuePartitions[i], path, domain);
            }
            currentSessionCookieCount = cookieValuePartitions.length;
          }
          for (int i = currentSessionCookieCount; i < initialSessionCookieCount; i++) {
            invalidateSessionCookie(responseMetaData, sessionName + "_" + i, path, domain);
          }
        }
      }
    });

    context.next();
  }

  private Cookie[] findSessionCookies(Set<Cookie> cookies) {
    if (cookies == null) {
      return new Cookie[0];
    }
    return cookies
      .stream()
      .filter(c -> c.name().startsWith(sessionName))
      .sorted((c1, c2) -> c1.name().compareTo(c2.name()))
      .toArray(Cookie[]::new);
  }

  private Cookie invalidateSessionCookie(ResponseMetaData responseMetaData, String cookieName, String path, String domain) {
    Cookie sessionCookie = responseMetaData.expireCookie(cookieName);
    if (path != null) {
      sessionCookie.setPath(path);
    }
    if (domain != null) {
      sessionCookie.setDomain(domain);
    }
    return sessionCookie;
  }

  private Cookie sessionCookie(ResponseMetaData responseMetaData, String name, String value, String path, String domain) {
    Cookie sessionCookie = responseMetaData.cookie(name, value);
    if (path != null) {
      sessionCookie.setPath(path);
    }
    if (domain != null) {
      sessionCookie.setDomain(domain);
    }
    return sessionCookie;
  }
}
