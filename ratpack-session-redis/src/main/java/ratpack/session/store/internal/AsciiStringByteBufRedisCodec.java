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

package ratpack.session.store.internal;

import com.lambdaworks.redis.codec.RedisCodec;
import io.netty.buffer.*;
import io.netty.util.AsciiString;

import java.nio.ByteBuffer;

public class AsciiStringByteBufRedisCodec implements RedisCodec<AsciiString, ByteBuf> {

  @Override
  public AsciiString decodeKey(ByteBuffer bytes) {
    return new AsciiString(bytes);
  }

  @Override
  public ByteBuf decodeValue(ByteBuffer bytes) {
    return Unpooled.wrappedBuffer(bytes);
  }

  @Override
  public ByteBuffer encodeKey(AsciiString key) {
    return ByteBuffer.wrap(key.array());
  }

  @Override
  public ByteBuffer encodeValue(ByteBuf value) {
    return value.nioBuffer();
  }
}
