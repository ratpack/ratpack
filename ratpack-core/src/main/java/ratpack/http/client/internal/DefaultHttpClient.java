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
import ratpack.exec.internal.ExecControllerInternal;
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

    @Override
    public void channelReleased(Channel ch) throws Exception {
    }
  };

  private static final ChannelPoolHandler POOLING_HANDLER = new AbstractChannelPoolHandler() {
    @Override
    public void channelCreated(Channel ch) throws Exception {

    }

    @Override
    public void channelReleased(Channel ch) throws Exception {
      if (ch.isOpen()) {
        ch.config().setAutoRead(true);
        ch.pipeline().addLast(IdlingConnectionHandler.INSTANCE);
      }
    }

    @Override
    public void channelAcquired(Channel ch) throws Exception {
      ch.pipeline().remove(IdlingConnectionHandler.INSTANCE);
    }
  };

  private final HttpChannelPoolMap channelPoolMap = new HttpChannelPoolMap() {
    @Override
    protected ChannelPool newPool(HttpChannelKey key) {
      Bootstrap bootstrap = new Bootstrap()
        .remoteAddress(key.host, key.port)
        .group(key.execution.getEventLoop())
        .channel(ChannelImplDetector.getSocketChannelImpl())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) key.connectTimeout.toMillis())
        .option(ChannelOption.ALLOCATOR, spec.byteBufAllocator)
        .option(ChannelOption.AUTO_READ, false)
        .option(ChannelOption.SO_KEEPALIVE, isPooling());

      if (isPooling()) {
        ChannelPool channelPool = new FixedChannelPool(bootstrap, POOLING_HANDLER, getPoolSize());
        ((ExecControllerInternal) key.execution.getController()).onClose(() -> {
          remove(key);
          channelPool.close();
        });
        return channelPool;
      } else {
        return new SimpleChannelPool(bootstrap, NOOP_HANDLER, ALWAYS_UNHEALTHY);
      }
    }
  };

  private final DefaultHttpClient.Spec spec;

  private DefaultHttpClient(DefaultHttpClient.Spec spec) {
    this.spec = spec;
  }

  @Override
  public int getPoolSize() {
    return spec.poolSize;
  }

  private boolean isPooling() {
    return getPoolSize() > 0;
  }

  @Override
  public HttpChannelPoolMap getChannelPoolMap() {
    return channelPoolMap;
  }

  @Override
  public Action<? super RequestSpec> getRequestInterceptor() {
    return spec.requestInterceptor;
  }

  @Override
  public Action<? super HttpResponse> getResponseInterceptor() {
    return spec.responseInterceptor;
  }

  public ByteBufAllocator getByteBufAllocator() {
    return spec.byteBufAllocator;
  }

  public int getMaxContentLength() {
    return spec.maxContentLength;
  }

  @Override
  public int getMaxResponseChunkSize() {
    return spec.responseMaxChunkSize;
  }

  public Duration getReadTimeout() {
    return spec.readTimeout;
  }

  public Duration getConnectTimeout() {
    return spec.connectTimeout;
  }

  @Override
  public void close() {
    channelPoolMap.close();
  }

  @Override
  public HttpClient copyWith(Action<? super HttpClientSpec> action) throws Exception {
    return of(new DefaultHttpClient.Spec(spec), action);
  }

  public static HttpClient of(Action<? super HttpClientSpec> action) throws Exception {
    DefaultHttpClient.Spec spec = new DefaultHttpClient.Spec();
    return of(spec, action);
  }

  private static HttpClient of(DefaultHttpClient.Spec spec, Action<? super HttpClientSpec> action) throws Exception {
    action.execute(spec);

    return new DefaultHttpClient(
      spec
    );
  }

  private static class Spec implements HttpClientSpec {

    private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
    private int poolSize;
    private int maxContentLength = ServerConfig.DEFAULT_MAX_CONTENT_LENGTH;
    private int responseMaxChunkSize = 8192;
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration connectTimeout = Duration.ofSeconds(30);
    private Action<? super RequestSpec> requestInterceptor = Action.noop();
    private Action<? super HttpResponse> responseInterceptor = Action.noop();

    private Spec() {
    }

    private Spec(Spec spec) {
      this.byteBufAllocator = spec.byteBufAllocator;
      this.poolSize = spec.poolSize;
      this.maxContentLength = spec.maxContentLength;
	    this.responseMaxChunkSize = spec.responseMaxChunkSize;
      this.readTimeout = spec.readTimeout;
	    this.connectTimeout = spec.connectTimeout;
      this.requestInterceptor = spec.requestInterceptor;
      this.responseInterceptor = spec.responseInterceptor;
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
    public HttpClientSpec responseMaxChunkSize(int numBytes) {
      this.responseMaxChunkSize = numBytes;
      return this;
    }

    @Override
    public HttpClientSpec readTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    @Override
    public HttpClientSpec connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    @Override
    public HttpClientSpec requestIntercept(Action<? super RequestSpec> interceptor) {
      requestInterceptor = requestInterceptor.append(interceptor);
      return this;
    }

    @Override
    public HttpClientSpec responseIntercept(Action<? super HttpResponse> interceptor) {
      responseInterceptor = responseInterceptor.append(interceptor);
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
    return Promise.async(downstream -> new ContentAggregatingRequestAction(uri, this, 0, Execution.current(), requestConfigurer.append(spec.requestInterceptor)).connect(downstream));
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return Promise.async(downstream -> new ContentStreamingRequestAction(uri, this, 0, Execution.current(), requestConfigurer.append(spec.requestInterceptor)).connect(downstream));
  }

}
