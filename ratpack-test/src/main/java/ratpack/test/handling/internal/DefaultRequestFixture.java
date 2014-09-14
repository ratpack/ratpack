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
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.http.MutableHeaders;
import ratpack.http.MutableStatus;
import ratpack.http.Request;
import ratpack.http.internal.DefaultMutableStatus;
import ratpack.http.internal.DefaultRequest;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.path.PathBinding;
import ratpack.path.internal.DefaultPathBinding;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.RequestFixture;

import java.nio.file.Path;
import java.util.Map;

import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;

/**
 * @see ratpack.test.UnitTest#handle(ratpack.handling.Handler, ratpack.func.Action)
 */
@SuppressWarnings("UnusedDeclaration")
public class DefaultRequestFixture implements RequestFixture {

  private final ByteBuf requestBody = unreleasableBuffer(buffer());
  private final MutableHeaders requestHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
  private final NettyHeadersBackedMutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());

  private final MutableStatus status = new DefaultMutableStatus();

  private String method = "GET";
  private String uri = "/";

  private int timeout = 5;

  private RegistryBuilder registryBuilder = Registries.registry();

  private LaunchConfigBuilder launchConfigBuilder = LaunchConfigBuilder.noBaseDir();

  @Override
  public RequestFixture body(byte[] bytes, String contentType) {
    requestHeaders.add(HttpHeaders.Names.CONTENT_TYPE, contentType);
    requestHeaders.add(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
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

  /**
   * Invokes a handler in a controlled way, allowing it to be tested.
   *
   * @param handler The handler to invoke
   * @return A result object indicating what happened
   * @throws ratpack.test.handling.HandlerTimeoutException if the handler takes more than {@link #timeout(int)} seconds to send a response or call {@code next()} on the context
   */
  @Override
  public HandlingResult handle(Handler handler) throws HandlerTimeoutException {
    Request request = new DefaultRequest(requestHeaders, method, uri, requestBody);

    Registry registry = registryBuilder.build();

    return new DefaultHandlingResult(
      request,
      status,
      responseHeaders,
      registry,
      timeout,
      launchConfigBuilder,
      handler
    );
  }

  @Override
  public HandlingResult handle(final Action<? super Chain> chainAction) throws HandlerTimeoutException {
    return handle(new Handler() {
      @Override
      public void handle(Context context) throws Exception {
        Handlers.chain(context.getLaunchConfig(), chainAction).handle(context);
      }
    });
  }

  @Override
  public RequestFixture header(String name, String value) {
    requestHeaders.add(name, value);
    return this;
  }

  @Override
  public RequestFixture launchConfig(Action<? super LaunchConfigBuilder> action) throws Exception {
    launchConfigBuilder = LaunchConfigBuilder.noBaseDir();
    action.execute(launchConfigBuilder);
    return this;
  }

  @Override
  public RequestFixture launchConfig(Path baseDir, Action<? super LaunchConfigBuilder> action) throws Exception {
    launchConfigBuilder = LaunchConfigBuilder.baseDir(baseDir);
    action.execute(launchConfigBuilder);
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
    registryBuilder.add(PathBinding.class, new DefaultPathBinding(boundTo, pastBinding, ImmutableMap.copyOf(pathTokens), null));
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

}
