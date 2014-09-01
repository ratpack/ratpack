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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.event.internal.DefaultEventController;
import ratpack.file.internal.ResponseTransmitter;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.http.Request;
import ratpack.http.SentResponse;
import ratpack.http.Status;
import ratpack.http.internal.CustomHttpResponse;
import ratpack.http.internal.DefaultSentResponse;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import ratpack.util.internal.NumberUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;

class DefaultResponseTransmitter implements ResponseTransmitter {

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultResponseTransmitter.class);
  private static final Runnable NOOP_RUNNABLE = new Runnable() {
    @Override
    public void run() {

    }
  };

  private final AtomicBoolean transmitted;
  private final Channel channel;
  private final FullHttpRequest nettyRequest;
  private final Request ratpackRequest;
  private final HttpHeaders responseHeaders;
  private final Status responseStatus;
  private final DefaultEventController<RequestOutcome> requestOutcomeEventController;
  private final long startTime;
  private final boolean isKeepAlive;

  private long stopTime;

  private Runnable onWritabilityChanged = NOOP_RUNNABLE;

  public DefaultResponseTransmitter(AtomicBoolean transmitted, Channel channel, FullHttpRequest nettyRequest, Request ratpackRequest, HttpHeaders responseHeaders, Status responseStatus, DefaultEventController<RequestOutcome> requestOutcomeEventController, long startTime) {
    this.transmitted = transmitted;
    this.channel = channel;
    this.nettyRequest = nettyRequest.retain();
    this.ratpackRequest = ratpackRequest;
    this.responseHeaders = responseHeaders;
    this.responseStatus = responseStatus;
    this.requestOutcomeEventController = requestOutcomeEventController;
    this.startTime = startTime;
    this.isKeepAlive = isKeepAlive(nettyRequest);
  }

  private ChannelFuture pre() {
    transmitted.set(true);

    stopTime = System.nanoTime();

    HttpResponseStatus nettyStatus = new HttpResponseStatus(responseStatus.getCode(), responseStatus.getMessage());
    HttpResponse headersResponse = new CustomHttpResponse(nettyStatus, responseHeaders);
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
  }

  @Override
  public void transmit(final Object body) {
    ChannelFuture channelFuture = pre();
    if (channelFuture == null) {
      return;
    }

    channelFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (channel.isOpen()) {
          channel.write(body);
          post();
        }
      }
    });
  }

  @Override
  public Subscriber<Object> transmitter() {
    return new Subscriber<Object>() {
      private Subscription subscription;
      private final AtomicBoolean done = new AtomicBoolean();

      private final ChannelFutureListener cancelOnFailure = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (!done.get()) {
            if (!future.isSuccess()) {
              cancel();
            }
          }
        }
      };

      private void cancel() {
        if (done.compareAndSet(false, true)) {
          subscription.cancel();
          post();
        }
      }

      @Override
      public void onSubscribe(Subscription s) {
        this.subscription = s;

        onWritabilityChanged = new Runnable() {
          @Override
          public void run() {
            if (channel.isWritable() && !done.get()) {
              subscription.request(1);
            }
          }
        };

        ChannelFuture channelFuture = pre();
        if (channelFuture == null) {
          s.cancel();
          notifyListeners(channel.close());
        } else {
          channelFuture.addListener(cancelOnFailure);
          if (channel.isWritable()) {
            subscription.request(1);
          }
        }
      }

      @Override
      public void onNext(Object o) {
        if (channel.isOpen()) {
          channel.writeAndFlush(o).addListener(cancelOnFailure);
          if (channel.isWritable()) {
            subscription.request(1);
          }
        }
      }

      @Override
      public void onError(Throwable t) {
        LOGGER.debug("Exception thrown transmitting stream", t);
        cancel();
      }

      @Override
      public void onComplete() {
        if (done.compareAndSet(false, true)) {
          post();
        }
      }
    };
  }

  private void post() {
    if (channel.isOpen()) {
      ChannelFuture lastContentFuture = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      if (!isKeepAlive) {
        lastContentFuture.addListener(ChannelFutureListener.CLOSE);
      }
      notifyListeners(lastContentFuture);
    } else {
      notifyListeners(channel.newSucceededFuture());
    }
  }

  private void notifyListeners(ChannelFuture future) {
    if (requestOutcomeEventController.isHasListeners()) {
      future.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture ignore) throws Exception {
          SentResponse sentResponse = new DefaultSentResponse(new NettyHeadersBackedHeaders(responseHeaders), responseStatus);
          RequestOutcome requestOutcome = new DefaultRequestOutcome(ratpackRequest, sentResponse, stopTime);
          requestOutcomeEventController.fire(requestOutcome);
        }
      });
    }
  }

  public void writabilityChanged() {
    onWritabilityChanged.run();
  }
}
