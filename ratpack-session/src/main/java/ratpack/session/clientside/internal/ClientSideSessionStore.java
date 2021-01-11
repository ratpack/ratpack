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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
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
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.core.http.Request;
import ratpack.core.http.Response;
import ratpack.session.SessionCookieConfig;
import ratpack.session.SessionStore;
import ratpack.session.clientside.ClientSideSessionConfig;
import ratpack.session.clientside.Crypto;
import ratpack.session.clientside.Signer;

import javax.inject.Provider;
import java.nio.CharBuffer;
import java.util.Optional;

public class ClientSideSessionStore implements SessionStore {

  private static final String SESSION_SEPARATOR = ":";

  private final Provider<Request> request;
  private final Provider<Response> response;
  private final Signer signer;
  private final Crypto crypto;
  private final ByteBufAllocator bufferAllocator;
  private final SessionCookieConfig cookieConfig;
  private final ClientSideSessionConfig config;

  private final CookieOrdering latCookieOrdering;
  private final CookieOrdering dataCookieOrdering;
  private final long expirySeconds;

  private static class CookieOrdering extends Ordering<Cookie> {
    private final int prefixLen;
    private final String prefix;

    public CookieOrdering(String prefix) {
      this.prefix = prefix;
      this.prefixLen = prefix.length() + 1;
    }

    @Override
    public int compare(Cookie left, Cookie right) {
      Integer leftNum = Integer.valueOf(left.name().substring(prefixLen));
      Integer rightNum = Integer.valueOf(right.name().substring(prefixLen));
      return leftNum.compareTo(rightNum);
    }
  }

  @Inject
  public ClientSideSessionStore(Provider<Request> request, Provider<Response> response, Signer signer, Crypto crypto, ByteBufAllocator bufferAllocator, SessionCookieConfig cookieConfig, ClientSideSessionConfig config) {
    this.request = request;
    this.response = response;
    this.signer = signer;
    this.crypto = crypto;
    this.bufferAllocator = bufferAllocator;
    this.cookieConfig = cookieConfig;
    this.config = config;
    this.expirySeconds = cookieConfig.getExpires() == null ? 0 : cookieConfig.getExpires().getSeconds();

    this.latCookieOrdering = new CookieOrdering(config.getLastAccessTimeCookieName());
    this.dataCookieOrdering = new CookieOrdering(config.getSessionCookieName());
  }

  @Override
  public Operation store(AsciiString sessionId, ByteBuf sessionData) {
    return Operation.of(() -> {
      CookieStorage cookieStorage = getCookieStorage();
      int oldSessionCookiesCount = cookieStorage.data.size();
      String[] sessionCookiePartitions = serialize(sessionData);
      for (int i = 0; i < sessionCookiePartitions.length; i++) {
        setCookie(config.getSessionCookieName() + "_" + i, sessionCookiePartitions[i]);
      }
      for (int i = sessionCookiePartitions.length; i < oldSessionCookiesCount; i++) {
        invalidateCookie(config.getSessionCookieName() + "_" + i);
      }
      setLastAccessTime(cookieStorage);
    });
  }

  @Override
  public Promise<ByteBuf> load(AsciiString sessionId) {
    return Promise.sync(() -> {
      CookieStorage cookieStorage = getCookieStorage();
      if (!isValid(cookieStorage)) {
        return Unpooled.EMPTY_BUFFER;
      }
      setLastAccessTime(cookieStorage);
      return deserialize(cookieStorage.data);
    });
  }

  @Override
  public Operation remove(AsciiString sessionId) {
    return Operation.of(() -> reset(getCookieStorage()));
  }

  private void reset(CookieStorage cookieStorage) {
    cookieStorage.lastAccessToken.forEach(this::invalidateCookie);
    cookieStorage.data.forEach(this::invalidateCookie);
    cookieStorage.clear();
  }

  @Override
  public Promise<Long> size() {
    return Promise.value(-1L);
  }

  private boolean isValid(CookieStorage cookieStorage) throws Exception {
    ByteBuf payload = null;

    try {
      payload = deserialize(cookieStorage.lastAccessToken);
      if (payload.readableBytes() == 0) {
        reset(cookieStorage);
        return false;
      }
      long lastAccessTime;
      try {
        lastAccessTime = payload.readLong();
      } catch (IndexOutOfBoundsException e) {
        // When using a NoPadding algorithm, decrypting the payload may trim one or more 0-bytes that are required in
        // order for us to read epoch timestamps ending in one or more 0-bytes, so get the long directly instead
        lastAccessTime = payload.getLong(payload.readerIndex());
      }

      long currentTime = System.currentTimeMillis();
      long maxInactivityIntervalMillis = config.getMaxInactivityInterval().toMillis();
      if (currentTime - lastAccessTime > maxInactivityIntervalMillis) {
        reset(cookieStorage);
        return false;
      }
    } finally {
      if (payload != null) {
        payload.release();
      }
    }
    return true;
  }

