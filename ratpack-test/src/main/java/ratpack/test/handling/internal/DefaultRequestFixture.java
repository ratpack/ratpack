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
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.exec.ExecController;
import ratpack.exec.Promise;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.http.MutableHeaders;
import ratpack.http.internal.DefaultRequest;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;
import ratpack.impose.Impositions;
import ratpack.path.internal.DefaultPathBinding;
import ratpack.path.internal.PathBindingStorage;
import ratpack.path.internal.RootPathBinding;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.server.internal.RequestBodyReader;
import ratpack.server.internal.ServerRegistry;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.RequestFixture;
import ratpack.test.http.MultipartFileSpec;
import ratpack.test.http.internal.DefaultMultipartForm;
import ratpack.test.http.MultipartFormSpec;
import ratpack.util.Exceptions;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Collections;
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
  private String protocol = "HTTP/1.1";
  private String uri = "/";
  private HostAndPort remoteHostAndPort = HostAndPort.fromParts("localhost", 45678);
  private HostAndPort localHostAndPort = HostAndPort.fromParts("localhost", ServerConfig.DEFAULT_PORT);
  private int timeout = 5;

  private RegistryBuilder registryBuilder = Registry.builder();

  private ServerConfigBuilder serverConfigBuilder = ServerConfig.builder();
  private DefaultPathBinding pathBinding;

  private Optional<DefaultMultipartForm.Builder> formBuilder = Optional.empty();

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
  public MultipartFileSpec file() {
    return findOrCreateForm().file();
  }

  @Override
  public RequestFixture file(String field, String filename, String data) {
    DefaultMultipartForm.Builder form = findOrCreateForm();
    form.file().field(field).name(filename).data(data).add();

    return this;
  }

  @Override
  public MultipartFormSpec form() {
    return findOrCreateForm();
  }

  @Override
  public RequestFixture form(Map<String, String> data) {
    DefaultMultipartForm.Builder form = findOrCreateForm();
    form.fields(data);

    return this;
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
    ServerConfig serverConfig = registry.get(ServerConfig.class);
    writeMultipartFormIfRequired();

    DefaultRequest request = new DefaultRequest(
      Instant.now(), requestHeaders, HttpMethod.valueOf(method.toUpperCase()), HttpVersion.valueOf(protocol), uri,
      new InetSocketAddress(remoteHostAndPort.getHost(), remoteHostAndPort.getPort()),
      new InetSocketAddress(localHostAndPort.getHost(), localHostAndPort.getPort()),
      serverConfig,
      new RequestBodyReader() {

        private boolean unread = true;
        private long maxContentLength = 8096;

        public boolean isUnread() {
          return unread;
        }

        @Override
        public long getContentLength() {
          return requestBody.readableBytes();
        }

        @Override
        public long getMaxContentLength() {
          return maxContentLength;
        }

        @Override
        public void setMaxContentLength(long maxContentLength) {
          this.maxContentLength = maxContentLength;
        }

        @Override
        public Promise<? extends ByteBuf> read(Block onTooLarge) {
          return Promise.sync(() -> {
            unread = false;
            return requestBody;
          })
            .route(r -> r.readableBytes() > maxContentLength, onTooLarge.action());
        }

        @Override
        public TransformablePublisher<? extends ByteBuf> readStream() {
          return Streams.publish(Collections.singleton(requestBody)).wiretap(e ->
            unread = false
          );
        }
      },
      idleTimeout -> {
      },
      null
    );

    if (pathBinding != null) {
      handler = Handlers.chain(
        ctx -> {
          ctx.getExecution().get(PathBindingStorage.TYPE).push(pathBinding);
          ctx.next();
        },
        handler
      );
    }

    try {
      return new DefaultHandlingResult(
        request,
        results,
        responseHeaders,
        registry,
        timeout,
        handler
      );
    } catch (Exception e) {
      throw Exceptions.uncheck(e);
    } finally {
      registry.get(ExecController.class).close();
    }
  }

  @Override
  public RequestFixture header(CharSequence name, String value) {
    requestHeaders.add(name, value);
    return this;
  }

  @Override
  public RequestFixture serverConfig(Action<? super ServerConfigBuilder> action) throws Exception {
    serverConfigBuilder = ServerConfig.builder();
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
    return pathBinding(boundTo, pastBinding, pathTokens, "");
  }

  @Override
  public RequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens, String description) {
    pathBinding = new DefaultPathBinding(boundTo, ImmutableMap.copyOf(pathTokens), new RootPathBinding(boundTo + "/" + pastBinding), description);
    return this;
  }

  @Override
  public RequestFixture registry(Action<? super RegistrySpec> action) throws Exception {
    action.execute(registryBuilder);
    return this;
  }

  @Override
  public RequestFixture responseHeader(CharSequence name, String value) {
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

  @Override
  public RequestFixture protocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  private void writeMultipartFormIfRequired() {
    if(formBuilder.isPresent()) {
      DefaultMultipartForm form = formBuilder.get().build();
      method("POST");
      body(form.getBody(), form.getContentType());
    }
  }

  private DefaultMultipartForm.Builder findOrCreateForm() {
    if(!formBuilder.isPresent()) {
      formBuilder = Optional.of(DefaultMultipartForm.builder());
    }

    return formBuilder.get();
  }

  private Registry getEffectiveRegistry(final DefaultHandlingResult.ResultsHolder results) {

    ClientErrorHandler clientErrorHandler = (context, statusCode) -> {
      results.setClientError(statusCode);
      context.getResponse().status(statusCode);
      results.getLatch().countDown();
    };

    ServerErrorHandler serverErrorHandler = (context, throwable1) -> {
      results.setThrowable(throwable1);
      results.getLatch().countDown();
    };

    final Registry userRegistry = Registry.builder().
      add(ClientErrorHandler.class, clientErrorHandler).
      add(ServerErrorHandler.class, serverErrorHandler).
      build();
    return Exceptions.uncheck(() -> {
      ServerConfig serverConfig = serverConfigBuilder.build();
      DefaultExecController execController = new DefaultExecController(serverConfig.getThreads());
      return ServerRegistry.serverRegistry(new TestServer(), Impositions.none(), execController, serverConfig, r -> userRegistry.join(registryBuilder.build()));
    });
  }

  // TODO some kind of impl here
  private static class TestServer implements RatpackServer {

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

    @Override
    public Optional<Registry> getRegistry() {
      throw new UnsupportedOperationException();
    }
  }
}
