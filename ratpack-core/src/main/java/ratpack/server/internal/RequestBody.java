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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import ratpack.exec.Downstream;
import ratpack.exec.Promise;
import ratpack.http.RequestBodyAlreadyReadException;

import java.util.ArrayList;
import java.util.List;

public class RequestBody implements RequestBodyReader, RequestBodyAccumulator {

  private final List<ByteBuf> byteBufs = new ArrayList<>();
  private final ChannelHandlerContext ctx;

  private boolean read;
  private boolean done;
  private int maxContentLength = -1;
  private int length;
  private Downstream<? super ByteBuf> downstream;
  private ByteBuf compositeBuffer;

  public RequestBody(ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void add(HttpContent httpContent) {
    if (httpContent != LastHttpContent.EMPTY_LAST_CONTENT) {
      ByteBuf byteBuf = httpContent.content();
      length += byteBuf.readableBytes();
      if (maxContentLength > 0 && maxContentLength < length) {
        assert downstream != null;
        tooLarge(downstream);
        return;
      }
      byteBufs.add(byteBuf);
    }
    if (httpContent instanceof LastHttpContent) {
      if (downstream != null) {
        complete(downstream);
      } else {
        done = true;
      }
    } else if (downstream != null) {
      ctx.read();
    }
  }

  @Override
  public void close() {
    if (compositeBuffer == null) {
      //noinspection Convert2streamapi
      for (ByteBuf byteBuf : byteBufs) {
        byteBuf.release();
      }
      byteBufs.clear();
    } else {
      if (compositeBuffer.refCnt() > 0) {
        compositeBuffer.release();
      }
    }
  }

  private void tooLarge(Downstream<? super ByteBuf> downstream) {
    close();
    NettyHandlerAdapter.sendError(ctx, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
    downstream.complete();
  }

  private void complete(Downstream<? super ByteBuf> downstream) {
    if (byteBufs.isEmpty()) {
      downstream.success(Unpooled.EMPTY_BUFFER);
    } else {
      ByteBuf[] byteBufsArray = this.byteBufs.toArray(new ByteBuf[this.byteBufs.size()]);
      byteBufs.clear();
      compositeBuffer = Unpooled.unmodifiableBuffer(byteBufsArray);
      downstream.success(compositeBuffer);
    }
  }

  @Override
  public Promise<ByteBuf> read(int maxContentLength) {
    return Promise.<ByteBuf>of(downstream -> {
      if (read) {
        downstream.error(new RequestBodyAlreadyReadException());
        return;
      }
      read = true;

      if (length > maxContentLength) {
        tooLarge(downstream);
      } else if (done) {
        complete(downstream);
      } else {
        this.maxContentLength = maxContentLength;
        this.downstream = downstream;
        ctx.read();
      }
    });
  }
}
