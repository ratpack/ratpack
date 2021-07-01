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

package ratpack.server.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Action;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.handling.internal.DoubleTransmissionException;
import ratpack.http.Request;
import ratpack.http.SentResponse;
import ratpack.http.internal.*;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultResponseTransmitter implements ResponseTransmitter {

  static final AttributeKey<DefaultResponseTransmitter> ATTRIBUTE_KEY = AttributeKey.valueOf(DefaultResponseTransmitter.class.getName());

  private final static Logger LOGGER = LoggerFactory.getLogger(ResponseTransmitter.class);


  private static final Runnable NOOP_RUNNABLE = () -> {
  };
  private static final LastHttpContentResponseWriter EMPTY_BODY = new LastHttpContentResponseWriter(LastHttpContent.EMPTY_LAST_CONTENT);
  private static final DefaultHttpHeaders ERROR_RESPONSE_HEADERS = new DefaultHttpHeaders();

  static {
    ERROR_RESPONSE_HEADERS.set(HttpHeaderNames.CONTENT_LENGTH, 0);
    ERROR_RESPONSE_HEADERS.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
  }

  private final AtomicBoolean transmitted;
  private final Channel channel;
  private final Clock clock;
  private final Request ratpackRequest;
  private final HttpHeaders responseHeaders;
  private final RequestBody requestBody;
  private final boolean isSsl;
  private final HttpRequest nettyRequest;

  private List<Action<? super RequestOutcome>> outcomeListeners;

  private Instant stopTime;

  private Runnable onWritabilityChanged = NOOP_RUNNABLE;

  public DefaultResponseTransmitter(
    AtomicBoolean transmitted,
    Channel channel,
    Clock clock,
    HttpRequest nettyRequest,
    Request ratpackRequest,
    HttpHeaders responseHeaders,
    @Nullable RequestBody requestBody
  ) {
    this.transmitted = transmitted;
    this.channel = channel;
    this.clock = clock;
    this.ratpackRequest = ratpackRequest;
    this.responseHeaders = responseHeaders;
    this.requestBody = requestBody;
    this.nettyRequest = nettyRequest;
    this.isSsl = channel.pipeline().get(SslHandler.class) != null;
  }

  interface ResponseWriter {
    Promise<Boolean> write(Channel channel);

    default void dispose() {
    }
  }

  static class LastHttpContentResponseWriter implements ResponseWriter {
    private final LastHttpContent content;

    public LastHttpContentResponseWriter(LastHttpContent content) {
      this.content = content;
    }

    public Promise<Boolean> write(Channel channel) {
      return Promise.async(down ->
        channel.writeAndFlush(content).addListener(future -> down.success(future.isSuccess()))
      );
    }

    public void dispose() {
      content.release();
    }
  }

  private void sendResponse(HttpResponseStatus responseStatus, ResponseWriter bodyWriter) {
    if (transmitted.compareAndSet(false, true)) {
      stopTime = clock.instant();
      if (requestBody == null) {
        sendResponseAfterRequestBodyDrain(responseStatus, bodyWriter);
      } else {
        requestBody.drain()
          .onError(e -> {
            LOGGER.warn("An error occurred draining the unread request body. The connection will be closed", e);
            forceCloseWithResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
          })
          .then(outcome -> {
            if (outcome == RequestBody.DrainOutcome.TOO_LARGE) {
              bodyWriter.dispose();
              forceCloseWithResponse(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            } else {
              if (outcome != RequestBody.DrainOutcome.DRAINED) {
                throw new IllegalStateException("unhandled drain outcome");
              }
              sendResponseAfterRequestBodyDrain(responseStatus, bodyWriter);
            }
          });
      }
    } else {
      String msg = "attempt at double transmission for: " + ratpackRequest.getRawUri();
      LOGGER.warn(msg, new DoubleTransmissionException(msg));
      bodyWriter.dispose();
    }
  }

  private void sendResponseAfterRequestBodyDrain(HttpResponseStatus responseStatus, ResponseWriter bodyWriter) {
    try {
      boolean responseRequestedConnectionClose = responseHeaders.contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true);
      boolean requestRequestedConnectionClose = !HttpUtil.isKeepAlive(this.nettyRequest);

      boolean keepAlive = !requestRequestedConnectionClose && !responseRequestedConnectionClose;
      if (!keepAlive && !responseRequestedConnectionClose) {
        forceCloseConnection();
      }

      HttpResponse headersResponse = new CustomHttpResponse(responseStatus, responseHeaders);
      if (mustHaveBody(responseStatus) && keepAlive && HttpUtil.getContentLength(headersResponse, -1) == -1 && !HttpUtil.isTransferEncodingChunked(headersResponse)) {
        HttpUtil.setTransferEncodingChunked(headersResponse, true);
      }

      Promise.<Boolean>async(down ->
        channel.writeAndFlush(headersResponse)
          .addListener(future -> down.success(future.isSuccess()))
      )
        .then(success -> {
          if (success) {
            bodyWriter.write(channel)
              .onError(e -> {
                LOGGER.warn("Error from response body writer", e);
                channel.close();
              })
              .then(completed -> {
                if (channel.isOpen() && completed && keepAlive) {
                  channel.read();
                  ConnectionIdleTimeout.of(channel).reset();
                } else {
                  channel.close();
                }
                notifyListeners(responseStatus);
              });
          } else {
            bodyWriter.dispose();
            notifyListeners(responseStatus);
          }
        });
    } catch (Exception e) {
      bodyWriter.dispose();
      LOGGER.warn("Error finalizing response", e);
      forceCloseWithResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

  }

  private void forceCloseWithResponse(HttpResponseStatus status) {
    Promise.async(down ->
      channel.writeAndFlush(new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        Unpooled.EMPTY_BUFFER,
        ERROR_RESPONSE_HEADERS,
        EmptyHttpHeaders.INSTANCE
      ))
        .addListener(future -> down.success(future.isSuccess()))
    )
      .then(__ -> notifyListeners(status));
  }

  private boolean mustHaveBody(HttpResponseStatus responseStatus) {
    int code = responseStatus.code();
    return (code < 100 || code >= 200) && code != 204 && code != 304;
  }

  @Override
  public void transmit(HttpResponseStatus responseStatus, ByteBuf body) {
    if (body.readableBytes() == 0) {
      body.release();
      sendResponse(responseStatus, EMPTY_BODY);
    } else {
      sendResponse(responseStatus, new LastHttpContentResponseWriter(new DefaultLastHttpContent(body.touch())));
    }
  }

  private boolean isHead() {
    return ratpackRequest.getMethod().isHead();
  }

  private static final Set<OpenOption> OPEN_OPTIONS = Collections.singleton(StandardOpenOption.READ);

  @Override
  public void transmit(HttpResponseStatus status, Path file) {
    if (isHead()) {
      sendResponse(status, EMPTY_BODY);
    } else {
      String sizeString = responseHeaders.getAsString(HttpHeaderConstants.CONTENT_LENGTH);
      long size = sizeString == null ? 0 : Long.parseLong(sizeString);
      boolean compress = !responseHeaders.contains(HttpHeaderConstants.CONTENT_ENCODING, HttpHeaderConstants.IDENTITY, true);

      if (!isSsl && !compress && file.getFileSystem().equals(FileSystems.getDefault())) {
        sendResponse(status, channel ->
          Blocking.get(() -> FileChannel.open(file, OPEN_OPTIONS))
            .flatMap(fileChannel ->
              Promise.<Boolean>async(down ->
                channel.writeAndFlush(new DefaultFileRegion(fileChannel, 0, size))
                  .addListener(future -> down.success(future.isSuccess()))
              )
            )
            .mapError(any -> false)
        );
      } else {
        sendResponse(status, channel ->
          Blocking.get(() -> Files.newByteChannel(file))
            .flatMap(fileChannel ->
              Promise.<Boolean>async(down -> {
                channel.write(new HttpChunkedInput(new ChunkedNioStream(fileChannel)));
                channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                  .addListener(future -> down.success(future.isSuccess()));
              })
            )
            .mapError(any -> false)
        );
      }
    }
  }

  @Override
  public void transmit(HttpResponseStatus status, Publisher<? extends ByteBuf> publisher) {
    sendResponse(status, channel ->
      Promise.async(downstream -> {
        publisher.subscribe(new Subscriber<ByteBuf>() {
          private Subscription subscription;

          private final AtomicBoolean done = new AtomicBoolean();

          private final ChannelFutureListener cancelOnFailure = future -> {
            if (!future.isSuccess()) {
              cancel();
            }
          };

          private final GenericFutureListener<Future<? super Void>> cancelOnCloseListener =
            c -> cancel();

          private void cancel() {
            channel.closeFuture().removeListener(cancelOnCloseListener);
            if (done.compareAndSet(false, true)) {
              subscription.cancel();
              downstream.success(false);
            }
          }

          @Override
          public void onSubscribe(Subscription subscription) {
            if (subscription == null) {
              throw new NullPointerException("'subscription' is null");
            }
            if (this.subscription != null) {
              subscription.cancel();
              return;
            }

            this.subscription = subscription;

            channel.closeFuture().addListener(cancelOnCloseListener);
            onWritabilityChanged = () -> {
              if (channel.isWritable() && !done.get()) {
                subscription.request(1);
              }
            };
            if (channel.isWritable()) {
              subscription.request(1);
            }

          }

          @Override
          public void onNext(ByteBuf o) {
            o.touch();
            if (channel.isOpen()) {
              channel.writeAndFlush(new DefaultHttpContent(o)).addListener(cancelOnFailure);
              if (channel.isWritable()) {
                subscription.request(1);
              }
            } else {
              o.release();
              cancel();
            }
          }

          @Override
          public void onError(Throwable t) {
            if (t == null) {
              throw new NullPointerException("error is null");
            }
            LOGGER.warn("Exception thrown transmitting stream", t);
            if (done.compareAndSet(false, true)) {
              channel.closeFuture().removeListener(cancelOnCloseListener);
              downstream.success(false);
            }
          }

          @Override
          public void onComplete() {
            if (done.compareAndSet(false, true)) {
              channel.closeFuture().removeListener(cancelOnCloseListener);
              onWritabilityChanged = NOOP_RUNNABLE;
              channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
              downstream.success(true);
            }
          }
        });
      }));
  }

  private void notifyListeners(final HttpResponseStatus responseStatus) {
    if (outcomeListeners != null) {
      channel.attr(ATTRIBUTE_KEY).set(null);
      SentResponse sentResponse = new DefaultSentResponse(new NettyHeadersBackedHeaders(responseHeaders), new DefaultStatus(responseStatus));
      RequestOutcome requestOutcome = new DefaultRequestOutcome(ratpackRequest, sentResponse, stopTime);
      for (Action<? super RequestOutcome> outcomeListener : outcomeListeners) {
        try {
          outcomeListener.execute(requestOutcome);
        } catch (Exception e) {
          LOGGER.warn("request outcome listener " + outcomeListener + " threw exception", e);
        }
      }
    }
  }

  public void writabilityChanged() {
    onWritabilityChanged.run();
  }

  @Override
  public void addOutcomeListener(Action<? super RequestOutcome> action) {
    if (outcomeListeners == null) {
      outcomeListeners = new ArrayList<>(1);
    }
    outcomeListeners.add(action);
  }

  @Override
  public void forceCloseConnection() {
    responseHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
  }
}
