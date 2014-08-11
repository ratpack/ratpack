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
  private final AtomicBoolean transmitted;
  private final Channel channel;
  private final FullHttpRequest nettyRequest;
  private final Request ratpackRequest;
  private final HttpHeaders responseHeaders;
  private final Status responseStatus;
  private final DefaultEventController<RequestOutcome> requestOutcomeEventController;
  private final long startTime;

  public DefaultResponseTransmitter(AtomicBoolean transmitted, Channel channel, FullHttpRequest nettyRequest, Request ratpackRequest, HttpHeaders responseHeaders, Status responseStatus, DefaultEventController<RequestOutcome> requestOutcomeEventController, long startTime) {
    this.transmitted = transmitted;
    this.channel = channel;
    this.nettyRequest = nettyRequest.retain();
    this.ratpackRequest = ratpackRequest;
    this.responseHeaders = responseHeaders;
    this.responseStatus = responseStatus;
    this.requestOutcomeEventController = requestOutcomeEventController;
    this.startTime = startTime;
  }

  @Override
  public void transmit(Object body) {
    transmitted.set(true);
    HttpResponseStatus nettyStatus = new HttpResponseStatus(responseStatus.getCode(), responseStatus.getMessage());
    HttpResponse nettyResponse = new CustomHttpResponse(nettyStatus, responseHeaders);
    nettyRequest.release();

    boolean isKeepAlive = isKeepAlive(nettyRequest);
    if (channel.isOpen()) {
      if (isKeepAlive) {
        nettyResponse.headers().set(HttpHeaderConstants.CONNECTION, HttpHeaderConstants.KEEP_ALIVE);
      }

      long stopTime = System.nanoTime();
      if (startTime > 0) {
        nettyResponse.headers().set("X-Response-Time", NumberUtil.toMillisDiffString(startTime, stopTime));
      }

      ChannelFuture writeFuture = channel.writeAndFlush(nettyResponse);

      writeFuture.addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) throws Exception {
          if (!future.isSuccess()) {
            channel.close();
          }
        }
      });

      writeFuture = channel.write(body);

      writeFuture.addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) throws Exception {
          if (!future.isSuccess()) {
            channel.close();
          }
        }
      });

      ChannelFuture lastContentFuture = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

      if (requestOutcomeEventController.isHasListeners()) {
        SentResponse sentResponse = new DefaultSentResponse(new NettyHeadersBackedHeaders(responseHeaders), responseStatus);
        RequestOutcome requestOutcome = new DefaultRequestOutcome(ratpackRequest, sentResponse, stopTime);
        requestOutcomeEventController.fire(requestOutcome);
      }

      if (!isKeepAlive) {
        lastContentFuture.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }
}
