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

package ratpack.test.handling.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.CharsetUtil;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.exec.ExecController;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.http.MutableHeaders;
import ratpack.http.Request;
import ratpack.http.internal.DefaultRequest;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;
import ratpack.path.PathBinding;
import ratpack.path.internal.DefaultPathBinding;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.server.internal.ServerRegistry;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.RequestFixture;
import ratpack.util.ExceptionUtils;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;

/**
 * @see ratpack.test.handling.RequestFixture#handle(ratpack.handling.Handler, ratpack.func.Action)
 */
@SuppressWarnings("UnusedDeclaration")
public class DefaultRequestFixture implements RequestFixture {

  private final ByteBuf requestBody = unreleasableBuffer(buffer());
  private final MutableHeaders requestHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
  private final NettyHeadersBackedMutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());

  private String method = "GET";
  private String uri = "/";
  private HostAndPort remoteHostAndPort = HostAndPort.fromParts("localhost", 45678);
  private HostAndPort localHostAndPort = HostAndPort.fromParts("localhost", ServerConfig.DEFAULT_PORT);
  private int timeout = 5;

  private RegistryBuilder registryBuilder = Registries.registry();

  private ServerConfig.Builder serverConfigBuilder = ServerConfig.noBaseDir();

  @Override
  public RequestFixture body(byte[] bytes, String contentType) {
    requestHeaders.add(HttpHeaderNames.CONTENT_TYPE, contentType);
    requestHeaders.add(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
    requestBody.capacity(bytes.length).writeBytes(bytes);
    return this;
  }

  @Override
  public RequestFixture body(String text, String contentType) {
    return body(text.getBytes(CharsetUtil.UTF_8), contentType);
  }

  @Override
  public RegistrySpec getRegistry() {
    return registryBuilder;
  }

  @Override
  public HandlingResult handle(Handler handler) throws HandlerTimeoutException {
    final DefaultHandlingResult.ResultsHolder results = new DefaultHandlingResult.ResultsHolder();
    return invoke(handler, getEffectiveRegistry(results), results);
  }

  @Override
  public HandlingResult handleChain(Action<? super Chain> chainAction) throws Exception {
    final DefaultHandlingResult.ResultsHolder results = new DefaultHandlingResult.ResultsHolder();
    Registry registry = getEffectiveRegistry(results);
    ServerConfig serverConfig = registry.get(ServerConfig.class);
    Handler handler = Handlers.chain(serverConfig, registry, chainAction);
    return invoke(handler, registry, results);
  }

  private HandlingResult invoke(Handler handler, Registry registry, DefaultHandlingResult.ResultsHolder results) throws HandlerTimeoutException {
    Request request = new DefaultRequest(requestHeaders, HttpMethod.valueOf(method.toUpperCase()), uri,
      new InetSocketAddress(remoteHostAndPort.getHostText(), remoteHostAndPort.getPort()),
      new InetSocketAddress(localHostAndPort.getHostText(), localHostAndPort.getPort()),
      requestBody);

    try {
      ServerConfig serverConfig = registry.get(ServerConfig.class);
      return new DefaultHandlingResult(
        request,
        results,
        responseHeaders,
        registry,
        timeout,
        handler
      );
    } catch (Exception e) {
      throw ExceptionUtils.uncheck(e);
    } finally {
      registry.get(ExecController.class).close();
    }
  }

  @Override
  public RequestFixture header(String name, String value) {
    requestHeaders.add(name, value);
    return this;
  }

  @Override
  public RequestFixture serverConfig(Action<? super ServerConfig.Builder> action) throws Exception {
    serverConfigBuilder = ServerConfig.noBaseDir();
    action.execute(serverConfigBuilder);
    return this;
  }

  @Override
  public RequestFixture serverConfig(Path baseDir, Action<? super ServerConfig.Builder> action) throws Exception {
    serverConfigBuilder = ServerConfig.baseDir(baseDir);
    action.execute(serverConfigBuilder);
    return this;
  }

  @Override
  public RequestFixture method(String method) {
    if (method == null) {
      throw new IllegalArgumentException("method must not be null");
    }
    this.method = method.toUpperCase();
    return this;
  }

  @Override
  public RequestFixture pathBinding(Map<String, String> pathTokens) {
    return pathBinding("", "", pathTokens);
  }

  @Override
  public RequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens) {
    registryBuilder.add(PathBinding.class, new DefaultPathBinding(boundTo, pastBinding, ImmutableMap.copyOf(pathTokens), Optional.empty()));
    return this;
  }

  @Override
  public RequestFixture registry(Action<? super RegistrySpec> action) throws Exception {
    action.execute(registryBuilder);
    return this;
  }

  @Override
  public RequestFixture responseHeader(String name, String value) {
    responseHeaders.add(name, value);
    return this;
  }

  @Override
  public RequestFixture timeout(int timeoutSeconds) {
    if (timeoutSeconds < 0) {
      throw new IllegalArgumentException("timeout must be > 0");
    }
    this.timeout = timeoutSeconds;
    return this;
  }

  @Override
  public RequestFixture uri(String uri) {
    if (uri == null) {
      throw new NullPointerException("uri cannot be null");
    }
    if (!uri.startsWith("/")) {
      uri = "/" + uri;
    }

    this.uri = uri;
    return this;
  }

  @Override
  public RequestFixture remoteAddress(HostAndPort remote) {
    remoteHostAndPort = remote;
    return this;
  }

  @Override
  public RequestFixture localAddress(HostAndPort local) {
    localHostAndPort = local;
    return this;
  }

  private Registry getEffectiveRegistry(final DefaultHandlingResult.ResultsHolder results) {

    ClientErrorHandler clientErrorHandler = (context, statusCode) -> {
      results.setClientError(statusCode);
      results.getLatch().countDown();
    };

    ServerErrorHandler serverErrorHandler = (context, throwable1) -> {
      results.setThrowable(throwable1);
      results.getLatch().countDown();
    };

    final Registry userRegistry = Registries.registry().
      add(ClientErrorHandler.class, clientErrorHandler).
      add(ServerErrorHandler.class, serverErrorHandler).
      build();
    return ExceptionUtils.uncheck(() -> ServerRegistry.serverRegistry(serverConfigBuilder.build(), new TestServer(), r -> userRegistry.join(registryBuilder.build())));
  }

  // TODO some kind of impl here
  private static class TestServer implements RatpackServer {
    @Override
    public ServerConfig getServerConfig() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getScheme() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getBindPort() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getBindHost() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRunning() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public void stop() throws Exception {
      throw new UnsupportedOperationException();
    }

    @Override
    public RatpackServer reload() throws Exception {
      throw new UnsupportedOperationException();
    }
  }
}
