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

import com.google.common.base.Predicate;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.event.internal.DefaultEventController;
import ratpack.exec.ExecControl;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.func.Pair;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.http.Request;
import ratpack.http.SentResponse;
import ratpack.http.internal.*;
import ratpack.util.internal.InternalRatpackError;
import ratpack.util.internal.NumberUtil;

import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class DefaultResponseTransmitter implements ResponseTransmitter {

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultResponseTransmitter.class);
  private static final Runnable NOOP_RUNNABLE = () -> {

  };

  private final AtomicBoolean transmitted;
  private final ExecControl execControl;
  private final Channel channel;
  private final FullHttpRequest nettyRequest;
  private final Request ratpackRequest;
  private final HttpHeaders responseHeaders;
  private final DefaultEventController<RequestOutcome> requestOutcomeEventController;
  private final boolean compressionEnabled;
  private final Predicate<? super Pair<Long, String>> shouldCompress;
  private final long startTime;
  private final boolean isKeepAlive;
  private final boolean isSsl;

  private long stopTime;

  private Runnable onWritabilityChanged = NOOP_RUNNABLE;

  public DefaultResponseTransmitter(AtomicBoolean transmitted, ExecControl execControl, Channel channel, FullHttpRequest nettyRequest, Request ratpackRequest, HttpHeaders responseHeaders, DefaultEventController<RequestOutcome> requestOutcomeEventController, boolean compressionEnabled, Predicate<? super Pair<Long, String>> shouldCompress, long startTime) {
    this.transmitted = transmitted;
    this.execControl = execControl;
    this.channel = channel;
    this.compressionEnabled = compressionEnabled;
    this.shouldCompress = shouldCompress;
    this.nettyRequest = nettyRequest.retain();
    this.ratpackRequest = ratpackRequest;
    this.responseHeaders = responseHeaders;
    this.requestOutcomeEventController = requestOutcomeEventController;
    this.startTime = startTime;
    this.isKeepAlive = isKeepAlive(nettyRequest);
    this.isSsl = channel.pipeline().get(SslHandler.class) != null;
  }

  private ChannelFuture pre(HttpResponseStatus responseStatus) {
    if (transmitted.compareAndSet(false, true)) {
      stopTime = System.nanoTime();

      HttpResponse headersResponse = new CustomHttpResponse(responseStatus, responseHeaders);
      nettyRequest.release();

      if (isKeepAlive) {
        headersResponse.headers().set(HttpHeaderConstants.CONNECTION, HttpHeaderConstants.KEEP_ALIVE);
      }

      if (startTime > 0) {
        headersResponse.headers().set("X-Response-Time", NumberUtil.toMillisDiffString(startTime, stopTime));
      }

      if (channel.isOpen()) {
        return channel.writeAndFlush(headersResponse).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      } else {
        return null;
      }
    } else {
      LOGGER.warn("attempt at double transmission for: " + ratpackRequest.getRawUri(), new InternalRatpackError(""));
      return null;
    }
  }

  @Override
  public void transmit(final HttpResponseStatus responseStatus, final ByteBuf body) {
    responseHeaders.set(HttpHeaderConstants.CONTENT_LENGTH, body.readableBytes());
    transmit(responseStatus, new DefaultHttpContent(body));
  }

  private void transmit(final HttpResponseStatus responseStatus, final Object body) {
    ChannelFuture channelFuture = pre(responseStatus);
    if (channelFuture == null) {
      return;
    }

    channelFuture.addListener(future -> {
      if (channel.isOpen()) {
        channel.write(body);
        post(responseStatus);
      }
    });
  }

  @Override
  public void transmit(final HttpResponseStatus responseStatus, final BasicFileAttributes basicFileAttributes, final Path file) {
    String contentType = responseHeaders.get(HttpHeaderConstants.CONTENT_TYPE);
    final long size = basicFileAttributes.size();

    Pair<Long, String> fileDetails = Pair.of(size, contentType);
    final boolean compressThis = compressionEnabled && (contentType != null && shouldCompress.apply(fileDetails));
    if (compressionEnabled && !compressThis) {
      // Signal to the compressor not to compress this
      responseHeaders.set(HttpHeaderConstants.CONTENT_ENCODING, HttpHeaderConstants.IDENTITY);
    }

    responseHeaders.set(HttpHeaderConstants.CONTENT_LENGTH, size);

    if (!isSsl && !compressThis && file.getFileSystem().equals(FileSystems.getDefault())) {
      execControl.blocking(() -> new FileInputStream(file.toFile()).getChannel()).then(fileChannel -> {
        FileRegion defaultFileRegion = new DefaultFileRegion(fileChannel, 0, size);
        transmit(responseStatus, defaultFileRegion);
      });
    } else {
      execControl.blocking(() -> Files.newByteChannel(file)).then(fileChannel ->
        transmit(responseStatus, new HttpChunkedInput(new ChunkedNioStream(fileChannel)))
      );
    }
  }

  @Override
  public Subscriber<ByteBuf> transmitter(final HttpResponseStatus responseStatus) {
    return new Subscriber<ByteBuf>() {
      private Subscription subscription;
      private final AtomicBoolean done = new AtomicBoolean();

      private final ChannelFutureListener cancelOnFailure = future -> {
        if (!done.get()) {
          if (!future.isSuccess()) {
            cancel();
          }
        }
      };

      private void cancel() {
        if (done.compareAndSet(false, true)) {
          subscription.cancel();
          post(responseStatus);
        }
      }

      @Override
      public void onSubscribe(Subscription s) {
        if (this.subscription != null) {
          s.cancel();
          return;
        }

        this.subscription = s;

        onWritabilityChanged = () -> {
          if (channel.isWritable() && !done.get()) {
            subscription.request(1);
          }
        };

        ChannelFuture channelFuture = pre(responseStatus);
        if (channelFuture == null) {
          s.cancel();
          notifyListeners(responseStatus, channel.close());
        } else {
          channelFuture.addListener(cancelOnFailure);
          if (channel.isWritable()) {
            subscription.request(1);
          }
        }
      }

      @Override
      public void onNext(ByteBuf o) {
        if (channel.isOpen()) {
          channel.writeAndFlush(new DefaultHttpContent(o)).addListener(cancelOnFailure);
          if (channel.isWritable()) {
            subscription.request(1);
          }
        }
      }

      @Override
      public void onError(Throwable t) {
        LOGGER.warn("Exception thrown transmitting stream", t);
        if (done.compareAndSet(false, true)) {
          post(responseStatus);
        }
      }

      @Override
      public void onComplete() {
        if (done.compareAndSet(false, true)) {
          post(responseStatus);
        }
      }
    };
  }

  private void post(HttpResponseStatus responseStatus) {
    if (channel.isOpen()) {
      ChannelFuture lastContentFuture = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      if (!isKeepAlive) {
        lastContentFuture.addListener(ChannelFutureListener.CLOSE);
      }
      notifyListeners(responseStatus, lastContentFuture);
    } else {
      notifyListeners(responseStatus, channel.newSucceededFuture());
    }
  }

  private void notifyListeners(final HttpResponseStatus responseStatus, ChannelFuture future) {
    if (requestOutcomeEventController.isHasListeners()) {
      future.addListener(ignore -> {
        SentResponse sentResponse = new DefaultSentResponse(new NettyHeadersBackedHeaders(responseHeaders), new DefaultStatus(responseStatus));
        RequestOutcome requestOutcome = new DefaultRequestOutcome(ratpackRequest, sentResponse, stopTime);
        requestOutcomeEventController.fire(requestOutcome);
      });
    }
  }

  public void writabilityChanged() {
    onWritabilityChanged.run();
  }
}
