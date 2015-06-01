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

package ratpack.session.clientside.internal;

import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import ratpack.exec.ExecControl;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.session.SessionStore;
import ratpack.session.clientside.ClientSideSessionConfig;
import ratpack.session.clientside.Crypto;
import ratpack.session.clientside.Signer;

import javax.inject.Provider;
import java.nio.CharBuffer;
import java.util.Set;

public class ClientSideSessionStore implements SessionStore {

  private static final String SESSION_SEPARATOR = ":";

  private final ExecControl execControl;
  private final Provider<Request> request;
  private final Provider<Response> response;
  private final Signer signer;
  private final Crypto crypto;
  private final ByteBufAllocator bufferAllocator;
  private final ClientSideSessionConfig config;

  @Inject
  public ClientSideSessionStore(ExecControl execControl, Provider<Request> request, Provider<Response> response, Signer signer, Crypto crypto, ByteBufAllocator bufferAllocator, ClientSideSessionConfig config) {
    this.execControl = execControl;
    this.request = request;
    this.response = response;
    this.signer = signer;
    this.crypto = crypto;
    this.bufferAllocator = bufferAllocator;
    this.config = config;
  }

  @Override
  public Operation store(AsciiString sessionId, ByteBuf sessionData) {
    return execControl.operation(() -> {
      int oldSessionCookiesCount = getCookies(config.getSessionCookieName()).length;
      String[] sessionCookiePartitions = serialize(sessionData);
      for (int i = 0; i < sessionCookiePartitions.length; i++) {
        addCookie(config.getSessionCookieName() + "_" + i, sessionCookiePartitions[i]);
      }
      for (int i = sessionCookiePartitions.length; i < oldSessionCookiesCount; i++) {
        invalidateCookie(config.getSessionCookieName() + "_" + i);
      }
    });
  }

  @Override
  public Promise<ByteBuf> load(AsciiString sessionId) {
    return execControl.promiseFrom(() -> deserialize(getCookies(config.getSessionCookieName())));
  }

  @Override
  public Operation remove(AsciiString sessionId) {
    return execControl.operation(() -> {
      int oldSessionCookiesCount = getCookies(config.getSessionCookieName()).length;
      for (int i = 0; i < oldSessionCookiesCount; i++) {
        invalidateCookie(config.getSessionCookieName() + "_" + i);
      }
    });
  }

  @Override
  public Promise<Long> size() {
    return execControl.promiseOf(-1l);
  }

  private boolean isValid() {
    Cookie[] cookies = getCookies(config.getLastAccessTimeCookieName());
    if (cookies.length == 0) {
      return false;
    }
    ByteBuf payload = null;

    try {
      payload = deserialize(cookies);
      if (payload.readableBytes() == 0) {
        invalidateCookies(cookies);
        return false;
      }
      long lastAccessTime = payload.readLong();
      long currentTime = System.currentTimeMillis();
      long maxInactivityIntervalMillis = config.getMaxInactivityInterval().toMillis();
      if (currentTime - lastAccessTime > maxInactivityIntervalMillis) {
        invalidateCookies(cookies);
        return false;
      }
    } finally {
      if (payload != null) {
        payload.release();
      }
    }
    return true;
  }

  private String[] serialize(ByteBuf sessionData) {
    if (sessionData == null || sessionData.readableBytes() == 0) {
      return new String[0];
    }

    ByteBuf encrypted = null;
    ByteBuf digest = null;

    try {
      encrypted = crypto.encrypt(sessionData, bufferAllocator);
      String encryptedBase64 = toBase64(encrypted);
      digest = signer.sign(encrypted.resetReaderIndex(), bufferAllocator);
      String digestBase64 = toBase64(digest);
      String digestedBase64 = encryptedBase64 + SESSION_SEPARATOR + digestBase64;
      if (digestedBase64.length() <= config.getMaxSessionCookieSize()) {
        return new String[]{digestedBase64};
      }
      int count = (int) Math.ceil((double) digestedBase64.length() / config.getMaxSessionCookieSize());
      String[] partitions = new String[count];
      for (int i = 0; i < count; i++) {
        int from = i * config.getMaxSessionCookieSize();
        int to = Math.min(from + config.getMaxSessionCookieSize(), digestedBase64.length());
        partitions[i] = digestedBase64.substring(from, to);
      }
      return partitions;
    } finally {
      if (encrypted != null) {
        encrypted.release();
      }
      if (digest != null) {
        digest.release();
      }
    }
  }

  private ByteBuf deserialize(Cookie[] sessionCookies) {
    if (sessionCookies.length == 0) {
      return Unpooled.buffer(0, 0);
    }
    StringBuilder sessionCookie = new StringBuilder();
    for (int i = 0; i < sessionCookies.length; i++) {
      sessionCookie.append(sessionCookies[i].value());
    }
    String[] parts = sessionCookie.toString().split(SESSION_SEPARATOR);
    if (parts.length != 2) {
      return Unpooled.buffer(0, 0);
    }
    ByteBuf payload = null;
    ByteBuf digest = null;
    ByteBuf expectedDigest = null;
    ByteBuf decryptedPayload = null;
    try {
      payload = fromBase64(bufferAllocator, parts[0]);
      digest = fromBase64(bufferAllocator, parts[1]);
      expectedDigest = signer.sign(payload, bufferAllocator);
      if (ByteBufUtil.equals(digest, expectedDigest)) {
        decryptedPayload = crypto.decrypt(payload.resetReaderIndex(), bufferAllocator);
      } else {
        decryptedPayload = Unpooled.buffer(0, 0);
      }
    } finally {
      if (payload != null) {
        payload.release();
      }
      if (digest != null) {
        digest.release();
      }
      if (expectedDigest != null) {
        expectedDigest.release();
      }
    }
    return decryptedPayload;
  }

  private String toBase64(ByteBuf byteBuf) {
    ByteBuf encoded = Base64.encode(byteBuf, false, Base64Dialect.STANDARD);
    try {
      return encoded.toString(CharsetUtil.ISO_8859_1);
    } finally {
      encoded.release();
    }
  }

  private ByteBuf fromBase64(ByteBufAllocator bufferAllocator, String string) {
    ByteBuf byteBuf = ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(string), CharsetUtil.ISO_8859_1);
    try {
      return Base64.decode(byteBuf, Base64Dialect.STANDARD);
    } finally {
      byteBuf.release();
    }
  }

  private Cookie[] getCookies(String startsWith) {
    Set<Cookie> cookies = request.get().getCookies();
    if (cookies == null || cookies.size() == 0) {
      return new Cookie[0];
    }
    return cookies
      .stream()
      .filter(c -> c.name().startsWith(startsWith))
      .sorted((c1, c2) -> c1.name().compareTo(c2.name()))
      .toArray(Cookie[]::new);
  }

  private void invalidateCookies(Cookie[] cookies) {
    for (int i = 0; i < cookies.length; i++) {
      invalidateCookie(cookies[i].name());
    }
  }

  private void invalidateCookie(String cookieName) {
    Cookie cookie = response.get().expireCookie(cookieName);
    if (config.getPath() != null) {
      cookie.setPath(config.getPath());
    }
    if (config.getDomain() != null) {
      cookie.setDomain(config.getDomain());
    }
  }

  private void addCookie(String name, String value) {
    Cookie sessionCookie = response.get().cookie(name, value);
    if (config.getPath() != null) {
      sessionCookie.setPath(config.getPath());
    }
    if (config.getDomain() != null) {
      sessionCookie.setDomain(config.getDomain());
    }
  }
}
