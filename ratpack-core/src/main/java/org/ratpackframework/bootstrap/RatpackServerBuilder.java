package org.ratpackframework.bootstrap;

import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.ratpackframework.Action;
import org.ratpackframework.bootstrap.internal.NettyRatpackServer;
import org.ratpackframework.bootstrap.internal.NoopInit;
import org.ratpackframework.http.internal.NettyRoutingAdapter;
import org.ratpackframework.routing.Handler;

import java.net.InetSocketAddress;

public class RatpackServerBuilder {

  public static final int DEFAULT_PORT = 5050;
  private final Handler handler;

  private int port = DEFAULT_PORT;
  private String host = null;
  private Action<RatpackServer> init = new NoopInit();
  private EventExecutorGroup appExecutor = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 2);

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

  public EventExecutorGroup getAppExecutor() {
    return appExecutor;
  }

  public void setAppExecutor(EventExecutorGroup appExecutor) {
    this.appExecutor = appExecutor;
  }

  public RatpackServer build() {
    InetSocketAddress address = buildSocketAddress();
    ChannelInitializer<SocketChannel> channelInitializer = buildChannelInitializer();
    return new NettyRatpackServer(
        address, channelInitializer, init
    );
  }

  private ChannelInitializer<SocketChannel> buildChannelInitializer() {
    return new ChannelInitializer<SocketChannel>() {
      public void initChannel(SocketChannel ch) throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

        pipeline.addLast(appExecutor, "handler", buildNettyAdapter());
      }
    };
  }

  protected InetSocketAddress buildSocketAddress() {
    return (host == null) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
  }


  protected ChannelInboundMessageHandlerAdapter<FullHttpRequest> buildNettyAdapter() {
    return new NettyRoutingAdapter(handler);
  }

}
