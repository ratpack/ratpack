package org.ratpackframework.bootstrap.internal;

import com.google.common.util.concurrent.AbstractIdleService;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.Handler;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NettyRatpackServer extends AbstractIdleService implements RatpackServer {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final InetSocketAddress address;
  private final ChannelFactory channelFactory;
  private final ChannelGroup channelGroup;
  private final Handler<RatpackServer> init;
  private final ChannelPipelineFactory channelPipelineFactory;

  @Inject
  public NettyRatpackServer(
      InetSocketAddress address,
      ChannelFactory channelFactory, ChannelGroup channelGroup, ChannelPipelineFactory channelPipelineFactory,
      Handler<RatpackServer> init
  ) {
    this.address = address;
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
    Channel channel = bootstrap.bind(address);
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
    return address.getPort();
  }

  @Override
  public String getBindHost() {
    return address.getHostName();
  }

}
