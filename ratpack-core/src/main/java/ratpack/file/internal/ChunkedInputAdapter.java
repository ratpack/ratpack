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

package ratpack.file.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;

/**
 * Adapted from https://github.com/scireum/sirius/blob/develop/web/src/sirius/web/http/ChunkedInputAdapter.java
 *
 * Credit to Andreas Haufler (aha@scireum.de).
 */
public class ChunkedInputAdapter implements ChunkedInput<HttpContent> {

  private ChunkedInput<ByteBuf> input;

  /**
   * Creates a new adapter for the given input
   *
   * @param input the chunked input to retrieve chunks from
   */
  public ChunkedInputAdapter(ChunkedInput<ByteBuf> input) {
    this.input = input;
  }

  @Override
  public boolean isEndOfInput() throws Exception {
    return input.isEndOfInput();
  }

  @Override
  public void close() throws Exception {
    input.close();
  }

  @Override
  public HttpContent readChunk(ChannelHandlerContext ctx) throws Exception {
    ByteBuf bb = input.readChunk(ctx);
    if (bb == null || bb.readableBytes() == 0) {
      return LastHttpContent.EMPTY_LAST_CONTENT;
    }
    return new DefaultHttpContent(bb);
  }
}