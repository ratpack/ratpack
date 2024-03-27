/*
 * Copyright 2013 the original author or authors.
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

package ratpack.core.server.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.core.handling.Handler;
import ratpack.core.handling.Handlers;
import ratpack.core.handling.internal.ChainHandler;
import ratpack.core.handling.internal.DefaultContext;
import ratpack.core.handling.internal.DescribingHandler;
import ratpack.core.handling.internal.DescribingHandlers;
import ratpack.core.http.Headers;
import ratpack.core.http.MutableHeaders;
import ratpack.core.http.Response;
import ratpack.core.http.internal.*;
import ratpack.core.render.internal.DefaultRenderController;
import ratpack.core.server.ServerConfig;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.registry.Registry;
import ratpack.func.Action;

import javax.annotation.Nullable;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends ChannelInboundHandlerAdapter {

  private final static Logger LOGGER = LoggerFactory.getLogger(NettyHandlerAdapter.class);

  private final Handler[] handlers;

  private final DefaultContext.ApplicationConstants applicationConstants;

  private final Registry serverRegistry;
  private final boolean development;
  private final Clock clock;
  private final Duration idleTimeout;
  private final ServerConfig serverConfig;

  public NettyHandlerAdapter(Registry serverRegistry, Handler handler) {
    this.serverConfig = serverRegistry.get(ServerConfig.class);
    this.handlers = ChainHandler.unpack(handler);
    this.serverRegistry = serverRegistry;
    this.applicationConstants = new DefaultContext.ApplicationConstants(
      this.serverRegistry,
      new DefaultRenderController(),
      serverRegistry.get(ExecController.class),
      Handlers.notFound()
    );
    this.development = serverConfig.isDevelopment();
    this.clock = serverRegistry.get(Clock.class);
    this.idleTimeout = serverConfig.getIdleTimeout();
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    Attribute<ChannelState> attr = ctx.channel().attr(ChannelState.KEY);
    if (attr.get() == null) {
      ChannelState state = new ChannelState(new ConnectionIdleTimeout(ctx.pipeline(), idleTimeout));
      attr.set(state);
      ctx.channel().closeFuture().addListener(future -> {
        if (state.responseTransmitter != null) {
          state.responseTransmitter.onConnectionClosed();
        }

        if (state.requestBody != null) {
          state.requestBody.onClose();
        }
      });
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.read();
    super.channelActive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ChannelState state = state(ctx);
    if (state.messageQueue == null) {
      handleMessage(ctx, msg, state);
    } else {
      // There are no bounds on the queue at this level.
      // We are relying on the upstream TCP buffers to provide the bounding.
      state.messageQueue.add(msg);
    }
  }

  private void handleMessage(ChannelHandlerContext ctx, Object msg, ChannelState state) throws Exception {
    if (msg instanceof HttpRequest) {
      handleRequest(ctx, (HttpRequest) msg, state);
    } else if (msg instanceof HttpContent) {
      handleContent(ctx, (HttpContent) msg, state);
    } else {
      handleOther(ctx, msg, state);
    }
  }

  private void handleRequest(ChannelHandlerContext ctx, HttpRequest request, ChannelState state) {
    state.requestedNextRequest = false;
    if (state.responseTransmitter == null) {
      newRequest(ctx, request, state);
    } else {
      state.messageQueue = new ArrayDeque<>();
      state.messageQueue.add(request);
    }
  }

  private static void handleContent(ChannelHandlerContext ctx, HttpContent httpContent, ChannelState state) {
    if (state.requestBody == null) {
      httpContent.release();
    } else {
      state.requestBody.add(httpContent.touch());
    }

    // Read for the next request proactively so that we detect if the client closes the connection.
    if (httpContent instanceof LastHttpContent) {
      state.requestedNextRequest = true;
      ctx.channel().read();
    }
  }

  private void handleOther(ChannelHandlerContext ctx, Object msg, ChannelState state) throws Exception {
    Action<Object> subscriber = state.rawSubscriber;
    if (subscriber == null) {
      super.channelRead(ctx, ReferenceCountUtil.touch(msg));
    } else {
      subscriber.execute(ReferenceCountUtil.touch(msg));
    }
  }

  private void newRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest, ChannelState state) {
    if (!nettyRequest.decoderResult().isSuccess()) {
      LOGGER.debug("Failed to decode HTTP request.", nettyRequest.decoderResult().cause());
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    Headers requestHeaders = new NettyHeadersBackedHeaders(nettyRequest.headers());

    long contentLength = HttpUtil.getContentLength(nettyRequest, -1L);
    String transferEncoding = requestHeaders.get(HttpHeaderNames.TRANSFER_ENCODING);
    boolean hasBody = contentLength > 0 || transferEncoding != null;

    state.requestBody = hasBody
      ? new RequestBody(contentLength, nettyRequest, ctx)
      : null;

    Channel channel = ctx.channel();
    InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
    InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();

    DefaultRequest request = new DefaultRequest(
      clock.instant(),
      requestHeaders,
      nettyRequest.method(),
      nettyRequest.protocolVersion(),
      nettyRequest.uri(),
      remoteAddress,
      socketAddress,
      serverConfig,
      state.requestBody,
      state.idleTimeout,
      state.sslSession
    );

    HttpHeaders nettyHeaders = new DefaultHttpHeaders();
    MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyHeaders);
    AtomicBoolean responseInitiated = new AtomicBoolean(false);

    state.responseTransmitter = new DefaultResponseTransmitter(
      responseInitiated,
      channel,
      clock,
      nettyRequest,
      request,
      nettyHeaders,
      state.requestBody,
      () -> onResponseSent(ctx, state)
    );

    DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(
      applicationConstants,
      request,
      channel,
      state.responseTransmitter,
      rawChannelSubscriber -> {
        responseInitiated.set(true);
        state.rawSubscriber = rawChannelSubscriber;
      }
    );

    Response response = new DefaultResponse(
      responseHeaders,
      ctx.alloc(),
      state.responseTransmitter,
      state.idleTimeout
    );

    requestConstants.response = response;

    DefaultContext.start(
      channel.eventLoop(),
      requestConstants,
      serverRegistry,
      handlers,
      execution -> {
        if (!responseInitiated.get()) {
          unhandledRequest(ctx, execution, requestConstants, request, response, state.responseTransmitter);
        }
      });
  }

  private void onResponseSent(ChannelHandlerContext ctx, ChannelState state) {
    state.idleTimeout.reset();
    state.responseTransmitter = null;
    Queue<Object> queue = state.messageQueue;
    if (queue != null) {
      while (true) {
        Object next = queue.poll();
        try {
          handleMessage(ctx, next, state(ctx));
        } catch (Exception e) {
          ctx.fireExceptionCaught(e);
          return;
        }

        if (next instanceof LastHttpContent || queue.isEmpty()) {
          break;
        }
      }

      if (queue.isEmpty()) {
        state.messageQueue = null;
      }
    }

    if (!state.requestedNextRequest) {
      state.requestedNextRequest = true;
      ctx.channel().read();
    }
  }

  private void unhandledRequest(
    ChannelHandlerContext ctx,
    Execution execution,
    DefaultContext.RequestConstants requestConstants,
    DefaultRequest request,
    Response response,
    ResponseTransmitter responseTransmitter
  ) {
    Handler lastHandler = requestConstants.handler;
    StringBuilder description = new StringBuilder();
    description
      .append("No response sent for ")
      .append(request.getMethod().getName())
      .append(" request to ")
      .append(request.getUri());

    if (lastHandler != null) {
      description.append(" (last handler: ");

      if (lastHandler instanceof DescribingHandler) {
        ((DescribingHandler) lastHandler).describeTo(description);
      } else {
        DescribingHandlers.describeTo(lastHandler, description);
      }
      description.append(")");
    }

    String message = description.toString();
    LOGGER.warn(message);

    response.getHeaders().clear();

    ByteBuf body;
    if (development) {
      CharBuffer charBuffer = CharBuffer.wrap(message);
      body = ByteBufUtil.encodeString(ctx.alloc(), charBuffer, CharsetUtil.UTF_8);
      response.contentType(HttpHeaderConstants.PLAIN_TEXT_UTF8);
    } else {
      body = Unpooled.EMPTY_BUFFER;
    }

    response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, body.readableBytes());
    execution.getController().fork()
      .eventLoop(execution.getEventLoop())
      .start(e -> responseTransmitter.transmit(HttpResponseStatus.INTERNAL_SERVER_ERROR, body));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (!isIgnorableException(cause)) {
      LOGGER.error("", cause);
    }

    Channel channel = ctx.channel();
    if (channel.isActive()) {
      sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    } else {
      channel.close();
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    ChannelState state = state(ctx);
    if (evt instanceof IdleStateEvent) {
      if (state.requestBody != null) {
        state.requestBody.onIdleTimeout();
      }
      ctx.close();
    }
    if (evt instanceof SslHandshakeCompletionEvent && ((SslHandshakeCompletionEvent) evt).isSuccess()) {
      SSLEngine engine = ctx.pipeline().get(SslHandler.class).engine();
      if (engine.getWantClientAuth() || engine.getNeedClientAuth()) {
        state.sslSession = engine.getSession();
      }
    }

    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    ChannelState state = state(ctx);
    ResponseTransmitter responseTransmitter = state.responseTransmitter;
    if (responseTransmitter != null) {
      responseTransmitter.onWritabilityChanged();
    }

    super.channelWritabilityChanged(ctx);
  }

  private static boolean isIgnorableException(Throwable throwable) {
    if (throwable instanceof ClosedChannelException) {
      return true;
    } else if (throwable instanceof IOException) {
      // There really does not seem to be a better way of detecting this kind of exception
      String message = throwable.getMessage();
      if (message == null) {
        return false;
      } else {
        message = message.toLowerCase(Locale.ROOT);
        return message.endsWith("connection reset by peer")
          || message.contains("broken pipe");
      }
    } else {
      return false;
    }
  }

  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderConstants.CONTENT_TYPE, HttpHeaderConstants.PLAIN_TEXT_UTF8);

    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private static ChannelState state(ChannelHandlerContext ctx) {
    return ctx.channel().attr(ChannelState.KEY).get();
  }

  private static final class ChannelState {

    private static final AttributeKey<ChannelState> KEY = AttributeKey.valueOf(NettyHandlerAdapter.class, "channel");

    @Nullable
    Queue<Object> messageQueue;

    @Nullable
    SSLSession sslSession;

    @Nullable
    Action<Object> rawSubscriber;

    ResponseTransmitter responseTransmitter;

    RequestBody requestBody;

    boolean requestedNextRequest;

    final ConnectionIdleTimeout idleTimeout;


    ChannelState(ConnectionIdleTimeout idleTimeout) {
      this.idleTimeout = idleTimeout;
    }
  }

}
