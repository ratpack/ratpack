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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import ratpack.file.internal.SmartHttpContentCompressor;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.server.Stopper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class RatpackChannelInitializer extends ChannelInitializer<SocketChannel> {

  private final boolean compressResponses;
  private NettyHandlerAdapter nettyHandlerAdapter;
  private SSLContext sslContext;
  private int maxContentLength;

  public RatpackChannelInitializer(LaunchConfig launchConfig, Handler handler, Stopper stopper) {
    this.nettyHandlerAdapter = new NettyHandlerAdapter(stopper, handler, launchConfig);
    this.sslContext = launchConfig.getSSLContext();
    this.maxContentLength = launchConfig.getMaxContentLength();
    this.compressResponses = launchConfig.isCompressResponses();
  }

  public void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();

    if (sslContext != null) {
      SSLEngine engine = sslContext.createSSLEngine();
      engine.setUseClientMode(false);
      pipeline.addLast("ssl", new SslHandler(engine));
    }

    pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false));
    pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
    pipeline.addLast("encoder", new HttpResponseEncoder());
    if (compressResponses) {
      pipeline.addLast("deflater", new SmartHttpContentCompressor());
    }
    pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
    pipeline.addLast("handler", nettyHandlerAdapter);
  }
}