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
import io.netty.channel.pool.*;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.*;
import ratpack.server.ServerConfig;
import ratpack.util.internal.ChannelImplDetector;

import java.net.URI;
import java.time.Duration;

public class DefaultHttpClient implements HttpClientInternal {

  private static final ChannelHealthChecker ALWAYS_UNHEALTHY = channel ->
    channel.eventLoop().newSucceededFuture(Boolean.FALSE);

  private static final ChannelPoolHandler NOOP_HANDLER = new AbstractChannelPoolHandler() {
    @Override
    public void channelCreated(Channel ch) throws Exception {}
  };


  private final HttpChannelPoolMap channelPoolMap = new HttpChannelPoolMap() {
    @Override
    protected ChannelPool newPool(HttpChannelKey key) {
      Bootstrap bootstrap = new Bootstrap()
        .remoteAddress(key.host, key.port)
        .group(Execution.current().getEventLoop())
        .channel(ChannelImplDetector.getSocketChannelImpl())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) key.connectTimeout.toMillis())
        .option(ChannelOption.ALLOCATOR, byteBufAllocator)
        .option(ChannelOption.SO_KEEPALIVE, isPooling());

      if (isPooling()) {
        return new FixedChannelPool(bootstrap, NOOP_HANDLER, getPoolSize());
      } else {
        return new SimpleChannelPool(bootstrap, NOOP_HANDLER, ALWAYS_UNHEALTHY);
      }
    }
  };

  private final ByteBufAllocator byteBufAllocator;
  private final int maxContentLength;
  private final int poolSize;
  private final Duration readTimeout;

  private DefaultHttpClient(ByteBufAllocator byteBufAllocator, int maxContentLength, int poolSize, Duration readTimeout) {
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLength = maxContentLength;
    this.poolSize = poolSize;
    this.readTimeout = readTimeout;
  }

  @Override
  public int getPoolSize() {
    return poolSize;
  }

  private boolean isPooling() {
    return getPoolSize() > 0;
  }

  @Override
  public HttpChannelPoolMap getChannelPoolMap() {
    return channelPoolMap;
  }

  public ByteBufAllocator getByteBufAllocator() {
    return byteBufAllocator;
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  @Override
  public void close() {
    channelPoolMap.close();
  }

  public static HttpClient of(Action<? super HttpClientSpec> action) throws Exception {
    DefaultHttpClient.Spec spec = new DefaultHttpClient.Spec();
    action.execute(spec);

    return new DefaultHttpClient(
      spec.byteBufAllocator,
      spec.maxContentLength,
      spec.poolSize,
      spec.readTimeout
    );
  }

  private static class Spec implements HttpClientSpec {

    private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
    private int poolSize;
    private int maxContentLength = ServerConfig.DEFAULT_MAX_CONTENT_LENGTH;
    private Duration readTimeout = Duration.ofSeconds(30);

    private Spec() {
    }

    @Override
    public HttpClientSpec poolSize(int poolSize) {
      this.poolSize = poolSize;
      return this;
    }

    @Override
    public HttpClientSpec byteBufAllocator(ByteBufAllocator byteBufAllocator) {
      this.byteBufAllocator = byteBufAllocator;
      return this;
    }

    @Override
    public HttpClientSpec maxContentLength(int maxContentLength) {
      this.maxContentLength = maxContentLength;
      return this;
    }

    @Override
    public HttpClientSpec readTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }
  }

  @Override
  public Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action) {
    return request(uri, action);
  }

  @Override
  public Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action) {
    return request(uri, action.prepend(RequestSpec::post));
  }

  @Override
  public Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return Promise.async(new ContentAggregatingRequestAction(uri, this, 0, Execution.current(), requestConfigurer));
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return Promise.async(new ContentStreamingRequestAction(uri, this, 0, Execution.current(), requestConfigurer));
  }

}
