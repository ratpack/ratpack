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

package ratpack.session.store.internal;

import io.netty.handler.codec.http.Cookie;
import ratpack.func.Action;
import ratpack.func.Pair;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.ResponseMetaData;
import ratpack.session.Crypto;
import ratpack.session.internal.StorageHashContainer;
import ratpack.session.store.SessionStorage;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CookieBasedSessionStorageBindingHandler implements Handler {

  private final String session_separator = ":";
  private final String ratpack_session = "ratpack_session";
  private final Handler handler;

  public CookieBasedSessionStorageBindingHandler(Handler handler) {
    this.handler = handler;
  }

  public void handle(final Context context) {
    context.getRequest().addLazy(SessionStorage.class, createSessionStorage(context));
    context.getResponse().beforeSend(serializeSession(context));
    context.insert(handler);
  }

  private Supplier<SessionStorage> createSessionStorage(Context context) {
    return () -> {
      Cookie cookieSession = context.getRequest()
        .getCookies()
        .stream()
        .filter(cookie -> ratpack_session.equals(cookie.getName()))
        .findFirst().orElse(null);
      DefaultSessionStorage storage = new DefaultSessionStorage(deserializeSession(context, cookieSession));
      context.getRequest().add(StorageHashContainer.class, new StorageHashContainer(storage.hashCode()));
      return storage;
    };
  }

  private String getEncodedPair(Map.Entry<String, Object> pairs) {
    String encoded = null;
    try {
      encoded = URLEncoder.encode(pairs.getKey(), "utf-8") + "=" + URLEncoder.encode(pairs.getValue().toString(), "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return encoded;
  }

  private Action<ResponseMetaData> serializeSession(Context context) {
    SessionStorage storage = context.getRequest()
      .maybeGet(SessionStorage.class)
      .orElse(null);
    return responseMetaData -> {
      if (storage != null) {
        boolean hasChanged = context.getRequest().get(StorageHashContainer.class).getHashCode() != storage.hashCode();
        if (hasChanged) {
          String encodedPairs = storage
            .entrySet()
            .stream()
            .map(this::getEncodedPair)
            .collect(Collectors.joining("&"));

          if (encodedPairs == null || encodedPairs.isEmpty()) {
            invalidateSession(responseMetaData);
          } else {
            String s = Base64.getUrlEncoder().encodeToString(encodedPairs.getBytes("utf-8"));
            String digest = context.get(Crypto.class).sign(encodedPairs);

            responseMetaData.cookie(ratpack_session, s + session_separator + digest);
          }
        }
      }
    };
  }

  private void invalidateSession(ResponseMetaData responseMetaData) {
    responseMetaData.expireCookie(ratpack_session);
  }

  private ConcurrentMap<String, Object> deserializeSession(Context context, Cookie cookieSession) {
    ConcurrentMap<String, Object> sessionStorage = new ConcurrentHashMap<>();

    String encodedPairs = cookieSession == null ? null : cookieSession.getValue();
    if (encodedPairs != null) {
      String[] parts = encodedPairs.split(session_separator);
      if (parts.length == 2) {
        String encoded = parts[0];
        String digest = parts[1];

        Crypto crypto = context.get(Crypto.class);
        try {
          byte[] pairs = Base64.getUrlDecoder().decode(encoded.getBytes("utf-8"));
          String unpacked = new String(pairs);

          if (crypto.sign(unpacked).equals(digest)) {

            Arrays.stream(unpacked.split("&")).map(s -> {
              String[] pair = s.split("=");
              return Pair.of(pair[0], pair[1]);
            }).forEach(getDecodedPairs(sessionStorage));

          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

    }

    return sessionStorage;
  }

  private Consumer<Pair<String, String>> getDecodedPairs(ConcurrentMap<String, Object> sessionStorage) throws UnsupportedEncodingException {
    return p -> {
      try {
        String key = URLDecoder.decode(p.getLeft(), "utf-8");
        String value = URLDecoder.decode(p.getRight(), "utf-8");
        sessionStorage.put(key, value);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    };
  }

}