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

package org.ratpackframework.bootstrap.internal;

import com.google.common.util.concurrent.AbstractIdleService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.ratpackframework.util.Action;
import org.ratpackframework.bootstrap.RatpackServer;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NettyRatpackServer extends AbstractIdleService implements RatpackServer {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final InetSocketAddress requestedAddress;
  private InetSocketAddress boundAddress;
  private final ChannelInitializer<SocketChannel> channelInitializer;
  private final Action<RatpackServer> init;
  private Channel channel;
  private ServerBootstrap bootstrap;
  private NioEventLoopGroup bossGroup;
  private NioEventLoopGroup workerGroup;

  public NettyRatpackServer(
      InetSocketAddress requestedAddress,
      ChannelInitializer<SocketChannel> channelInitializer,
      Action<RatpackServer> init
  ) {
    this.requestedAddress = requestedAddress;
    this.channelInitializer = channelInitializer;
    this.init = init;
  }

  @Override
  protected void startUp() throws Exception {

    bootstrap = new ServerBootstrap();
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(channelInitializer);

    channel = bootstrap.bind(requestedAddress).sync().channel();
    boundAddress = (InetSocketAddress) channel.localAddress();

    init.execute(this);

    if (logger.isLoggable(Level.INFO)) {
      logger.info(String.format("Ratpack started for http://%s:%s", getBindHost(), getBindPort()));
    }
  }

  @Override
  protected void shutDown() throws Exception {
    channel.close().sync();
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }

  public int getBindPort() {
    return boundAddress == null ? -1 : boundAddress.getPort();
  }

  public String getBindHost() {
    return boundAddress == null ? null : boundAddress.getAddress().getHostName();
  }

}