  private void setLastAccessTime(CookieStorage cookieStorage) throws Exception {
    ByteBuf data = null;
    try {
      data = Unpooled.buffer();
      data.writeLong(System.currentTimeMillis());
      int oldCookiesCount = cookieStorage.lastAccessToken.size();
      String[] partitions = serialize(data);
      for (int i = 0; i < partitions.length; i++) {
        setCookie(config.getLastAccessTimeCookieName() + "_" + i, partitions[i]);
      }
      for (int i = partitions.length; i < oldCookiesCount; i++) {
        invalidateCookie(config.getLastAccessTimeCookieName() + "_" + i);
      }
    } finally {
      if (data != null) {
        data.release();
      }
    }
  }

  private String[] serialize(ByteBuf sessionData) throws Exception {
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

  private ByteBuf deserialize(ImmutableList<Cookie> sessionCookies) throws Exception {
    if (sessionCookies.isEmpty()) {
      return Unpooled.EMPTY_BUFFER;
    }

    StringBuilder sessionCookie = new StringBuilder();
    for (Cookie cookie : sessionCookies) {
      sessionCookie.append(cookie.value());
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
        payload.touch().release();
      }
      if (digest != null) {
        digest.release();
      }
      if (expectedDigest != null) {
        expectedDigest.release();
      }
    }
    return decryptedPayload.touch();
  }

  private String toBase64(ByteBuf byteBuf) {
    ByteBuf encoded = Base64.encode(byteBuf, false, Base64Dialect.STANDARD);
    try {
      return encoded.toString(CharsetUtil.UTF_8);
    } finally {
      encoded.release();
    }
  }

  private ByteBuf fromBase64(ByteBufAllocator bufferAllocator, String string) {
    ByteBuf byteBuf = ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(string), CharsetUtil.UTF_8);
    try {
      return Base64.decode(byteBuf, Base64Dialect.STANDARD);
    } finally {
      byteBuf.release();
    }
  }

  private static class CookieStorage {
    private ImmutableList<Cookie> lastAccessToken;
    private ImmutableList<Cookie> data;

    public CookieStorage(ImmutableList<Cookie> lastAccessToken, ImmutableList<Cookie> data) {
      this.lastAccessToken = lastAccessToken;
      this.data = data;
    }

    void clear() {
      this.lastAccessToken = ImmutableList.of();
      this.data = ImmutableList.of();
    }
  }

  private CookieStorage getCookieStorage() {
    Request request = this.request.get();
    Optional<CookieStorage> cookieStorageOpt = request.maybeGet(CookieStorage.class);
    CookieStorage cookieStorage;
    if (cookieStorageOpt.isPresent()) {
      cookieStorage = cookieStorageOpt.get();
    } else {
      cookieStorage = new CookieStorage(
        readCookies(latCookieOrdering, request),
        readCookies(dataCookieOrdering, request)
      );
      request.add(CookieStorage.class, cookieStorage);
    }

    return cookieStorage;
  }

  private ImmutableList<Cookie> readCookies(CookieOrdering cookieOrdering, Request request) {
    Iterable<Cookie> iterable = Iterables.filter(request.getCookies(), c -> c.name().startsWith(cookieOrdering.prefix));
    return cookieOrdering.immutableSortedCopy(iterable);
  }

  private void invalidateCookie(Cookie cookie) {
    invalidateCookie(cookie.name());
  }

  private void invalidateCookie(String name) {
    Cookie cookie = responseCookie(name);
    cookie.setValue("");
    cookie.setMaxAge(0);
  }

  private void setCookie(String name, String value) {
    Cookie cookie = responseCookie(name);
    cookie.setValue(value);
    if (expirySeconds > 0) {
      cookie.setMaxAge(expirySeconds);
    }
  }

  private Cookie responseCookie(String name) {
    Response r = response.get();
    return r.getCookies()
      .stream()
      .filter(c -> c.name().equals(name))
      .findFirst()
      .orElseGet(() -> {
        Cookie createdCookie = r.cookie(name, "");
        if (cookieConfig.getPath() != null) {
          createdCookie.setPath(cookieConfig.getPath());
        }
        if (cookieConfig.getDomain() != null) {
          createdCookie.setDomain(cookieConfig.getDomain());
        }
        createdCookie.setHttpOnly(cookieConfig.isHttpOnly());
        createdCookie.setSecure(cookieConfig.isSecure());

        return createdCookie;
      });
  }
}
