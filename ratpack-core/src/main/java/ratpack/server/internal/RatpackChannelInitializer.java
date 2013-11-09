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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class RatpackChannelInitializer extends ChannelInitializer<SocketChannel> {

  private NettyHandlerAdapter nettyHandlerAdapter;
  private SSLContext sslContext;
  private int maxContentLength;

  public RatpackChannelInitializer(LaunchConfig launchConfig, Handler handler) {
    ListeningExecutorService blockingExecutorService = MoreExecutors.listeningDecorator(launchConfig.getBlockingExecutorService());
    this.nettyHandlerAdapter = new NettyHandlerAdapter(handler, launchConfig, blockingExecutorService);
    this.sslContext = launchConfig.getSSLContext();
    this.maxContentLength = launchConfig.getMaxContentLength();
  }

  public void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();

    if (sslContext != null) {
      SSLEngine engine = sslContext.createSSLEngine();
      engine.setUseClientMode(false);
      pipeline.addLast("ssl", new SslHandler(engine));
    }

    pipeline.addLast("decoder", new HttpRequestDecoder());
    pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
    pipeline.addLast("encoder", new HttpResponseEncoder());
    pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
    pipeline.addLast("handler", nettyHandlerAdapter);
  }
}