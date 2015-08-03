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

package ratpack.server.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequestDecoder;

import java.util.List;

public class HttpRequestHolderDecoder extends HttpRequestDecoder implements ByteBufHolder {
  private ByteBufHolder holder;

  public HttpRequestHolderDecoder() {
  }

  public HttpRequestHolderDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize);
  }

  public HttpRequestHolderDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders) {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders);
  }

  @Override
  public ByteBuf content() {
    return holder.content();
  }

  @Override
  public ByteBufHolder copy() {
    return holder.copy();
  }

  @Override
  public ByteBufHolder duplicate() {
    return holder.duplicate();
  }

  @Override
  public int refCnt() {
    return holder.refCnt();
  }

  @Override
  public ByteBufHolder retain() {
    return holder.retain();
  }

  @Override
  public ByteBufHolder retain(int increment) {
    return holder.retain(increment);
  }

  @Override
  public ByteBufHolder touch() {
    return holder.touch();
  }

  @Override
  public ByteBufHolder touch(Object hint) {
    return holder.touch(hint);
  }

  @Override
  public boolean release() {
    return holder.release();
  }

  @Override
  public boolean release(int decrement) {
    return holder.release(decrement);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
    if (holder == null || buffer.compareTo(holder.content()) != 0) {
      holder = new DefaultByteBufHolder(buffer.copy());
    }
    super.decode(ctx, buffer, out);
  }
}
