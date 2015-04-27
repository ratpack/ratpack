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
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import ratpack.session.clientside.Crypto;
import ratpack.session.clientside.SessionService;
import ratpack.session.clientside.Signer;
import ratpack.util.Exceptions;

import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultClientSessionService implements SessionService {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
  private static final Escaper ESCAPER = UrlEscapers.urlFormParameterEscaper();

  private static final ByteBuf EQUALS = Unpooled.unreleasableBuffer(ByteBufUtil.encodeString(UnpooledByteBufAllocator.DEFAULT, CharBuffer.wrap("="), CharsetUtil.UTF_8));
  private static final ByteBuf AMPERSAND = Unpooled.unreleasableBuffer(ByteBufUtil.encodeString(UnpooledByteBufAllocator.DEFAULT, CharBuffer.wrap("&"), CharsetUtil.UTF_8));

  private static final String SESSION_SEPARATOR = ":";

  private final Signer signer;
  private final Crypto crypto;

  public DefaultClientSessionService(Signer signer, Crypto crypto) {
    this.signer = signer;
    this.crypto = crypto;
  }

  @Override
  public String serializeSession(ByteBufAllocator bufferAllocator, Set<Map.Entry<String, Object>> entries) {
    ByteBuf[] buffers = new ByteBuf[3 * entries.size() + entries.size() - 1];
    try {
      int i = 0;

      for (Map.Entry<String, Object> entry : entries) {
        buffers[i++] = encode(bufferAllocator, entry.getKey());
        buffers[i++] = EQUALS;
        buffers[i++] = encode(bufferAllocator, entry.getValue().toString());

        if (i < buffers.length) {
          buffers[i++] = AMPERSAND;
        }
      }

      ByteBuf payloadBuffer = Unpooled.wrappedBuffer(buffers.length, buffers);
      byte[] payloadBytes = new byte[payloadBuffer.readableBytes()];
      payloadBuffer.getBytes(0, payloadBytes);
      if (crypto != null) {
        payloadBytes = crypto.encrypt(payloadBuffer);
        payloadBuffer = Unpooled.wrappedBuffer(payloadBytes);
      }

      String payloadString = ENCODER.encodeToString(payloadBytes);

      byte[] digest = signer.sign(payloadBuffer);
      String digestString = ENCODER.encodeToString(digest);

      return payloadString + SESSION_SEPARATOR + digestString;

    } finally {
      for (ByteBuf buffer : buffers) {
        if (buffer != null) {
          buffer.release();
        }
      }
    }
  }

  private ByteBuf encode(ByteBufAllocator bufferAllocator, String value) {
    String escaped = ESCAPER.escape(value);
    return ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(escaped), CharsetUtil.UTF_8);
  }

  @Override
  public ConcurrentMap<String, Object> deserializeSession(Cookie cookie) {
    ConcurrentMap<String, Object> sessionStorage = new ConcurrentHashMap<>();

    String encodedPairs = cookie == null ? null : cookie.value();
    if (encodedPairs != null) {
      String[] parts = encodedPairs.split(SESSION_SEPARATOR);
      if (parts.length == 2) {
        byte[] urlEncoded = DECODER.decode(parts[0]);
        byte[] digest = DECODER.decode(parts[1]);

        try {
          byte[] expectedDigest = signer.sign(Unpooled.wrappedBuffer(urlEncoded));

          if (Arrays.equals(digest, expectedDigest)) {
            byte[] message;
            if (crypto == null) {
              message = urlEncoded;
            } else {
              message = crypto.decrypt(Unpooled.wrappedBuffer(urlEncoded));
            }

            String payload = new String(message, CharsetUtil.UTF_8);
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(payload, CharsetUtil.UTF_8, false);
            Map<String, List<String>> decoded = queryStringDecoder.parameters();
            for (Map.Entry<String, List<String>> entry : decoded.entrySet()) {
              String value = entry.getValue().isEmpty() ? null : entry.getValue().get(0);
              sessionStorage.put(entry.getKey(), value);
            }
          }
        } catch (Exception e) {
          throw Exceptions.uncheck(e);
        }
      }
    }

    return sessionStorage;
  }
}
