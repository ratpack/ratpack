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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import ratpack.session.clientside.Signer;
import ratpack.util.Exceptions;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class DefaultSigner implements Signer {

  private final SecretKeySpec secretKeySpec;

  public DefaultSigner(SecretKeySpec secretKeySpec) {
    this.secretKeySpec = secretKeySpec;
  }

  @Override
  public ByteBuf sign(ByteBuf message, ByteBufAllocator byteBufAllocator) {
    return Exceptions.uncheck(() -> {
      Mac mac = Mac.getInstance(secretKeySpec.getAlgorithm());
      mac.init(secretKeySpec);
      mac.update(message.nioBuffer());
      return Unpooled.wrappedBuffer(mac.doFinal());
    });
  }

}
