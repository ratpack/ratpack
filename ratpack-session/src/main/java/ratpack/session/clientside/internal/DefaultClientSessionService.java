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

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import io.netty.buffer.*;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import ratpack.session.clientside.Crypto;
import ratpack.session.clientside.SessionService;
import ratpack.session.clientside.Signer;
import ratpack.util.Exceptions;

import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultClientSessionService implements SessionService {

  private static final Escaper ESCAPER = UrlEscapers.urlFormParameterEscaper();

  private static final ByteBuf EQUALS = Unpooled.unreleasableBuffer(ByteBufUtil.encodeString(UnpooledByteBufAllocator.DEFAULT, CharBuffer.wrap("="), CharsetUtil.UTF_8));
  private static final ByteBuf AMPERSAND = Unpooled.unreleasableBuffer(ByteBufUtil.encodeString(UnpooledByteBufAllocator.DEFAULT, CharBuffer.wrap("&"), CharsetUtil.UTF_8));

  private static final String SESSION_SEPARATOR = ":";

  private final ByteBufAllocator bufferAllocator;
  private final Signer signer;
  private final Crypto crypto;

  public DefaultClientSessionService(ByteBufAllocator bufferAllocator, Signer signer, Crypto crypto) {
    this.bufferAllocator = bufferAllocator;
    this.signer = signer;
    this.crypto = crypto;
  }

  @Override
  public String[] serializeSession(Set<Map.Entry<String, Object>> entries, int maxCookieSize) {
    String serializedSession = serializeSession(entries);
    int sessionSize = serializedSession.length();
    if (sessionSize <= maxCookieSize) {
      return new String[]{serializedSession};
    }
    int numOfPartitions = (int) Math.ceil((double) sessionSize / maxCookieSize);
    String[] partitions = new String[numOfPartitions];
    for (int i = 0; i < numOfPartitions; i++) {
      int from = i * maxCookieSize;
      int to = Math.min(from + maxCookieSize, sessionSize);
      partitions[i] = serializedSession.substring(from, to);
    }
    return partitions;
  }

  @Override
  public String serializeSession(Set<Map.Entry<String, Object>> entries) {
    ByteBuf[] buffers = new ByteBuf[3 * entries.size() + entries.size() - 1];
    try {
      int i = 0;

      for (Map.Entry<String, Object> entry : entries) {
        buffers[i++] = encode(entry.getKey());
        buffers[i++] = EQUALS;
        buffers[i++] = encode(entry.getValue().toString());

        if (i < buffers.length) {
          buffers[i++] = AMPERSAND;
        }
      }

      ByteBuf payloadBuffer = Unpooled.wrappedBuffer(buffers.length, buffers);

      ByteBuf encrypted = crypto.encrypt(payloadBuffer, bufferAllocator);
      String encryptedBase64 = toBase64(encrypted);
      ByteBuf digest = signer.sign(encrypted.resetReaderIndex(), bufferAllocator);
      String digestBase64 = toBase64(digest);

      digest.release();
      encrypted.release();

      return encryptedBase64 + SESSION_SEPARATOR + digestBase64;
    } finally {
      for (ByteBuf buffer : buffers) {
        if (buffer != null) {
          buffer.release();
        }
      }
    }
  }

  private String toBase64(ByteBuf byteBuf) {
    ByteBuf encoded = Base64.encode(byteBuf, false, Base64Dialect.STANDARD);
    try {
      return encoded.toString(CharsetUtil.ISO_8859_1);
    } finally {
      encoded.release();
    }
  }

  private ByteBuf fromBase64(String string) {
    ByteBuf byteBuf = ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(string), CharsetUtil.ISO_8859_1);
    try {
      return Base64.decode(byteBuf, Base64Dialect.STANDARD);
    } finally {
      byteBuf.release();
    }
  }

  private ByteBuf encode(String value) {
    String escaped = ESCAPER.escape(value);
    return ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(escaped), CharsetUtil.UTF_8);
  }

  @Override
  public ConcurrentMap<String, Object> deserializeSession(Cookie[] sessionCookies) {
    // assume table is sorted
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < sessionCookies.length; i++) {
      sb.append(sessionCookies[i].value());
    }
    return deserializeSession(sb.toString());
  }

  private ConcurrentMap<String, Object> deserializeSession(String cookieValue) {
    ConcurrentMap<String, Object> sessionStorage = new ConcurrentHashMap<>();
    if (cookieValue != null) {
      String[] parts = cookieValue.split(SESSION_SEPARATOR);
      if (parts.length == 2) {
        ByteBuf payload = fromBase64(parts[0]);
        ByteBuf digest = fromBase64(parts[1]);

        try {
          ByteBuf expectedDigest = signer.sign(payload, bufferAllocator);
          if (ByteBufUtil.equals(digest, expectedDigest)) {
            ByteBuf decryptedPayload = crypto.decrypt(payload.resetReaderIndex(), bufferAllocator);
            String payloadString = decryptedPayload.toString(CharsetUtil.UTF_8);
            decryptedPayload.release();

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(payloadString, CharsetUtil.UTF_8, false);
            Map<String, List<String>> decoded = queryStringDecoder.parameters();
            for (Map.Entry<String, List<String>> entry : decoded.entrySet()) {
              String value = entry.getValue().isEmpty() ? null : entry.getValue().get(0);
              sessionStorage.put(entry.getKey(), value);
            }
          }
        } catch (Exception e) {
          throw Exceptions.uncheck(e);
        } finally {
          payload.release();
          digest.release();
        }
      }
    }

    return sessionStorage;
  }
}
