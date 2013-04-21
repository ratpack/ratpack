package org.ratpackframework.bootstrap.internal;

import com.google.common.util.concurrent.AbstractIdleService;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.handler.Handler;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NettyRatpackServer extends AbstractIdleService implements RatpackServer {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final InetSocketAddress requestedAddress;
  private InetSocketAddress boundAddress;
  private final ChannelFactory channelFactory;
  private final ChannelGroup channelGroup;
  private final Handler<RatpackServer> init;
  private final ChannelPipelineFactory channelPipelineFactory;

  public NettyRatpackServer(
      InetSocketAddress requestedAddress,
      ChannelFactory channelFactory, ChannelGroup channelGroup, ChannelPipelineFactory channelPipelineFactory,
      Handler<RatpackServer> init
  ) {
    this.requestedAddress = requestedAddress;
    this.channelFactory = channelFactory;
    this.channelGroup = channelGroup;
    this.channelPipelineFactory = channelPipelineFactory;
    this.init = init;
  }

  @Override
  protected void startUp() {
    ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
    bootstrap.setPipelineFactory(channelPipelineFactory);
    bootstrap.setOption("child.tcpNoDelay", true);
    bootstrap.setOption("child.keepAlive", true);
    Channel channel = bootstrap.bind(requestedAddress);
    boundAddress = ((InetSocketAddress) channel.getLocalAddress());
    channelGroup.add(channel);

    init.handle(this);

    if (logger.isLoggable(Level.INFO)) {
      logger.info(String.format("Ratpack started for http://%s:%s", getBindHost(), getBindPort()));
    }
  }

  @Override
  protected void shutDown() {
    channelGroup.close().awaitUninterruptibly();
    channelFactory.releaseExternalResources();
  }

  @Override
  public int getBindPort() {
    return boundAddress == null ? -1 : boundAddress.getPort();
  }

  @Override
  public String getBindHost() {
    return boundAddress == null ? null : boundAddress.getAddress().getHostName();
  }

}
