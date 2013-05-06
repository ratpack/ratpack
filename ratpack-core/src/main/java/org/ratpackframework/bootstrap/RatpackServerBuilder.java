package org.ratpackframework.bootstrap;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.ratpackframework.Action;
import org.ratpackframework.http.Handler;
import org.ratpackframework.bootstrap.internal.NettyRatpackServer;
import org.ratpackframework.bootstrap.internal.NoopInit;
import org.ratpackframework.http.internal.NettyRoutingAdapter;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RatpackServerBuilder {

  public static final int DEFAULT_PORT = 5050;
  private final Handler handler;

  private int port = DEFAULT_PORT;
  private String host = null;
  private Action<RatpackServer> init = new NoopInit();
  private Executor appExecutor = Executors.newCachedThreadPool();
  private Executor ioExecutor = Executors.newCachedThreadPool();
  private int maxIoThreads = Runtime.getRuntime().availableProcessors() * 2;

  public RatpackServerBuilder(Handler handler) {
    this.handler = handler;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Action<RatpackServer> getInit() {
    return init;
  }

  public void setInit(Action<RatpackServer> init) {
    this.init = init;
  }

  public Executor getAppExecutor() {
    return appExecutor;
  }

  public void setAppExecutor(Executor appExecutor) {
    this.appExecutor = appExecutor;
  }

  public Executor getIoExecutor() {
    return ioExecutor;
  }

  public void setIoExecutor(Executor ioExecutor) {
    this.ioExecutor = ioExecutor;
  }

  public int getMaxIoThreads() {
    return maxIoThreads;
  }

  public void setMaxIoThreads(int maxIoThreads) {
    this.maxIoThreads = maxIoThreads;
  }

  public RatpackServer build() {
    InetSocketAddress address = buildSocketAddress();
    ChannelFactory channelFactory = buildChannelFactory();
    ChannelGroup channelGroup = buildChannelGroup();
    ChannelPipelineFactory channelPipelineFactory = buildChannelPipelineFactory(buildNettyAdapter());

    return new NettyRatpackServer(
        address, channelFactory, channelGroup, channelPipelineFactory, init
    );
  }

  protected InetSocketAddress buildSocketAddress() {
    return (host == null) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
  }

  protected ChannelFactory buildChannelFactory() {
    return new NioServerSocketChannelFactory(appExecutor, ioExecutor, maxIoThreads);
  }

  protected SimpleChannelUpstreamHandler buildNettyAdapter() {
    return new NettyRoutingAdapter(handler);
  }

  protected ChannelPipelineFactory buildChannelPipelineFactory(final SimpleChannelUpstreamHandler appHandler) {
    return new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
        return Channels.pipeline(new HttpRequestDecoder(), new HttpResponseEncoder(), new HttpContentCompressor(), appHandler);
      }
    };
  }

  private ChannelGroup buildChannelGroup() {
    return new DefaultChannelGroup("ratpack");
  }

}
