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

package ratpack.session.clientside.serializer;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import ratpack.registry.Registry;
import ratpack.session.clientside.ValueSerializer;

import java.nio.CharBuffer;
import java.util.Objects;

/**
 * Serializes values of cookie session entries from String to byte buffer,
 * and deserializes them from {@code base64} encoded String to decoded String.
 */
public class StringValueSerializer implements ValueSerializer {
  private static final Escaper ESCAPER = UrlEscapers.urlFormParameterEscaper();

  @Override
  public ByteBuf serialize(Registry registry, ByteBufAllocator bufAllocator, Object value) throws Exception {
    Objects.requireNonNull(value);
    return encode(bufAllocator, value.toString());
  }

  @Override
  public Object deserialize(Registry registry, String value) throws Exception {
    return value == null || value.isEmpty() ? null : value;
  }

  private ByteBuf encode(ByteBufAllocator bufferAllocator, String value) {
    String escaped = ESCAPER.escape(value);
    return ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(escaped), CharsetUtil.UTF_8);
  }
}
