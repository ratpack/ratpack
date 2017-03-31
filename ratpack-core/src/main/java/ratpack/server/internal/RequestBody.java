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
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.http.ConnectionClosedException;
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

  private long maxContentLength = -1;
  private Block onTooLarge;
  private long length;
  private Downstream<? super ByteBuf> downstream;
  private boolean receivedLast;
  private Consumer<? super HttpContent> onAdd;
  private State state = State.UNREAD;

  public RequestBody(long advertisedLength, HttpRequest request, ChannelHandlerContext ctx) {
    this.advertisedLength = advertisedLength;
    this.request = request;
    this.ctx = ctx;
  }

  @Override
  public void add(HttpContent httpContent) {
    if (state == State.READ) {
      httpContent.release();
    } else if (onAdd == null) {
      ByteBuf byteBuf = httpContent.content().touch();
      int readableBytes = byteBuf.readableBytes();
      if (readableBytes > 0) {
        length += readableBytes;
        byteBufs.add(byteBuf);
      } else {
        byteBuf.release();
      }
      if (httpContent instanceof LastHttpContent) {
        receivedLast = true;
        if (downstream != null) {
          complete(downstream);
        }
      } else if (downstream != null) {
        ctx.read();
      }
    } else {
      onAdd.accept(httpContent.touch());
    }
  }

  @Override
  public State getState() {
    return state;
  }

  public boolean isComplete() {
    return state == State.READ;
  }

  @Override
  public void close() {
    //noinspection Convert2streamapi
    for (ByteBuf byteBuf : byteBufs) {
      byteBuf.release();
    }
    byteBufs.clear();
  }

  public void forceCloseConnection() {
    close();
    ctx.channel().attr(DefaultResponseTransmitter.ATTRIBUTE_KEY).get().forceCloseConnection();
  }

  private void tooLarge(Downstream<? super ByteBuf> downstream) {
    state = State.READ;
    forceCloseConnection();
    try {
      onTooLarge.execute();
      downstream.complete();
    } catch (Throwable t) {
      downstream.error(t);
    }
  }

  private void complete(Downstream<? super ByteBuf> downstream) {
    state = State.READ;

    if (byteBufs.isEmpty()) {
      downstream.success(Unpooled.EMPTY_BUFFER);
    } else {
      downstream.success(composeReceived());
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
  public TransformablePublisher<ByteBuf> readStream() {
    return new BufferingPublisher<ByteBuf>(ByteBuf::release, write -> {
      if (state != State.UNREAD) {
        throw new RequestBodyAlreadyReadException();
      }

      state = State.READING;

      if (advertisedLength > maxContentLength || length > maxContentLength) {
        forceCloseConnection();
        state = State.READ;
        throw new RequestBodyTooLargeException(maxContentLength, Math.max(advertisedLength, length));
      }

      ctx.channel().config().setAutoRead(false);

      return new Subscription() {
        boolean autoRead;
        boolean unexpectedCloseHandled;

        @Override
        public void request(long n) {
          if (!unexpectedCloseHandled) {
            ctx.channel().closeFuture().addListener(f1 -> {
              if (!done) {
                cancel();
                write.error(new ConnectionClosedException("The connection closed unexpectedly"));
              }
            });
            unexpectedCloseHandled = true;
          }
          if (onAdd == null) {
            ByteBuf alreadyReceived = composeReceived();
            if (alreadyReceived.readableBytes() > 0) {
              write.item(alreadyReceived);
            } else {
              alreadyReceived.release();
            }
            if (receivedLast) {
              state = State.READ;
              write.complete();
              return;
            } else {
              onAdd = httpContent -> {
                if (httpContent != LastHttpContent.EMPTY_LAST_CONTENT) {
                  ByteBuf byteBuf = httpContent.content().touch();
                  length += byteBuf.readableBytes();
                  if (maxContentLength > 0 && maxContentLength < length) {
                    byteBuf.release();
                    state = State.READ;
                    forceCloseConnection();
                    write.error(new RequestBodyTooLargeException(maxContentLength, length));
                    return;
                  }

                  write.item(byteBuf);
                }
                if (httpContent instanceof LastHttpContent) {
                  state = State.READ;
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
    }).bindExec(ByteBuf::release);
  }

  public void discard(Consumer<? super Throwable> resume) {
    close();

    if (state != State.UNREAD) {
      throw new RequestBodyAlreadyReadException();
    }

    state = State.READING;

    if (advertisedLength > maxContentLength || length > maxContentLength) {
      state = State.READ;
      forceCloseConnection();
      resume.accept(new RequestBodyTooLargeException(maxContentLength, advertisedLength));
    } else if (receivedLast) {
      state = State.READ;
      resume.accept(null);
    } else {
      onAdd = httpContent -> {
        httpContent.release();
        if ((length += httpContent.content().readableBytes()) > maxContentLength) {
          state = State.READ;
          resume.accept(new RequestBodyTooLargeException(maxContentLength, length));
        } else if (httpContent instanceof LastHttpContent) {
          state = State.READ;
          resume.accept(null);
        } else {
          ctx.read();
        }
      };
      ctx.read();
    }

  }

  @Override
  public long getContentLength() {
    return advertisedLength;
  }

  @Override
  public void setMaxContentLength(long maxContentLength) {
    this.maxContentLength = maxContentLength;
  }

  @Override
  public long getMaxContentLength() {
    return maxContentLength;
  }

  @Override
  public Promise<ByteBuf> read(Block onTooLarge) {
    return Promise.<ByteBuf>async(downstream -> {
      if (state != State.UNREAD) {
        downstream.error(new RequestBodyAlreadyReadException());
        return;
      }
      state = State.READING;
      this.onTooLarge = onTooLarge;

      if (advertisedLength > maxContentLength || length > maxContentLength) {
        tooLarge(downstream);
      } else if (receivedLast) {
        complete(downstream);
      } else {
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
    }).map(byteBuf -> {
      Execution.current().onComplete(() -> {
        if (byteBuf.refCnt() > 0) {
          byteBuf.release();
        }
      });
      return byteBuf;
    });
  }

}
