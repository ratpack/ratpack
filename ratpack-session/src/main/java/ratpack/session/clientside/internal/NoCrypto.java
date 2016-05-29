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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ratpack.session.clientside.Crypto;

public class NoCrypto implements Crypto {

  public static final Crypto INSTANCE = new NoCrypto();

  private NoCrypto() {
  }

  @Override
  public ByteBuf encrypt(ByteBuf message, ByteBufAllocator allocator) {
    return passthrough(message);
  }

  private ByteBuf passthrough(ByteBuf message) {
    return message.retain().slice().asReadOnly();
  }

  @Override
  public ByteBuf decrypt(ByteBuf message, ByteBufAllocator allocator) {
    return passthrough(message);
  }
}
