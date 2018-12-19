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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.reactivestreams.Subscription;
import ratpack.bytebuf.ByteBufRef;
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

  private static final HttpResponse CONTINUE_RESPONSE = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);

  enum State {
    UNREAD, READING, READ
  }

  private final List<ByteBuf> received = new ArrayList<>();
  private final long advertisedLength;
  private final HttpRequest request;
  private final ChannelHandlerContext ctx;

  private long maxContentLength = -1;
  private long receivedLength;

  private boolean receivedLast;
  private boolean earlyClose;
  private State state = State.UNREAD;

  private Listener listener;

  private final GenericFutureListener<Future<Void>> closeListener = v -> {
    if (listener == null) {
      earlyClose = true;
    } else {
      listener.onEarlyClose();
    }
  };

  interface Listener {
    void onContent(HttpContent httpContent);

    void onEarlyClose();
  }

  public RequestBody(long advertisedLength, HttpRequest request, ChannelHandlerContext ctx) {
    this.advertisedLength = advertisedLength;
    this.request = request;
    this.ctx = ctx;

    ctx.channel().closeFuture().addListener(closeListener);
  }

  @Override
  public Promise<ByteBuf> read(Block onTooLarge) {
    return Promise.<ByteBuf>async(downstream -> {
      if (state != State.UNREAD) {
        downstream.error(new RequestBodyAlreadyReadException());
        return;
      }
      state = State.READING;

      if (isExceedsMaxContentLength(advertisedLength)) {
        tooLarge(onTooLarge, advertisedLength, downstream);
      } else if (isExceedsMaxContentLength(receivedLength)) {
        tooLarge(onTooLarge, receivedLength, downstream);
      } else if (receivedLast) {
        complete(downstream);
      } else if (earlyClose) {
        discard();
        downstream.error(closedException());
      } else {
        this.listener = new Listener() {
          @Override
          public void onContent(HttpContent httpContent) {
            addToReceived(httpContent);
            if (isExceedsMaxContentLength(receivedLength)) {
              tooLarge(onTooLarge, receivedLength, downstream);
            } else if (httpContent instanceof LastHttpContent) {
              listener = null;
              complete(downstream);
            } else {
              ctx.channel().read();
            }
          }

          @Override
          public void onEarlyClose() {
            discard();
            listener = null;
            downstream.error(closedException());
          }
        };

        startBodyRead(e -> {
          discard();
          downstream.error(e);
        });
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

  private void tooLarge(Block onTooLarge, long length, Downstream<? super ByteBuf> downstream) {
    discard();
    if (onTooLarge == RequestBodyReader.DEFAULT_TOO_LARGE_SENTINEL) {
      downstream.error(tooLargeException(length));
    } else {
      try {
        onTooLarge.execute();
      } catch (Throwable t) {
        downstream.error(t);
        return;
      }
      downstream.complete();
    }
  }

  private boolean isExceedsMaxContentLength(long contentLength) {
    return maxContentLength > 0 && contentLength > 0 && contentLength > maxContentLength;
  }

  private void startBodyRead(Consumer<? super Throwable> errorHandler) {
    if (isContinueExpected()) {
      ctx.writeAndFlush(CONTINUE_RESPONSE).addListener(future -> {
        if (future.isSuccess()) {
          ctx.read();
        } else {
          errorHandler.accept(future.cause());
        }
      });
    } else {
      ctx.read();
    }
  }

  @Override
  public TransformablePublisher<ByteBuf> readStream() {
    return new BufferingPublisher<ByteBuf>(ByteBuf::release, write -> {
      if (state != State.UNREAD) {
        throw new RequestBodyAlreadyReadException();
      }

      state = State.READING;

      if (isExceedsMaxContentLength(advertisedLength) || isExceedsMaxContentLength(receivedLength)) {
        discard();
        throw tooLargeException(Math.max(advertisedLength, receivedLength));
      }

      return new Subscription() {
        @Override
        public void request(long n) {
          if (listener == null) {
            ByteBuf alreadyReceived = composeReceived();
            if (alreadyReceived.readableBytes() > 0) {
              write.item(alreadyReceived);
            } else {
              alreadyReceived.release();
            }
            if (receivedLast) {
              state = State.READ;
              write.complete();
            } else {
              listener = new Listener() {
                @Override
                public void onContent(HttpContent httpContent) {
                  ByteBuf byteBuf = httpContent.content().touch();
                  int readableBytes = byteBuf.readableBytes();
                  if (readableBytes > 0) {
                    receivedLength += readableBytes;
                    if (isExceedsMaxContentLength(receivedLength)) {
                      byteBuf.release();
                      discard();
                      listener = null;
                      write.error(tooLargeException(RequestBody.this.receivedLength));
                      return;
                    } else {
                      write.item(byteBuf);
                    }
                  } else {
                    byteBuf.release();
                  }

                  if (httpContent instanceof LastHttpContent) {
                    state = State.READ;
                    listener = null;
                    write.complete();
                  } else if (write.getRequested() > 0) {
                    ctx.channel().read();
                  }
                }

                @Override
                public void onEarlyClose() {
                  discard();
                  listener = null;
                  write.error(closedException());
                }
              };

              if (earlyClose) {
                listener.onEarlyClose();
              } else {
                startBodyRead(e -> {
                  discard();
                  write.error(e);
                });
              }
            }
          } else {
            ctx.read();
          }
        }

        @Override
        public void cancel() {
          discard();
        }
      };
    }).bindExec(ByteBuf::release);
  }

  private RequestBodyTooLargeException tooLargeException(long receivedLength) {
    return new RequestBodyTooLargeException(maxContentLength, receivedLength);
  }

  private ConnectionClosedException closedException() {
    return new ConnectionClosedException(ConnectionClosureReason.get(ctx.channel()));
  }

  @Override
  public void add(HttpContent httpContent) {
    if (state == State.READ) {
      httpContent.release();
    } else {
      if (httpContent instanceof LastHttpContent) {
        ctx.channel().closeFuture().removeListener(closeListener);
        receivedLast = true;
      }

      if (listener == null) {
        addToReceived(httpContent);
      } else {
        listener.onContent(httpContent);
      }
    }
  }

  private void addToReceived(HttpContent httpContent) {
    ByteBuf byteBuf = httpContent.content().touch();
    int readableBytes = byteBuf.readableBytes();
    if (readableBytes > 0) {
      receivedLength += readableBytes;
      received.add(byteBuf);
    } else {
      byteBuf.release();
    }
  }

  public boolean isUnread() {
    return state == State.UNREAD;
  }

  private void release() {
    received.forEach(ByteBuf::release);
    received.clear();
  }

  private void discard() {
    state = State.READ;
    release();
    ctx.channel().attr(DefaultResponseTransmitter.ATTRIBUTE_KEY).get().forceCloseConnection();
  }

  private void complete(Downstream<? super ByteBuf> downstream) {
    state = State.READ;
    if (received.isEmpty()) {
      downstream.success(Unpooled.EMPTY_BUFFER);
    } else {
      downstream.success(composeReceived());
    }
  }

  private ByteBuf composeReceived() {
    if (received.isEmpty()) {
      return Unpooled.EMPTY_BUFFER;
    } else if (received.size() == 1) {
      return new ByteBufRef(received.remove(0));
    } else {
      ByteBuf[] byteBufsArray = this.received.toArray(new ByteBuf[0]);
      received.clear();
      return Unpooled.wrappedUnmodifiableBuffer(byteBufsArray);
    }
  }

  public void drain(Consumer<? super Throwable> resume) {
    release();

    if (!isUnread()) {
      throw new RequestBodyAlreadyReadException();
    }

    state = State.READING;
    if (earlyClose) {
      discard();
      resume.accept(null);
    } else if (advertisedLength > maxContentLength || receivedLength > maxContentLength) {
      discard();
      resume.accept(tooLargeException(advertisedLength));
    } else if (receivedLast || isContinueExpected()) {
      release(); // don't close connection, we can reuse
      state = State.READ;
      resume.accept(null);
    } else {
      listener = new Listener() {
        @Override
        public void onContent(HttpContent httpContent) {
          httpContent.release();
          if ((receivedLength += httpContent.content().readableBytes()) > maxContentLength) {
            state = State.READ;
            listener = null;
            resume.accept(tooLargeException(RequestBody.this.receivedLength));
          } else if (httpContent instanceof LastHttpContent) {
            state = State.READ;
            listener = null;
            resume.accept(null);
          } else {
            ctx.read();
          }
        }

        @Override
        public void onEarlyClose() {
          resume.accept(null);
        }
      };

      // Don't use startBodyRead as we don't want to issue continue
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

  private boolean isContinueExpected() {
    return HttpUtil.is100ContinueExpected(this.request);
  }

}
