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
import io.netty.handler.codec.http.*;
import org.reactivestreams.Subscription;
import ratpack.exec.Downstream;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.http.RequestBodyAlreadyReadException;
import ratpack.http.RequestBodyTooLargeException;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.internal.BufferingPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RequestBody implements RequestBodyReader, RequestBodyAccumulator {

  private final List<ByteBuf> byteBufs = new ArrayList<>();
  private final long advertisedLength;
  private final HttpRequest request;
  private final ChannelHandlerContext ctx;

  private boolean read;
  private boolean done;
  private long maxContentLength = -1;
  private Block onTooLarge;
  private long length;
  private Downstream<? super ByteBuf> downstream;
  private ByteBuf compositeBuffer;

  private Consumer<? super HttpContent> onAdd;

  public RequestBody(long advertisedLength, HttpRequest request, ChannelHandlerContext ctx) {
    this.advertisedLength = advertisedLength;
    this.request = request;
    this.ctx = ctx;
  }

  @Override
  public void add(HttpContent httpContent) {
    if (onAdd == null) {
      if (httpContent != LastHttpContent.EMPTY_LAST_CONTENT) {
        ByteBuf byteBuf = httpContent.content().touch();
        length += byteBuf.readableBytes();
        if (maxContentLength > 0 && maxContentLength < length) {
          assert downstream != null;
          tooLarge(downstream);
          return;
        }
        byteBufs.add(byteBuf);
      }
      if (httpContent instanceof LastHttpContent) {
        done = true;
        if (downstream != null) {
          complete(downstream);
        }
      } else if (downstream != null) {
        ctx.read();
      }
    } else {
      onAdd.accept(httpContent);
    }
  }

  public boolean isComplete() {
    return done;
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

  public void forceCloseConnection() {
    close();
    ctx.channel().attr(DefaultResponseTransmitter.ATTRIBUTE_KEY).get().forceCloseConnection();
  }

  private void tooLarge(Downstream<? super ByteBuf> downstream) {
    forceCloseConnection();
    try {
      onTooLarge.execute();
      downstream.complete();
    } catch (Throwable t) {
      downstream.error(t);
    }
  }

  private void complete(Downstream<? super ByteBuf> downstream) {
    if (byteBufs.isEmpty()) {
      downstream.success(Unpooled.EMPTY_BUFFER);
    } else {
      compositeBuffer = composeReceived();
      downstream.success(compositeBuffer);
    }
  }

  private ByteBuf composeReceived() {
    if (byteBufs.isEmpty()) {
      return Unpooled.EMPTY_BUFFER;
    } else {
      ByteBuf[] byteBufsArray = this.byteBufs.toArray(new ByteBuf[this.byteBufs.size()]);
      byteBufs.clear();
      return Unpooled.unmodifiableBuffer(byteBufsArray);
    }
  }

  @Override
  public TransformablePublisher<ByteBuf> readStream(long maxContentLength) {
    return new BufferingPublisher<ByteBuf>(ByteBuf::release, write -> {
      if (read) {
        throw new RequestBodyAlreadyReadException();
      }

      read = true;
      RequestBody.this.maxContentLength = maxContentLength;

      if (advertisedLength > maxContentLength || length > maxContentLength) {
        forceCloseConnection();
        throw new RequestBodyTooLargeException(maxContentLength, Math.max(advertisedLength, length));
      }

      ctx.channel().config().setAutoRead(false);

      return new Subscription() {
        boolean autoRead;

        @Override
        public void request(long n) {
          if (onAdd == null) {
            ByteBuf alreadyReceived = composeReceived();
            if (alreadyReceived.readableBytes() > 0) {
              write.item(alreadyReceived);
            }
            if (done) {
              write.complete();
              return;
            } else {
              onAdd = httpContent -> {
                if (httpContent != LastHttpContent.EMPTY_LAST_CONTENT) {
                  ByteBuf byteBuf = httpContent.content().touch();
                  length += byteBuf.readableBytes();
                  if (maxContentLength > 0 && maxContentLength < length) {
                    byteBuf.release();
                    forceCloseConnection();
                    write.error(new RequestBodyTooLargeException(maxContentLength, length));
                    return;
                  }

                  write.item(byteBuf);
                }
                if (httpContent instanceof LastHttpContent) {
                  done = true;
                  ctx.channel().config().setAutoRead(false);
                  write.complete();
                } else if (!autoRead && write.getRequested() > 0) {
                  ctx.channel().read();
                }
              };
            }
          }

          if (n == Long.MAX_VALUE) {
            ctx.channel().config().setAutoRead(true);
            autoRead = true;
          } else {
            ctx.channel().read();
          }
        }

        @Override
        public void cancel() {
          forceCloseConnection();
        }
      };
    }).bindExec();
  }

  @Override
  public long getContentLength() {
    return advertisedLength;
  }

  @Override
  public Promise<ByteBuf> read(long maxContentLength, Block onTooLarge) {
    return Promise.<ByteBuf>async(downstream -> {
      if (read) {
        downstream.error(new RequestBodyAlreadyReadException());
        return;
      }
      read = true;
      this.onTooLarge = onTooLarge;

      if (advertisedLength > maxContentLength || length > maxContentLength) {
        tooLarge(downstream);
      } else if (done) {
        complete(downstream);
      } else {
        this.maxContentLength = maxContentLength;
        this.downstream = downstream;
        if (HttpUtil.is100ContinueExpected(request)) {
          HttpResponse continueResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);

          ctx.writeAndFlush(continueResponse).addListener(future -> {
            if (!future.isSuccess()) {
              ctx.fireExceptionCaught(future.cause());
            }
          });
        }

        ctx.read();
      }
    });
  }

}
