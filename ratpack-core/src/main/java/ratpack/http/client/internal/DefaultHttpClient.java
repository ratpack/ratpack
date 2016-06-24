/*
 * Copyright 2016 the original author or authors.
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

package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;
import ratpack.util.internal.ChannelImplDetector;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class DefaultHttpClient implements HttpClient {
  public static final Logger LOGGER = LoggerFactory.getLogger(DefaultHttpClient.class);

  private ChannelPoolMap<URI, ChannelPool> channelPoolMap;
  private int maxContentLengthBytes;
  private PooledHttpConfig config;
  private ByteBufAllocator byteBufAllocator;
  private Bootstrap baseBoostrap;
  private ExecController execController;

  public DefaultHttpClient(ByteBufAllocator byteBufAllocator, int maxContentLengthBytes, ExecController execController) {
    this(new PooledHttpConfig(), byteBufAllocator, maxContentLengthBytes, execController);
  }

  public DefaultHttpClient(PooledHttpConfig config, ByteBufAllocator byteBufAllocator, int maxContentLengthBytes, ExecController execController) {
    this.config = config;
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLengthBytes = maxContentLengthBytes;
    this.execController = execController;

    baseBoostrap = new Bootstrap();
    baseBoostrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .channel(ChannelImplDetector.getSocketChannelImpl())
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)TimeUnit.NANOSECONDS.toMillis(config.getConnectionTimeoutNanos()))
      .group(execController.getEventLoopGroup());

    channelPoolMap = new AbstractChannelPoolMap<URI, ChannelPool>() {
      @Override
      protected ChannelPool newPool(URI uri) {
        if (config.isPooled()) {
          return createPooledPool(uri);
        } else {
          return createNonPooledPool(uri);
        }
      }
    };
  }

  @Override
  public Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action) {
    return request(uri, action);
  }

  private static class Post implements Action<RequestSpec> {
    @Override
    public void execute(RequestSpec requestSpec) throws Exception {
      requestSpec.method("POST");
    }
  }

  @Override
  public Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action) {
    return request(uri, new Post().append(action));
  }

  @Override
  public Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return Promise.async(downstream -> new ContentAggregatingRequestAction(requestConfigurer, channelPoolMap, config, uri, this.byteBufAllocator, this.maxContentLengthBytes, Execution.current(), 0).connect(downstream));
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return Promise.async(downstream -> new ContentStreamingRequestAction(requestConfigurer, channelPoolMap, config, uri, this.byteBufAllocator, Execution.current(), 0).connect(downstream));
  }

  private ChannelPool createPooledPool(URI uri) {
    baseBoostrap.option(ChannelOption.SO_KEEPALIVE, true);
    ChannelPoolHandler handler = createChannelPoolHandler(uri);
    return new FixedChannelPool(baseBoostrap, handler, config.getMaxConnections());
  }

  private ChannelPool createNonPooledPool(URI uri) {
    baseBoostrap.option(ChannelOption.SO_KEEPALIVE, false);
    ChannelPoolHandler handler = createChannelPoolHandler(uri);
    return new NonPoolingChannelPool(baseBoostrap, handler);
  }

  private ChannelPoolHandler createChannelPoolHandler(URI uri) {
    String scheme = uri.getScheme();
    boolean useSsl = false;
    if (scheme.equals("https")) {
      useSsl = true;
    } else if (!scheme.equals("http")) {
      throw new IllegalArgumentException(String.format("URL '%s' is not a http url", uri.toString()));
    }

    String host = uri.getHost();
    int port = uri.getPort() < 0 ? (useSsl ? 443 : 80) : uri.getPort();
    baseBoostrap.remoteAddress(host, port);

    return new AbstractChannelPoolHandler() {
      @Override
      public void channelCreated(Channel ch) throws Exception {
      }
    };
  }

}
