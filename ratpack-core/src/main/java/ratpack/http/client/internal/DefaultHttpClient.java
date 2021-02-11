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
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.resolver.AddressResolverGroup;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.exec.internal.ExecControllerInternal;
import ratpack.func.Action;
import ratpack.http.client.*;
import ratpack.server.ServerConfig;
import ratpack.server.internal.ServerRegistry;
import ratpack.util.Exceptions;
import ratpack.util.internal.TransportDetector;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultHttpClient implements HttpClientInternal {

  private static final ChannelHealthChecker ALWAYS_UNHEALTHY = channel ->
    channel.eventLoop().newSucceededFuture(Boolean.FALSE);

  private final Map<String, ChannelPoolStats> hostStats = new ConcurrentHashMap<>();

  private final HttpChannelPoolMap channelPoolMap = new HttpChannelPoolMap() {
    @Override
    protected ChannelPool newPool(HttpChannelKey key) {

      Bootstrap bootstrap = new Bootstrap()
        .remoteAddress(key.host, key.port)
        .group(key.execution.getEventLoop())
        .resolver(key.execution.maybeGet(AddressResolverGroup.class)
          .orElseGet(() -> ServerRegistry.defaultAddressResolver(key.execution.getController()))
        )
        .channel(TransportDetector.getSocketChannelImpl())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) key.connectTimeout.toMillis())
        .option(ChannelOption.ALLOCATOR, spec.byteBufAllocator)
        .option(ChannelOption.AUTO_READ, false)
        .option(ChannelOption.SO_KEEPALIVE, isPooling());

      if (isPooling()) {
        InstrumentedChannelPoolHandler channelPoolHandler = getPoolingHandler(key);
        hostStats.put(key.host, channelPoolHandler);
        CleanClosingFixedChannelPool channelPool = new CleanClosingFixedChannelPool(bootstrap, channelPoolHandler, getPoolSize(), getPoolQueueSize());
        ((ExecControllerInternal) key.execution.getController()).onClose(() -> {
          remove(key);
          channelPool.closeCleanly();
        });
        return channelPool;
      } else {
        InstrumentedChannelPoolHandler channelPoolHandler = getSimpleHandler(key);
        hostStats.put(key.host, channelPoolHandler);
        return new SimpleChannelPool(bootstrap, channelPoolHandler, ALWAYS_UNHEALTHY);
      }
    }
  };

  private final DefaultHttpClient.Spec spec;
  private final ProxyInternal proxy;

  private DefaultHttpClient(DefaultHttpClient.Spec spec) {
    this.spec = spec;

    if (spec.proxy != null) {
      DefaultProxy.Spec proxySpec = new DefaultProxy.Spec();
      Exceptions.uncheck(() -> spec.proxy.execute(proxySpec));
      this.proxy = new DefaultProxy(proxySpec);
    } else {
      proxy = null;
    }
  }

  private InstrumentedChannelPoolHandler getPoolingHandler(HttpChannelKey key) {

    if (spec.enableMetricsCollection) {
      return new InstrumentedFixedChannelPoolHandler(key, getPoolSize(), getIdleTimeout(), proxy);
    }
    return new NoopFixedChannelPoolHandler(key, getIdleTimeout(), proxy);
  }

  private InstrumentedChannelPoolHandler getSimpleHandler(HttpChannelKey key) {
    if (spec.enableMetricsCollection) {
      return new InstrumentedSimpleChannelPoolHandler(key, proxy);
    }
    return new NoopSimpleChannelPoolHandler(key, proxy);
  }

  @Override
  public int getPoolSize() {
    return spec.poolSize;
  }

  @Override
  public int getPoolQueueSize() {
    return spec.poolQueueSize;
  }

  @Override
  public Duration getIdleTimeout() {
    return spec.idleTimeout;
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
  public Proxy getProxy() {
    return proxy;
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

    private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;
    private int poolSize;
    private int poolQueueSize = Integer.MAX_VALUE;
    private Duration idleTimeout = Duration.ofSeconds(0);
    private int maxContentLength = ServerConfig.DEFAULT_MAX_CONTENT_LENGTH;
    private int responseMaxChunkSize = 8192;
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration connectTimeout = Duration.ofSeconds(30);
    private Action<? super RequestSpec> requestInterceptor = Action.noop();
    private Action<? super HttpResponse> responseInterceptor = Action.noop();
    private Action<? super Throwable> errorInterceptor = Action.noop();
    private boolean enableMetricsCollection;
    private Action<? super ProxySpec> proxy;

    private Spec() {
    }

    private Spec(Spec spec) {
      this.byteBufAllocator = spec.byteBufAllocator;
      this.poolSize = spec.poolSize;
      this.poolQueueSize = spec.poolQueueSize;
      this.idleTimeout = spec.idleTimeout;
      this.maxContentLength = spec.maxContentLength;
      this.responseMaxChunkSize = spec.responseMaxChunkSize;
      this.readTimeout = spec.readTimeout;
      this.connectTimeout = spec.connectTimeout;
      this.requestInterceptor = spec.requestInterceptor;
      this.responseInterceptor = spec.responseInterceptor;
      this.enableMetricsCollection = spec.enableMetricsCollection;
      this.proxy = spec.proxy;
    }

    @Override
    public HttpClientSpec poolSize(int poolSize) {
      this.poolSize = poolSize;
      return this;
    }

    @Override
    public HttpClientSpec poolQueueSize(int poolQueueSize) {
      this.poolQueueSize = poolQueueSize;
      return this;
    }

    @Override
    public HttpClientSpec idleTimeout(Duration idleTimeout) {
      this.idleTimeout = idleTimeout;
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

    @Override
    public HttpClientSpec responseIntercept(Operation operation) {
      responseInterceptor = responseInterceptor.append(response -> operation.then());
      return this;
    }

    @Override
    public HttpClientSpec errorIntercept(Action<? super Throwable> interceptor) {
      errorInterceptor = errorInterceptor.append(interceptor);
      return this;
    }

    @Override
    public HttpClientSpec enableMetricsCollection(boolean enableMetricsCollection) {
      this.enableMetricsCollection = enableMetricsCollection;
      return this;
    }

    @Override
    public HttpClientSpec proxy(Action<? super ProxySpec> proxy) {
      this.proxy = proxy;
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
    return intercept(
      Promise.async(downstream -> new ContentAggregatingRequestAction(uri, this, 0, Execution.current(), requestConfigurer.append(spec.requestInterceptor)).connect(downstream)),
      spec.responseInterceptor,
      spec.errorInterceptor
    );
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return intercept(
      Promise.async(downstream -> new ContentStreamingRequestAction(uri, this, 0, Execution.current(), requestConfigurer.append(spec.requestInterceptor)).connect(downstream)),
      spec.responseInterceptor,
      spec.errorInterceptor
    );
  }

  private <T extends HttpResponse> Promise<T> intercept(Promise<T> promise, Action<? super HttpResponse> action, Action<? super Throwable> errorAction) {
    return promise.wiretap(r -> {
      if (r.isError()) {
        ExecController.require()
          .fork()
          .eventLoop(Execution.current().getEventLoop())
          .start(e ->
            errorAction.execute(r.getThrowable())
          );
      }
    })
      .next(r ->
        ExecController.require()
          .fork()
          .eventLoop(Execution.current().getEventLoop())
          .start(e ->
            action.execute(r)
          )
      );
  }

  public HttpClientStats getHttpClientStats() {
    return new HttpClientStats(
      hostStats.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> e.getValue().getHostStats()
      ))
    );
  }
}
