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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.util.internal.RecyclableArrayList;

import java.util.List;

public class HttpRequestHolderDecoder extends HttpRequestDecoder {
  private volatile HttpRequest firstMessage;
  private ByteBuf byteBuf;

  public HttpRequestHolderDecoder() {
  }

  public HttpRequestHolderDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize);
  }

  public HttpRequestHolderDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders) {
    super(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf && byteBuf == null) {
      byteBuf = ((ByteBuf) msg).retain();
      super.channelRead(ctx, msg);
    } else if (msg instanceof HttpRequest) {
      firstMessage = (HttpRequest) msg;
//      byteBuf.release();
      super.channelRead(ctx, byteBuf);
      byteBuf = null;
    } else {
      super.channelRead(ctx, msg);
    }
  }

  @Override
  protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (firstMessage != null && out.isEmpty()) {
      try {
        if (out instanceof RecyclableArrayList) {
          ((RecyclableArrayList) out).recycle();
        }
        RecyclableArrayList out2 = RecyclableArrayList.newInstance();
        out2.add(firstMessage);
        super.callDecode(ctx, in, out2);
      } finally {
        firstMessage = null;
      }
    } else {
      super.callDecode(ctx, in, out);
    }
  }
}
