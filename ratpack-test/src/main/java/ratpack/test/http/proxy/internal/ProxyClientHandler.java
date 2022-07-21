/*
 * Copyright 2022 the original author or authors.
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

package ratpack.test.http.proxy.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;

import static ratpack.test.http.proxy.internal.ProxyHandlerNames.HTTP_CODEC_HANDLER;
import static ratpack.test.http.proxy.internal.ProxyHandlerNames.PROXY_AUTH_HANDLER;

public class ProxyClientHandler extends SimpleChannelInboundHandler<HttpRequest> {
  public static final Logger LOGGER = LoggerFactory.getLogger(ProxyClientHandler.class);

  private Channel clientChannel;
  private Channel destinationServerChannel;

  private final ConcurrentMap<Authority, Integer> numTunnelsByDestination;

  public ProxyClientHandler(ConcurrentMap<Authority, Integer> numTunnelsByDestination) {
    this.numTunnelsByDestination = numTunnelsByDestination;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    clientChannel = ctx.channel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (tunnelIsEstablished()) {
      destinationServerChannel.writeAndFlush(msg);
    } else {
      // We're still within the HTTP protocol
      super.channelRead(ctx, msg);
    }
  }

  private boolean tunnelIsEstablished() {
    return destinationServerChannel != null && destinationServerChannel.isActive();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
    if (HttpMethod.CONNECT.equals(request.method())) {
      clientChannel.config().setAutoRead(false);

      Authority destination = Authority.fromString(request.uri());
      LOGGER.info("Received CONNECT request from {} to {}", clientChannel.remoteAddress(), destination);

      ChannelFuture channelFuture = new Bootstrap()
        .group(clientChannel.eventLoop())
        .channel(clientChannel.getClass())
        .handler(new DestinationServerHandler(clientChannel))
        .connect(destination.getHost(), destination.getPort());

      destinationServerChannel = channelFuture.channel();

      channelFuture.addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            numTunnelsByDestination.merge(destination, 1, Integer::sum);
            signalTunnelEstablished(clientChannel);
            stopProcessingAsHttp(clientChannel);
            clientChannel.config().setAutoRead(true);
            LOGGER.info("Tunnel established from client {} to destination {}", clientChannel.remoteAddress(), destinationServerChannel.remoteAddress());
          } else {
            clientChannel.close();
          }
        });
    } else {
      clientChannel.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED));
    }
  }

  private static void signalTunnelEstablished(Channel clientChannel) {
    clientChannel.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
  }

  private static void stopProcessingAsHttp(Channel clientChannel) {
    if (clientChannel.pipeline().get(HTTP_CODEC_HANDLER) != null) {
      clientChannel.pipeline().remove(HTTP_CODEC_HANDLER);
    }
    if (clientChannel.pipeline().get(PROXY_AUTH_HANDLER) != null) {
      clientChannel.pipeline().remove(PROXY_AUTH_HANDLER);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (destinationServerChannel != null && destinationServerChannel.isActive()) {
      destinationServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
      LOGGER.info("Tunnel from client {} to destination {} closed", clientChannel.remoteAddress(), destinationServerChannel.remoteAddress());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.error("ProxyClientHandler encountered an error: " + cause.getMessage(), cause);
    if (clientChannel != null && clientChannel.isActive()) {
      clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }
}
