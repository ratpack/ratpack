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

package org.ratpackframework.server.internal;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.launch.LaunchConfig;

public class RatpackChannelInitializer extends ChannelInitializer<SocketChannel> {

  private NettyHandlerAdapter nettyHandlerAdapter;

  public RatpackChannelInitializer(LaunchConfig launchConfig, Handler handler) {
    ListeningExecutorService blockingExecutorService = MoreExecutors.listeningDecorator(launchConfig.getBlockingExecutorService());
    this.nettyHandlerAdapter = new NettyHandlerAdapter(handler, launchConfig, blockingExecutorService);
  }

  public void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("decoder", new HttpRequestDecoder());
    pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
    pipeline.addLast("encoder", new HttpResponseEncoder());
    pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
    pipeline.addLast("handler", nettyHandlerAdapter);
  }
}