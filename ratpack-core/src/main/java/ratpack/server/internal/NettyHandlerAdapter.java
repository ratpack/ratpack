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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.event.internal.DefaultEventController;
import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;
import ratpack.file.internal.ActivationBackedMimeTypes;
import ratpack.file.internal.ShouldCompressPredicate;
import ratpack.func.Action;
import ratpack.func.Pair;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.RequestOutcome;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.handling.direct.internal.DefaultDirectChannelAccess;
import ratpack.handling.internal.*;
import ratpack.http.MutableHeaders;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.internal.*;
import ratpack.launch.ServerConfig;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.render.internal.*;
import ratpack.server.Stopper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final AttributeKey<DefaultResponseTransmitter> RESPONSE_TRANSMITTER_ATTRIBUTE_KEY = AttributeKey.valueOf(DefaultResponseTransmitter.class.getName());

  private final static Logger LOGGER = LoggerFactory.getLogger(NettyHandlerAdapter.class);

  private final Handler[] handlers;

  private final ConcurrentHashMap<Channel, Action<Object>> channelSubscriptions = new ConcurrentHashMap<>(0);

  private final DefaultContext.ApplicationConstants applicationConstants;
  private final ExecController execController;
  private final Predicate<Pair<Long, String>> shouldCompress;

  private Registry rootRegistry;

  private final boolean addResponseTimeHeader;
  private final ExecControl execControl;

  public NettyHandlerAdapter(Stopper stopper, Handler handler, Registry rootRegistry) throws Exception {
    super(false);

    ServerConfig serverConfig = rootRegistry.get(ServerConfig.class);

    this.handlers = ChainHandler.unpack(handler);
    this.rootRegistry = rootRegistry.join(Registries.just(Stopper.class, stopper));
    this.addResponseTimeHeader = serverConfig.isTimeResponses();
    this.applicationConstants = new DefaultContext.ApplicationConstants(this.rootRegistry, new DefaultRenderController(), Handlers.notFound());
    this.execController = rootRegistry.get(ExecController.class);
    this.execControl = execController.getControl();

    if (serverConfig.isCompressResponses()) {
      ImmutableSet<String> blacklist = serverConfig.getCompressionMimeTypeBlackList();
      this.shouldCompress = new ShouldCompressPredicate(
        serverConfig.getCompressionMinSize(),
        serverConfig.getCompressionMimeTypeWhiteList(),
        blacklist.isEmpty() ? ActivationBackedMimeTypes.getDefaultExcludedMimeTypes() : blacklist
      );
    } else {
      this.shouldCompress = Predicates.alwaysFalse();
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
    if (!(msg instanceof FullHttpRequest)) {
      Action<Object> subscriber = channelSubscriptions.get(channelHandlerContext.channel());
      if (subscriber != null) {
        subscriber.execute(msg);
        return;
      }
    }
    super.channelRead(channelHandlerContext, msg);
  }

  public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest nettyRequest) throws Exception {
    if (!nettyRequest.decoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      nettyRequest.release();
      return;
    }

    final long startTime = addResponseTimeHeader ? System.nanoTime() : 0;

    final Channel channel = ctx.channel();
    InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
    InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();

    final ServerConfig serverConfig = rootRegistry.get(ServerConfig.class);
    final Request request = new DefaultRequest(new NettyHeadersBackedHeaders(nettyRequest.headers()), nettyRequest.method(), nettyRequest.uri(), remoteAddress, socketAddress, nettyRequest.content());
    final HttpHeaders nettyHeaders = new DefaultHttpHeaders(false);
    final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyHeaders);
    final DefaultEventController<RequestOutcome> requestOutcomeEventController = new DefaultEventController<>();
    final AtomicBoolean transmitted = new AtomicBoolean(false);

    final DefaultResponseTransmitter responseTransmitter = new DefaultResponseTransmitter(transmitted, execControl, channel, nettyRequest, request, nettyHeaders, requestOutcomeEventController, serverConfig.isCompressResponses(), shouldCompress, startTime);

    final Response response = new DefaultResponse(execControl, responseHeaders, ctx.alloc(), responseTransmitter);
    ctx.attr(RESPONSE_TRANSMITTER_ATTRIBUTE_KEY).set(responseTransmitter);

    Action<Action<Object>> subscribeHandler = thing -> {
      transmitted.set(true);
      channelSubscriptions.put(channel, thing);
      channel.closeFuture().addListener(future -> channelSubscriptions.remove(channel));
    };

    final DirectChannelAccess directChannelAccess = new DefaultDirectChannelAccess(channel, subscribeHandler);

    final DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(
      applicationConstants, request, response, directChannelAccess, requestOutcomeEventController.getRegistry()
    );

    DefaultContext.start(channel.eventLoop(), execController.getControl(), requestConstants, rootRegistry, handlers, execution -> {
      if (!transmitted.get()) {
        Handler lastHandler = requestConstants.handler;
        StringBuilder description = new StringBuilder();
        description
          .append("No response sent for ")
          .append(request.getMethod().getName())
          .append(" request to ")
          .append(request.getUri())
          .append(" (last handler: ");

        if (lastHandler instanceof DescribingHandler) {
          ((DescribingHandler) lastHandler).describeTo(description);
        } else {
          DescribingHandlers.describeTo(lastHandler, description);
        }

        description.append(")");
        String message = description.toString();
        LOGGER.warn(message);

        response.status(500);

        if (serverConfig.isDevelopment()) {
          response.send(message);
        } else {
          response.send();
        }
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
    ctx.attr(RESPONSE_TRANSMITTER_ATTRIBUTE_KEY).get().writabilityChanged();
  }

  private boolean isIgnorableException(Throwable throwable) {
    // There really does not seem to be a better way of detecting this kind of exception
    return throwable instanceof IOException && throwable.getMessage().equals("Connection reset by peer");
  }

  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderConstants.CONTENT_TYPE, HttpHeaderConstants.PLAIN_TEXT_UTF8);

    // Close the connection as soon as the error message is sent.
    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }
}
