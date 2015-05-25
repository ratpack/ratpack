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

import com.google.common.collect.Maps;
import io.netty.handler.codec.http.Cookie;
import ratpack.func.Pair;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.ResponseMetaData;
import ratpack.session.clientside.SessionService;
import ratpack.session.store.internal.ChangeTrackingSessionStorage;
import ratpack.stream.Streams;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class CookieBasedSessionStorageBindingHandler implements Handler {

  private final SessionService sessionService;
  private final String sessionName;
  private final String path;
  private final String domain;
  private final int maxCookieSize;
  private final Duration maxInactivityInterval;
  private static final String LAST_ACCESS_TIME_TOKEN = "$LAT$";

  public CookieBasedSessionStorageBindingHandler(SessionService sessionService, String sessionName, String path, String domain, int maxCookieSize, Duration maxInactivityInterval) {
    this.sessionService = sessionService;
    this.sessionName = sessionName;
    this.path = path;
    this.domain = domain;
    this.maxCookieSize = maxCookieSize;
    this.maxInactivityInterval = maxInactivityInterval;
  }

  public void handle(final Context context) {
    context.getRequest().addLazy(ChangeTrackingSessionStorage.class, () -> {
      Cookie[] sessionCookies = getSessionCookies(context.getRequest().getCookies());
      ConcurrentMap<String, Object> sessionMap = sessionService.deserializeSession(context, sessionCookies);
      ChangeTrackingSessionStorage changeTrackingSessionStorage = null;
      if (!maxInactivityInterval.isNegative()) {
        long lastAccessTime = Long.valueOf((String) sessionMap.getOrDefault(LAST_ACCESS_TIME_TOKEN, "-1"));
        long currentTime = System.currentTimeMillis();
        long maxInactivityIntervalMillis = maxInactivityInterval.toMillis();
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
          storage.getKeys().then(keys ->
            context.stream(Streams.publish(keys))
              .flatMap(key ->
                  storage.get(key, Object.class).map(value -> Pair.of(key, value.orElse(null)))
              )
              .toList()
              .then(entries -> {
                int initialSessionCookieCount = getSessionCookies(context.getRequest().getCookies()).length;
                int currentSessionCookieCount = 0;

                if (!entries.isEmpty()) {
                  Map<String, Object> data = Maps.newHashMap();
                  data.put(LAST_ACCESS_TIME_TOKEN, Long.toString(System.currentTimeMillis()));
                  for (Pair<String, Object> entry : entries) {
                    data.put(entry.left, entry.right);
                  }
                  String[] cookieValuePartitions = sessionService.serializeSession(context, data.entrySet(), maxCookieSize);
                  for (int i = 0; i < cookieValuePartitions.length; i++) {
                    addSessionCookie(responseMetaData, sessionName + "_" + i, cookieValuePartitions[i], path, domain);
                  }
                  currentSessionCookieCount = cookieValuePartitions.length;
                }
                for (int i = currentSessionCookieCount; i < initialSessionCookieCount; i++) {
                  invalidateSessionCookie(responseMetaData, sessionName + "_" + i, path, domain);
                }
              })
          );
        }
      }
    });

    context.next();
  }

  private Cookie[] getSessionCookies(Set<Cookie> cookies) {
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
