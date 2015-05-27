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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import ratpack.registry.Registry;
import ratpack.session.clientside.ValueSerializer;

import java.io.*;
import java.nio.CharBuffer;
import java.util.Base64;

/**
 * Serializes values of cookie session entries from java {@code Object} to byte buffer,
 * and deserializes them from {@code base64} encoded string (part of url query path) to java {@code Object}.
 */
public class JavaValueSerializer implements ValueSerializer {
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  @Override
  public ByteBuf serialize(Registry registry, ByteBufAllocator bufferAllocator, Object value) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    ObjectOutputStream outputStream = new ObjectOutputStream(stream);
    outputStream.writeObject(value);
    outputStream.close();
    byte[] bytes = stream.toByteArray();
    String encoded = ENCODER.encodeToString(bytes);
    return ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(encoded), CharsetUtil.UTF_8);
  }

  @Override
  public Object deserialize(Registry registry, String value) throws Exception {
    byte[] bytes = DECODER.decode(value);
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    ObjectInputStream inputStream = new ObjectInputStream(stream);
    Object obj = inputStream.readObject();
    inputStream.close();
    return obj;
  }
}
