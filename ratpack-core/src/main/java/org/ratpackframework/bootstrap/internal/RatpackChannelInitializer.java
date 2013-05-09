package org.ratpackframework.bootstrap.internal;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.ratpackframework.bootstrap.RatpackServerBuilder;
import org.ratpackframework.bootstrap.internal.NettyRoutingAdapter;
import org.ratpackframework.routing.Handler;

public class RatpackChannelInitializer extends ChannelInitializer<SocketChannel> {

  private final int workerThreads;
  private final Handler handler;

  public RatpackChannelInitializer(int workerThreads, Handler handler) {
    this.workerThreads = workerThreads;
    this.handler = handler;
  }

  public void initChannel(SocketChannel ch) throws Exception {
    // Create a default pipeline implementation.
    ChannelPipeline pipeline = ch.pipeline();

    pipeline.addLast("decoder", new HttpRequestDecoder());
    pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
    pipeline.addLast("encoder", new HttpResponseEncoder());
    pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

    NettyRoutingAdapter nettyRoutingAdapter = new NettyRoutingAdapter(handler);

    if (workerThreads > 0) {
      pipeline.addLast(new DefaultEventExecutorGroup(workerThreads), "handler", nettyRoutingAdapter);
    } else {
      pipeline.addLast("handler", nettyRoutingAdapter);
    }
  }
}