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
import ratpack.session.store.internal.ChangeTrackingSessionStorage;
import ratpack.stream.Streams;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class CookieBasedSessionStorageBindingHandler implements Handler {

  private final SessionService sessionService;
  private final String sessionName;
  private final String path;
  private final String domain;
  private final int maxCookieSize;
  private final long maxInactivityInterval;
  private static final String LAST_ACCESS_TIME_TOKEN = "$LAT$";

  public CookieBasedSessionStorageBindingHandler(SessionService sessionService, String sessionName, String path, String domain, int maxCookieSize, long maxInactivityInterval) {
    this.sessionService = sessionService;
    this.sessionName = sessionName;
    this.path = path;
    this.domain = domain;
    this.maxCookieSize = maxCookieSize;
    this.maxInactivityInterval = maxInactivityInterval;
  }

  public void handle(final Context context) {
    context.getRequest().addLazy(ChangeTrackingSessionStorage.class, () -> {
      Cookie[] sessionCookies = findSessionCookies(context.getRequest().getCookies());
      ConcurrentMap<String, Object> sessionMap = sessionService.deserializeSession(sessionCookies);
      ChangeTrackingSessionStorage changeTrackingSessionStorage = null;
      if (maxInactivityInterval > -1) {
        long lastAccessTime = Long.valueOf((String) sessionMap.getOrDefault(LAST_ACCESS_TIME_TOKEN, "-1"));
        long currentTime = System.currentTimeMillis();
        long maxInactivityIntervalMillis = maxInactivityInterval * 1000;
        if (lastAccessTime == -1 || (currentTime - lastAccessTime) > maxInactivityIntervalMillis) {
          sessionMap.remove(LAST_ACCESS_TIME_TOKEN);
          // init tracking session with invalidated attributes and then clear them to force storage.hasChanged() return true.
          // Then session cookies will automatically be invalidated.
          changeTrackingSessionStorage = new ChangeTrackingSessionStorage(sessionMap, context);
          sessionMap.clear();
        }
      }
      if (sessionMap.size() > 0) {
        sessionMap.remove(LAST_ACCESS_TIME_TOKEN);
      }
      if (changeTrackingSessionStorage != null) {
        return changeTrackingSessionStorage;
      } else {
        return new ChangeTrackingSessionStorage(sessionMap, context);
      }
    });

    context.getResponse().beforeSend(responseMetaData -> {
      Optional<ChangeTrackingSessionStorage> storageOptional = context.getRequest().maybeGet(ChangeTrackingSessionStorage.class);
      if (storageOptional.isPresent()) {
        ChangeTrackingSessionStorage storage = storageOptional.get();
        if (storage.hasChanged()) {
          Set<Map.Entry<String, Object>> entries = new HashSet<Map.Entry<String, Object>>();

          storage.getKeys().then((keys) -> {
            context.stream(Streams.publish(keys))
              .flatMap((key) -> storage.get(key, Object.class).map((value) -> {
                if (value.isPresent()) {
                  return new AbstractMap.SimpleImmutableEntry<String, Object>(key, value.get());
                } else {
                  return null;
                }
              }))
              .toList().then((entryList) -> {
              for (Map.Entry<String, Object> entry : entryList) {
                if (entry != null) {
                  entries.add(entry);
                }
              }
              int initialSessionCookieCount = findSessionCookies(context.getRequest().getCookies()).length;
              int currentSessionCookieCount = 0;

              if (!entries.isEmpty()) {
                entries.add(new AbstractMap.SimpleImmutableEntry<String, Object>(LAST_ACCESS_TIME_TOKEN, Long.toString(System.currentTimeMillis())));
                ByteBufAllocator bufferAllocator = context.get(ByteBufAllocator.class);
                String[] cookieValuePartitions = sessionService.serializeSession(bufferAllocator, entries, maxCookieSize);
                for (int i = 0; i < cookieValuePartitions.length; i++) {
                  addSessionCookie(responseMetaData, sessionName + "_" + i, cookieValuePartitions[i], path, domain);
                }
                currentSessionCookieCount = cookieValuePartitions.length;
              }
              for (int i = currentSessionCookieCount; i < initialSessionCookieCount; i++) {
                invalidateSessionCookie(responseMetaData, sessionName + "_" + i, path, domain);
              }
            });
          });
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

  private void invalidateSessionCookie(ResponseMetaData responseMetaData, String cookieName, String path, String domain) {
    Cookie sessionCookie = responseMetaData.expireCookie(cookieName);
    if (path != null) {
      sessionCookie.setPath(path);
    }
    if (domain != null) {
      sessionCookie.setDomain(domain);
    }
  }

  private void addSessionCookie(ResponseMetaData responseMetaData, String name, String value, String path, String domain) {
    Cookie sessionCookie = responseMetaData.cookie(name, value);
    if (path != null) {
      sessionCookie.setPath(path);
    }
    if (domain != null) {
      sessionCookie.setDomain(domain);
    }
  }
}
