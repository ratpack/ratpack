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

package ratpack.server.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.ExecController;
import ratpack.func.Action;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.internal.ChainHandler;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.internal.DescribingHandler;
import ratpack.handling.internal.DescribingHandlers;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.internal.*;
import ratpack.registry.Registry;
import ratpack.render.internal.DefaultRenderController;
import ratpack.server.ServerConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends ChannelInboundHandlerAdapter {

  private static final AttributeKey<Action<Object>> CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY = AttributeKey.valueOf("ratpack.subscriber");
  private static final AttributeKey<RequestBodyAccumulator> BODY_ACCUMULATOR_KEY = AttributeKey.valueOf(RequestBodyAccumulator.class.getName());

  private final static Logger LOGGER = LoggerFactory.getLogger(NettyHandlerAdapter.class);

  private final Handler[] handlers;

  private final DefaultContext.ApplicationConstants applicationConstants;

  private final Registry serverRegistry;
  private final boolean development;

  private final Set<HttpMethod> methodsCanHaveBody;

  public NettyHandlerAdapter(Registry serverRegistry, Handler handler) throws Exception {
    this.handlers = ChainHandler.unpack(handler);
    this.serverRegistry = serverRegistry;
    this.applicationConstants = new DefaultContext.ApplicationConstants(this.serverRegistry, new DefaultRenderController(), serverRegistry.get(ExecController.class), Handlers.notFound());
    ServerConfig serverConfig = serverRegistry.get(ServerConfig.class);
    this.methodsCanHaveBody = serverConfig.getMethodsCanHaveBody();
    this.development = serverConfig.isDevelopment();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.read();
    super.channelActive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      newRequest(ctx, (HttpRequest) msg);
    } else if (msg instanceof HttpContent) {
      RequestBodyAccumulator bodyAccumulator = ctx.attr(BODY_ACCUMULATOR_KEY).get();
      if (bodyAccumulator != null) {
        bodyAccumulator.add((HttpContent) msg);
      }
      if (msg instanceof LastHttpContent) {
        ctx.read();
      }
    } else {
      Action<Object> subscriber = ctx.attr(CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY).get();
      if (subscriber == null) {
        super.channelRead(ctx, msg);
      } else {
        subscriber.execute(msg);
      }
    }
  }

  private void newRequest(final ChannelHandlerContext ctx, final HttpRequest nettyRequest) throws Exception {
    if (!nettyRequest.decoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    RequestBody requestBody = canHaveBody(nettyRequest.method()) ? new RequestBody(HttpUtil.getContentLength(nettyRequest, -1L), nettyRequest, ctx) : null;
    if (requestBody != null) {
      ctx.attr(BODY_ACCUMULATOR_KEY).set(requestBody);
    }

    final Channel channel = ctx.channel();
    InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
    InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();

    final DefaultRequest request = new DefaultRequest(
      Instant.now(),
      new NettyHeadersBackedHeaders(nettyRequest.headers()),
      nettyRequest.method(),
      nettyRequest.protocolVersion(),
      nettyRequest.uri(),
      remoteAddress,
      socketAddress,
      serverRegistry.get(ServerConfig.class),
      requestBody
    );
    final HttpHeaders nettyHeaders = new DefaultHttpHeaders(false);
    final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyHeaders);
    final AtomicBoolean transmitted = new AtomicBoolean(false);

    final DefaultResponseTransmitter responseTransmitter = new DefaultResponseTransmitter(transmitted, channel, nettyRequest, request, nettyHeaders, requestBody);

    ctx.attr(DefaultResponseTransmitter.ATTRIBUTE_KEY).set(responseTransmitter);

    Action<Action<Object>> subscribeHandler = thing -> {
      transmitted.set(true);
      ctx.attr(CHANNEL_SUBSCRIBER_ATTRIBUTE_KEY).set(thing);
    };

    final DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(
      applicationConstants,
      request,
      channel,
      responseTransmitter,
      subscribeHandler
    );

    final Response response = new DefaultResponse(responseHeaders, ctx.alloc(), responseTransmitter);
    requestConstants.response = response;

    DefaultContext.start(channel.eventLoop(), requestConstants, serverRegistry, handlers, execution -> {
      if (requestBody != null) {
        requestBody.close();
      }
      channel.attr(BODY_ACCUMULATOR_KEY).remove();
      if (!transmitted.get()) {
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
        responseTransmitter.transmit(HttpResponseStatus.INTERNAL_SERVER_ERROR, body);
      }
    });
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (!isIgnorableException(cause)) {
      LOGGER.error("", cause);
      if (ctx.channel().isActive()) {
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    ctx.attr(DefaultResponseTransmitter.ATTRIBUTE_KEY).get().writabilityChanged();
  }

  private boolean isIgnorableException(Throwable throwable) {
    if (throwable instanceof ClosedChannelException) {
      return true;
    } else if (throwable instanceof IOException) {
      // There really does not seem to be a better way of detecting this kind of exception
      String message = throwable.getMessage();
      return message != null && message.endsWith("Connection reset by peer");
    } else {
      return false;
    }
  }

  @SuppressWarnings("deprecation")
  public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    @SuppressWarnings("deprecation")
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderConstants.CONTENT_TYPE, HttpHeaderConstants.PLAIN_TEXT_UTF8);

    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private boolean canHaveBody(HttpMethod method) {
    return methodsCanHaveBody.contains(method);
  }

}
