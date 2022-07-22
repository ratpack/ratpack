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

package ratpack.test.http.internal.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DestinationServerHandler extends ChannelInboundHandlerAdapter {
  public static final Logger LOGGER = LoggerFactory.getLogger(ProxyClientHandler.class);

  private final Channel clientChannel;

  public DestinationServerHandler(Channel clientChannel) {
    this.clientChannel = clientChannel;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    clientChannel.writeAndFlush(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (clientChannel.isActive()) {
      clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.error("ProxyRemoteServerHandler encountered an error: " + cause.getMessage(), cause);
    Channel destinationServerChannel = ctx.channel();
    if (destinationServerChannel != null && destinationServerChannel.isActive()) {
      destinationServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }
}
