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
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.exec.Blocking;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Action;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.handling.internal.DoubleTransmissionException;
import ratpack.http.Request;
import ratpack.http.RequestBodyTooLargeException;
import ratpack.http.SentResponse;
import ratpack.http.internal.*;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DefaultResponseTransmitter implements ResponseTransmitter {

  static final AttributeKey<DefaultResponseTransmitter> ATTRIBUTE_KEY = AttributeKey.valueOf(DefaultResponseTransmitter.class.getName());

  private final static Logger LOGGER = LoggerFactory.getLogger(ResponseTransmitter.class);

  private static final Runnable NOOP_RUNNABLE = () -> {
  };
  private final AtomicBoolean transmitted;
  private final Channel channel;
  private final Request ratpackRequest;
  private final HttpHeaders responseHeaders;
  private final RequestBody requestBody;
  private final boolean isSsl;

  private List<Action<? super RequestOutcome>> outcomeListeners;

  private boolean isKeepAlive;
  private Instant stopTime;

  private Runnable onWritabilityChanged = NOOP_RUNNABLE;

  public DefaultResponseTransmitter(
    AtomicBoolean transmitted,
    Channel channel,
    HttpRequest nettyRequest,
    Request ratpackRequest,
    HttpHeaders responseHeaders,
    @Nullable RequestBody requestBody
  ) {
    this.transmitted = transmitted;
    this.channel = channel;
    this.ratpackRequest = ratpackRequest;
    this.responseHeaders = responseHeaders;
    this.requestBody = requestBody;
    this.isKeepAlive = HttpUtil.isKeepAlive(nettyRequest);
    this.isSsl = channel.pipeline().get(SslHandler.class) != null;
  }

  private void drainRequestBody(Consumer<Throwable> next) {
    if (requestBody == null || !requestBody.isUnread()) {
      next.accept(null);
    } else {
      if (Execution.isActive()) {
        Promise.async(down ->
          requestBody.drain(e -> {
            if (e == null) {
              down.success(null);
            } else {
              down.error(e);
            }
          })

        )
          .onError(next::accept)
          .then(n -> next.accept(null));
      } else {
        requestBody.drain(next);
      }
    }
  }

  private ChannelFuture pre(HttpResponseStatus responseStatus) {
    if (transmitted.compareAndSet(false, true)) {
      stopTime = Instant.now();
      try {
        if (responseHeaders.contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true)) {
          isKeepAlive = false;
        } else if (!isKeepAlive) {
          forceCloseConnection();
        }

        HttpResponse headersResponse = new CustomHttpResponse(responseStatus, responseHeaders);
        if (mustHaveBody(responseStatus) && isKeepAlive && HttpUtil.getContentLength(headersResponse, -1) == -1 && !HttpUtil.isTransferEncodingChunked(headersResponse)) {
          HttpUtil.setTransferEncodingChunked(headersResponse, true);
        }

        if (channel.isOpen()) {
          return channel.writeAndFlush(headersResponse);
        } else {
          return null;
        }
      } catch (Exception e) {
        LOGGER.warn("Error finalizing response", e);
        return null;
      }
    } else {
      String msg = "attempt at double transmission for: " + ratpackRequest.getRawUri();
      LOGGER.warn(msg, new DoubleTransmissionException(msg));
      return null;
    }
  }

  private boolean mustHaveBody(HttpResponseStatus responseStatus) {
    int code = responseStatus.code();
    return (code < 100 || code >= 200) && code != 204 && code != 304;
  }

  @Override
  public void transmit(HttpResponseStatus responseStatus, ByteBuf body) {
    if (body.readableBytes() == 0) {
      body.release();
      transmit(responseStatus, LastHttpContent.EMPTY_LAST_CONTENT, false);
    } else {
      transmit(responseStatus, new DefaultLastHttpContent(body), false);
    }
  }

  private void transmit(final HttpResponseStatus responseStatus, Object body, boolean sendLastHttpContent) {
    ChannelFuture channelFuture = pre(responseStatus);
    if (channelFuture == null) {
      ReferenceCountUtil.release(body);
      isKeepAlive = false;
      post(responseStatus);
      return;
    }

    channelFuture.addListener(future -> {
      if (future.isSuccess() && channel.isOpen()) {
        if (sendLastHttpContent) {
          channel.write(body);
          post(responseStatus);
        } else {
          post(responseStatus, channel.writeAndFlush(body));
        }
      } else {
        ReferenceCountUtil.release(body);
        isKeepAlive = false;
        post(responseStatus);
      }
    });
  }

  @Override
  public void transmit(HttpResponseStatus status, Path file) {
    String sizeString = responseHeaders.getAsString(HttpHeaderConstants.CONTENT_LENGTH);
    long size = sizeString == null ? 0 : Long.parseLong(sizeString);
    boolean compress = !responseHeaders.contains(HttpHeaderConstants.CONTENT_ENCODING, HttpHeaderConstants.IDENTITY, true);

    if (!isSsl && !compress && file.getFileSystem().equals(FileSystems.getDefault())) {
      FileRegion defaultFileRegion = new DefaultFileRegion(file.toFile(), 0, size);
      transmit(status, defaultFileRegion, true);
    } else {
      Blocking.get(() ->
        Files.newByteChannel(file)
      ).then(fileChannel ->
        transmit(status, new HttpChunkedInput(new ChunkedNioStream(fileChannel)), false)
      );
    }
  }

  @Override
  public Subscriber<ByteBuf> transmitter(HttpResponseStatus responseStatus) {
    return new Subscriber<ByteBuf>() {
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
          post(responseStatus);
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

        ChannelFuture channelFuture = pre(responseStatus);
        if (channelFuture == null) {
          subscription.cancel();
          isKeepAlive = false;
          notifyListeners(responseStatus);
        } else {
          channelFuture.addListener(f -> {
            if (f.isSuccess() && channel.isOpen()) {
              channel.closeFuture().addListener(cancelOnCloseListener);
              if (channel.isWritable()) {
                this.subscription.request(1);
              }
              onWritabilityChanged = () -> {
                if (channel.isWritable() && !done.get()) {
                  this.subscription.request(1);
                }
              };
            } else {
              cancel();
            }
          });
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
          post(responseStatus);
        }
      }

      @Override
      public void onComplete() {
        if (done.compareAndSet(false, true)) {
          channel.closeFuture().removeListener(cancelOnCloseListener);
          post(responseStatus);
        }
      }
    };
  }

  private void post(HttpResponseStatus responseStatus) {
    post(responseStatus, channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT));
  }

  private void post(HttpResponseStatus responseStatus, ChannelFuture lastContentFuture) {
    lastContentFuture.addListener(v ->
      drainRequestBody(e -> {
        if (LOGGER.isDebugEnabled()) {
          if (e instanceof RequestBodyTooLargeException) {
            LOGGER.debug("Unread request body was too large to drain, will close connection (maxContentLength: {})", ((RequestBodyTooLargeException) e).getMaxContentLength());
          } else if (e != null){
            LOGGER.debug("An error occurred draining the unread request body. The connection will be closed", e);
          }
        }
        if (channel.isOpen()) {
          if (isKeepAlive && e == null) {
            lastContentFuture.channel().read();
            ConnectionIdleTimeout.of(channel).reset();
          } else {
            lastContentFuture.channel().close();
          }
        }

        notifyListeners(responseStatus);
      })
    );
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
