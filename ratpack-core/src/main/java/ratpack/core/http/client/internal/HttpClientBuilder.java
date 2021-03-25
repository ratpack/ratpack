/*
 * Copyright 2021 the original author or authors.
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

package ratpack.core.http.client.internal;

import io.netty.buffer.ByteBufAllocator;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import ratpack.core.http.client.*;
import ratpack.core.server.ServerConfig;
import ratpack.exec.ExecController;
import ratpack.exec.Operation;
import ratpack.exec.util.internal.TransportDetector;
import ratpack.func.Action;
import ratpack.func.Exceptions;

import java.time.Duration;
import java.util.function.Supplier;

public class HttpClientBuilder implements HttpClientSpec {

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
  private ProxyInternal proxy;
  private Supplier<AddressResolverGroup<?>> resolver = addressResolverSupplier(Action.noop());
  private ExecController execController;

  public HttpClientBuilder() {
  }

  HttpClientBuilder(DefaultHttpClient builder) {
    this.byteBufAllocator = builder.byteBufAllocator;
    this.poolSize = builder.poolSize;
    this.poolQueueSize = builder.poolQueueSize;
    this.idleTimeout = builder.idleTimeout;
    this.maxContentLength = builder.maxContentLength;
    this.responseMaxChunkSize = builder.responseMaxChunkSize;
    this.readTimeout = builder.readTimeout;
    this.connectTimeout = builder.connectTimeout;
    this.requestInterceptor = builder.requestInterceptor;
    this.responseInterceptor = builder.responseInterceptor;
    this.enableMetricsCollection = builder.enableMetricsCollection;
    this.proxy = builder.proxy;
    this.resolver = () -> builder.resolver;
  }

  @Override
  public HttpClientSpec execController(ExecController execController) {
    this.execController = execController;
    return this;
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
    DefaultProxy.Builder builder = new DefaultProxy.Builder();
    Exceptions.uncheck(() -> proxy.execute(builder));
    this.proxy = builder.build();
    return this;
  }

  @Override
  public HttpClientSpec addressResolver(AddressResolverGroup<?> resolver) {
    this.resolver = () -> resolver;
    return this;
  }

  @Override
  public HttpClientSpec addressResolver(Action<? super DnsNameResolverBuilder> a) {
    this.resolver = addressResolverSupplier(a);
    return this;
  }

  public HttpClient build() {
    return new DefaultHttpClient(
      byteBufAllocator,
      poolSize,
      poolQueueSize,
      idleTimeout,
      maxContentLength,
      responseMaxChunkSize,
      readTimeout,
      connectTimeout,
      requestInterceptor,
      responseInterceptor,
      errorInterceptor,
      enableMetricsCollection,
      resolver.get(),
      proxy
    );
  }

  private Supplier<AddressResolverGroup<?>> addressResolverSupplier(Action<? super DnsNameResolverBuilder> spec) {
    return () -> {
      ExecController execController = this.execController;
      if (execController == null) {
        execController = ExecController.current().orElseThrow(() ->
          new IllegalStateException(
            "Cannot build addressResolver as HttpClient is built on non managed thread, and execController not specified."
            + " Use HttpClientSpec.execController() or useJdkAddressResolver()."
          )
        );
      }

      DnsNameResolverBuilder resolverBuilder = new DnsNameResolverBuilder()
        .eventLoop(execController.getEventLoopGroup().next())
        .channelType(TransportDetector.getDatagramChannelImpl())
        .socketChannelType(TransportDetector.getSocketChannelImpl());

      Exceptions.uncheck(() -> spec.execute(resolverBuilder));
      return new DnsAddressResolverGroup(resolverBuilder);
    };
  }
}
