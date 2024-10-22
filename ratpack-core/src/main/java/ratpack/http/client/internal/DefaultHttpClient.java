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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.resolver.AddressResolverGroup;
import ratpack.api.Nullable;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.*;
import ratpack.util.internal.TransportDetector;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultHttpClient implements HttpClientInternal {

  private static final ChannelHealthChecker ALWAYS_UNHEALTHY = channel ->
    channel.eventLoop().newSucceededFuture(Boolean.FALSE);

  final ByteBufAllocator byteBufAllocator;
  final int poolSize;
  final int poolQueueSize;
  final Duration idleTimeout;
  final int maxContentLength;
  final int responseMaxChunkSize;
  final Duration readTimeout;
  final Duration connectTimeout;
  final Action<? super RequestSpec> requestInterceptor;
  final Action<? super HttpResponse> responseInterceptor;
  final Action<? super Throwable> errorInterceptor;
  final boolean enableMetricsCollection;
  final AddressResolverGroup<?> resolver;

  @Nullable
  final ProxyInternal proxy;

  private final Cache<String, ChannelPoolStats> hostStats = Caffeine.newBuilder()
    .maximumSize(1024)
    .build();

  private final ManagedChannelPoolMap channelPoolMap;

  public DefaultHttpClient(
    ByteBufAllocator byteBufAllocator,
    int poolSize,
    int poolQueueSize,
    Duration idleTimeout,
    int maxContentLength,
    int responseMaxChunkSize,
    Duration readTimeout,
    Duration connectTimeout,
    Action<? super RequestSpec> requestInterceptor,
    Action<? super HttpResponse> responseInterceptor,
    Action<? super Throwable> errorInterceptor,
    boolean enableMetricsCollection,
    AddressResolverGroup<?> resolver,
    @Nullable ProxyInternal proxy
  ) {
    this.byteBufAllocator = byteBufAllocator;
    this.poolSize = poolSize;
    this.poolQueueSize = poolQueueSize;
    this.idleTimeout = idleTimeout;
    this.maxContentLength = maxContentLength;
    this.responseMaxChunkSize = responseMaxChunkSize;
    this.readTimeout = readTimeout;
    this.connectTimeout = connectTimeout;
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.errorInterceptor = errorInterceptor;
    this.enableMetricsCollection = enableMetricsCollection;
    this.resolver = resolver;
    this.proxy = proxy;

    this.channelPoolMap = isPooling() ? getPoolingChannelManager() : getSimpleChannelManager();
  }

  private ManagedChannelPoolMap getPoolingChannelManager() {
    final HttpChannelPoolMap channelPoolMap = new HttpChannelPoolMap() {
      @Override
      protected ChannelPool newPool(HttpChannelKey key) {
        Bootstrap bootstrap = createBootstrap(key, true);

        InstrumentedChannelPoolHandler channelPoolHandler = getPoolingHandler(key);
        if (enableMetricsCollection) {
          hostStats.put(key.host, channelPoolHandler);
        }
        CleanClosingFixedChannelPool channelPool = new CleanClosingFixedChannelPool(bootstrap, channelPoolHandler, getPoolSize(), getPoolQueueSize());
        key.execController.onClose(() -> {
          remove(key);
          channelPool.closeCleanly();
        });
        return channelPool;
      }
    };

    return channelPoolMap;
  }

  private ManagedChannelPoolMap getSimpleChannelManager() {
    return new ManagedChannelPoolMap() {

      @Override
      public void close() {

      }

      @Override
      public ChannelPool get(HttpChannelKey key) {
        Bootstrap bootstrap = createBootstrap(key, true);
        return new SimpleChannelPool(bootstrap, getSimpleHandler(key), ALWAYS_UNHEALTHY);
      }

      @Override
      public boolean contains(HttpChannelKey key) {
        return false;
      }
    };
  }

  private Bootstrap createBootstrap(HttpChannelKey key, boolean pooling) {
    Bootstrap bootstrap = new Bootstrap()
      .remoteAddress(key.host, key.port)
      .group(key.eventLoop)
      .resolver(resolver)
      .channel(TransportDetector.getSocketChannelImpl())
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) key.connectTimeout.toMillis())
      .option(ChannelOption.ALLOCATOR, byteBufAllocator)
      .option(ChannelOption.AUTO_READ, false)
      .option(ChannelOption.SO_KEEPALIVE, pooling);
    return bootstrap;
  }

  private InstrumentedChannelPoolHandler getPoolingHandler(HttpChannelKey key) {
    if (enableMetricsCollection) {
      return new InstrumentedFixedChannelPoolHandler(key, getPoolSize(), getIdleTimeout());
    } else {
      return new NoopFixedChannelPoolHandler(key, getIdleTimeout());
    }
  }

  private InstrumentedChannelPoolHandler getSimpleHandler(HttpChannelKey key) {
    if (enableMetricsCollection) {
      return new InstrumentedSimpleChannelPoolHandler(key);
    } else {
      return new NoopSimpleChannelPoolHandler(key);
    }
  }

  @Override
  public int getPoolSize() {
    return poolSize;
  }

  @Override
  public int getPoolQueueSize() {
    return poolQueueSize;
  }

  @Override
  public Duration getIdleTimeout() {
    return idleTimeout;
  }

  private boolean isPooling() {
    return getPoolSize() > 0;
  }

  @Override
  public ChannelPoolMap<HttpChannelKey, ChannelPool> getChannelPoolMap() {
    return channelPoolMap;
  }

  @Override
  public Action<? super RequestSpec> getRequestInterceptor() {
    return requestInterceptor;
  }

  @Override
  public Action<? super HttpResponse> getResponseInterceptor() {
    return responseInterceptor;
  }

  public ByteBufAllocator getByteBufAllocator() {
    return byteBufAllocator;
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }

  @Override
  public int getMaxResponseChunkSize() {
    return responseMaxChunkSize;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  @Override
  public Proxy getProxy() {
    return proxy;
  }

  @Override
  public ProxyInternal getProxyInternal() {
    return proxy;
  }

  @Override
  public void close() {
    channelPoolMap.close();
  }

  @Override
  public HttpClient copyWith(Action<? super HttpClientSpec> action) throws Exception {
    HttpClientBuilder builder = new HttpClientBuilder(this);
    action.execute(builder);
    return builder.build();
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
    return intercept(
      Promise.async(downstream -> {
        ContentAggregatingRequestAction action;
        try {
          action = new ContentAggregatingRequestAction(
            uri,
            this,
            0,
            false,
            Execution.current(),
            requestConfigurer.append(requestInterceptor)
          );
        } catch (Exception e) {
          downstream.error(e);
          return;
        }
        action.connect(downstream);
      }),
      responseInterceptor,
      errorInterceptor
    );
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return intercept(
      Promise.async(downstream -> {
        ContentStreamingRequestAction action;
        try {
          action = new ContentStreamingRequestAction(
            uri,
            this,
            0,
            false,
            Execution.current(),
            requestConfigurer.append(requestInterceptor)
          );
        } catch (Exception e) {
          downstream.error(e);
          return;
        }
        action.connect(downstream);
      }),
      responseInterceptor,
      errorInterceptor
    );
  }

  private <T extends HttpResponse> Promise<T> intercept(Promise<T> promise, Action<? super HttpResponse> action, Action<? super Throwable> errorAction) {
    Promise<T> returnPromise = promise;
    if (errorAction != Action.noop()) {
      returnPromise = promise.wiretap(r -> {
        if (r.isError()) {
          ExecController.require()
            .fork()
            .eventLoop(Execution.current().getEventLoop())
            .start(e ->
              errorAction.execute(r.getThrowable())
            );
        }
      });
    }
    if (action != Action.noop()) {
      returnPromise = returnPromise.next(r ->
        ExecController.require()
          .fork()
          .eventLoop(Execution.current().getEventLoop())
          .start(e ->
            action.execute(r)
          )
      );
    }

    return returnPromise;
  }

  public HttpClientStats getHttpClientStats() {
    return new HttpClientStats(
      hostStats.asMap().entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> e.getValue().getHostStats()
      ))
    );
  }
}
