/*
 * Copyright 2015 the original author or authors.
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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
// import io.netty.handler.codec.http.HttpRequest;
// import io.netty.handler.codec.http.HttpRequestDecoder;
// import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import ratpack.server.ServerConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;


public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
  final SSLContext sslContext;
  final ChannelHandler handlerAdapter;
  final ServerConfig serverConfig;

  public NettyChannelInitializer(ChannelHandler channelHandler, ServerConfig config, SSLContext sslContext) {
    this.handlerAdapter = channelHandler;
    this.sslContext = sslContext;
    this.serverConfig = config;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    boolean requireClientSslAuth = serverConfig.isRequireClientSslAuth();
    ChannelPipeline pipeline = ch.pipeline();

    if (sslContext != null) {
      SSLEngine sslEngine = sslContext.createSSLEngine();
      sslEngine.setUseClientMode(false);
      sslEngine.setNeedClientAuth(requireClientSslAuth);
      pipeline.addLast("ssl", new SslHandler(sslEngine));
    }

    // pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false));
    // pipeline.addLast("encoder", new HttpResponseEncoder());
    pipeline.addLast(new HttpServerCodec(4096, 8192, 8192, false));

    pipeline.addLast("deflater", new IgnorableHttpContentCompressor());
    pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
    pipeline.addLast("adapter", handlerAdapter);

    ch.config().setAutoRead(false);
  }
}
