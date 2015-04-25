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

import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.Cookie;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.ResponseMetaData;
import ratpack.session.clientside.ClientSideSessionsModule;
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

  public CookieBasedSessionStorageBindingHandler(SessionService sessionService, String sessionName) {
    this.sessionService = sessionService;
    this.sessionName = sessionName;
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
          ClientSideSessionsModule.Config config = context.get(ClientSideSessionsModule.Config.COOKIE_SESSION_CONFIG_TYPE_TOKEN);
          if (!entries.isEmpty()) {
            ByteBufAllocator bufferAllocator = context.get(ByteBufAllocator.class);
            String[] cookieValuePartitions = sessionService.serializeSession(bufferAllocator, entries, config.getMaxCookieSize());
            for (int i = 0; i < cookieValuePartitions.length; i++) {
              sessionCookie(responseMetaData, sessionName + "_" + i, cookieValuePartitions[i], config.getPath(), config.getDomain());
            }
            currentSessionCookieCount = cookieValuePartitions.length;
          }
          for (int i = currentSessionCookieCount; i < initialSessionCookieCount; i++) {
            invalidateSessionCookie(responseMetaData, sessionName + "_" + i, config.getPath(), config.getDomain());
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
