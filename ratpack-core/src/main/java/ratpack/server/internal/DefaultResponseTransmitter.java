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
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.handling.RequestOutcome;
import ratpack.handling.internal.DefaultRequestOutcome;
import ratpack.handling.internal.DoubleTransmissionException;
import ratpack.http.Request;
import ratpack.http.SentResponse;
import ratpack.http.internal.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultResponseTransmitter implements ResponseTransmitter {

  private final static Logger LOGGER = LoggerFactory.getLogger(ResponseTransmitter.class);

  private static final LastHttpContentResponseBodyWriter EMPTY_BODY = new LastHttpContentResponseBodyWriter(LastHttpContent.EMPTY_LAST_CONTENT);
  private static final DefaultHttpHeaders ERROR_RESPONSE_HEADERS = new DefaultHttpHeaders();

  static {
    ERROR_RESPONSE_HEADERS.set(HttpHeaderNames.CONTENT_LENGTH, 0);
    ERROR_RESPONSE_HEADERS.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
  }

  private final AtomicBoolean responseInitiated;
  private final Channel channel;
  private final Clock clock;
  private final Request ratpackRequest;
  private final HttpHeaders responseHeaders;
  private final RequestBody requestBody;
  private final boolean isSsl;
  private final HttpRequest nettyRequest;

  private final Runnable onRequestFinished;
  private List<Action<? super RequestOutcome>> outcomeListeners;

  private Instant stopTime;
  private ResponseBodyWriter responseBodyWriter;
  private boolean done;

  public DefaultResponseTransmitter(
    AtomicBoolean responseInitiated,
    Channel channel,
    Clock clock,
    HttpRequest nettyRequest,
    Request ratpackRequest,
    HttpHeaders responseHeaders,
    @Nullable RequestBody requestBody,
    Runnable onRequestFinished
  ) {
    this.responseInitiated = responseInitiated;
    this.channel = channel;
    this.clock = clock;
    this.ratpackRequest = ratpackRequest;
    this.responseHeaders = responseHeaders;
    this.requestBody = requestBody;
    this.nettyRequest = nettyRequest;
    this.isSsl = channel.pipeline().get(SslHandler.class) != null;
    this.onRequestFinished = onRequestFinished;
  }

  private void preSendResponse(HttpResponseStatus responseStatus, ResponseBodyWriter responseBodyWriter, boolean drainRequestBeforeResponse) {
    if (responseInitiated.compareAndSet(false, true)) {
      this.responseBodyWriter = responseBodyWriter;
      stopTime = clock.instant();
      if (requestBody == null) {
        sendResponse(responseStatus, responseBodyWriter, true);
      } else if (drainRequestBeforeResponse) {
        requestBody.drain()
          .onError(e -> {
            LOGGER.warn("An error occurred draining the unread request body. The connection will be closed", e);
            forceCloseWithResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
          })
          .then(outcome -> {
            switch (outcome) {
              case TOO_LARGE:
                responseBodyWriter.dispose();
                forceCloseWithResponse(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
                break;
              case DISCARDED:
                addConnectionCloseResponseHeader();
                sendResponse(responseStatus, responseBodyWriter, false);
                break;
              case DRAINED:
                sendResponse(responseStatus, responseBodyWriter, false);
                break;
              default:
                throw new IllegalStateException("unhandled drain outcome: " + outcome);
            }
          });
      } else {
        sendResponse(responseStatus, responseBodyWriter, true);
      }
    } else {
      responseBodyWriter.dispose();
      if (done) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("", new DoubleTransmissionException("Attempt at double transmission after response sent for: " + ratpackRequest.getRawUri()));
        }
      } else {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("", new DoubleTransmissionException("Attempt at double transmission while sending response (connection will be closed) for: " + ratpackRequest.getRawUri()));
        }
        channel.close();
      }
    }
  }

  private void sendResponse(HttpResponseStatus responseStatus, ResponseBodyWriter bodyWriter, boolean drainRequestBeforeResponse) {
    try {
      boolean responseRequestedConnectionClose = responseHeaders.contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true);
      boolean requestRequestedConnectionClose = !HttpUtil.isKeepAlive(this.nettyRequest);

      boolean keepAlive = !requestRequestedConnectionClose && !responseRequestedConnectionClose;
      if (!keepAlive && !responseRequestedConnectionClose) {
        addConnectionCloseResponseHeader();
      }

      HttpResponse headersResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, responseStatus, responseHeaders);

      boolean isImplicitlyChunked = mustHaveBody(responseStatus)
        && keepAlive
        && HttpUtil.getContentLength(headersResponse, -1) == -1
        && !HttpUtil.isTransferEncodingChunked(headersResponse);

      if (isImplicitlyChunked) {
        HttpUtil.setTransferEncodingChunked(headersResponse, true);
      }

      sendResponseHeadersAndBody(responseStatus, bodyWriter, keepAlive, headersResponse, drainRequestBeforeResponse);
    } catch (Exception e) {
      bodyWriter.dispose();
      LOGGER.warn("Error finalizing response", e);
      forceCloseWithResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private void sendResponseHeadersAndBody(HttpResponseStatus responseStatus, ResponseBodyWriter bodyWriter, boolean keepAlive, HttpResponse headersResponse, boolean drainRequestBeforeResponse) {
    Promise.<Future<? super Void>>async(down ->
        channel.writeAndFlush(headersResponse).addListener(down::success)
      )
      .then(result -> {
        if (result.isSuccess()) {
          sendResponseBody(responseStatus, bodyWriter, keepAlive);
        } else {
          closeAfterHeaderSendFailure(responseStatus, bodyWriter, drainRequestBeforeResponse, result);
        }
      });
  }

  private void closeAfterHeaderSendFailure(HttpResponseStatus responseStatus, ResponseBodyWriter bodyWriter, boolean drainRequestBeforeResponse, Future<? super Void> result) {
    if (channel.isOpen()) {
      LOGGER.warn("Error writing response headers", result.cause());
      channel.close();
    }

    bodyWriter.dispose();
    if (requestBody != null && drainRequestBeforeResponse) {
      requestBody.drain()
        .onError(e -> {
          LOGGER.warn("An error occurred draining the unread request body after sending the response. The connection will be closed", e);
          channel.close();
          notifyListeners(responseStatus);
        })
        .then(outcome -> {
          switch (outcome) {
            case TOO_LARGE:
            case DISCARDED:
              channel.close();
              notifyListeners(responseStatus);
              break;
            case DRAINED:
              notifyListeners(responseStatus);
              break;
            default:
              throw new IllegalStateException("unhandled drain outcome: " + outcome);
          }
        });
    } else {
      notifyListeners(responseStatus);
    }
  }

  private void sendResponseBody(HttpResponseStatus responseStatus, ResponseBodyWriter bodyWriter, boolean keepAlive) {
    Promise.<Future<? super Void>>async(down ->
        bodyWriter.write(channel).addListener(down::success)
      )
      .then(result -> {
        if (channel.isOpen()) {
          closeChannelAfterSendingBody(keepAlive, result);
        }

        notifyListeners(responseStatus);
      });
  }

  private void closeChannelAfterSendingBody(boolean keepAlive, Future<? super Void> result) {
    if (!result.isSuccess()) {
      LOGGER.warn("Error from response body writer", result.cause());
    }

    if (result.isSuccess() && keepAlive) {
      onRequestFinished.run();
    } else {
      channel.close();
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
      .onError(__ -> {
        channel.close();
        notifyListeners(status);
      })
      .then(__ -> {
        channel.close();
        notifyListeners(status);
      });
  }

  private boolean mustHaveBody(HttpResponseStatus responseStatus) {
    int code = responseStatus.code();
    return (code < 100 || code >= 200) && code != 204 && code != 304;
  }

  @Override
  public void transmit(HttpResponseStatus responseStatus, ByteBuf body) {
    if (body.readableBytes() == 0) {
      body.release();
      preSendResponse(responseStatus, EMPTY_BODY, true);
    } else {
      preSendResponse(responseStatus, new LastHttpContentResponseBodyWriter(new DefaultLastHttpContent(body.touch())), true);
    }
  }

  private boolean isHead() {
    return ratpackRequest.getMethod().isHead();
  }

  @Override
  public void transmit(HttpResponseStatus status, Path file) {
    if (isHead()) {
      preSendResponse(status, EMPTY_BODY, true);
    } else {
      String sizeString = responseHeaders.getAsString(HttpHeaderConstants.CONTENT_LENGTH);
      long size = sizeString == null ? 0 : Long.parseLong(sizeString);

      boolean compress = !responseHeaders.contains(HttpHeaderConstants.CONTENT_ENCODING, HttpHeaderConstants.IDENTITY, true);
      boolean zeroCopy = !isSsl && !compress && file.getFileSystem().equals(FileSystems.getDefault());

      ResponseBodyWriter responseBodyWriter = zeroCopy
        ? new ZeroCopyFileResponseBodyWriter(file, size)
        : new ChunkedFileResponseBodyWriter(file);

      preSendResponse(status, responseBodyWriter, true);
    }
  }

  @Override
  public void transmit(HttpResponseStatus status, Publisher<? extends ByteBuf> publisher, boolean drainRequestBeforeResponse) {
    preSendResponse(status, new StreamingResponseBodyWriter(publisher), drainRequestBeforeResponse);
  }

  private void notifyListeners(final HttpResponseStatus responseStatus) {
    done = true;
    if (outcomeListeners != null) {
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

  @Override
  public void onWritabilityChanged() {
    if (responseBodyWriter != null && channel.isWritable()) {
      responseBodyWriter.onWritable();
    }
  }

  @Override
  public void onConnectionClosed() {
    if (responseBodyWriter != null) {
      responseBodyWriter.onClosed();
    }
  }

  @Override
  public void addOutcomeListener(Action<? super RequestOutcome> action) {
    if (outcomeListeners == null) {
      outcomeListeners = new ArrayList<>(1);
    }
    outcomeListeners.add(action);
  }

  void addConnectionCloseResponseHeader() {
    responseHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
  }

}
