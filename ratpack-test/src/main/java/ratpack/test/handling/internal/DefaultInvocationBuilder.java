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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import ratpack.handling.Handler;
import ratpack.http.MutableHeaders;
import ratpack.http.Request;
import ratpack.http.Status;
import ratpack.http.internal.DefaultMediaType;
import ratpack.http.internal.DefaultRequest;
import ratpack.http.internal.DefaultStatus;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.test.handling.Invocation;
import ratpack.test.handling.InvocationBuilder;
import ratpack.test.handling.InvocationTimeoutException;

import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;

/**
 * @see ratpack.test.UnitTest#invoke(ratpack.handling.Handler, ratpack.util.Action)
 */
@SuppressWarnings("UnusedDeclaration")
public class DefaultInvocationBuilder implements InvocationBuilder {

  private final ByteBuf requestBody = unreleasableBuffer(buffer());
  private final MutableHeaders requestHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
  private final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());

  private final Status status = new DefaultStatus();

  private String method = "GET";
  private String uri = "/";

  private int timeout = 5;

  private RegistryBuilder registryBuilder = Registries.registry();

  public DefaultInvocationBuilder() {
  }

  /**
   * Invokes a handler in a controlled way, allowing it to be tested.
   *
   * @param handler The handler to invoke
   * @return A result object indicating what happened
   *
   * @throws ratpack.test.handling.InvocationTimeoutException if the handler takes more than {@link #timeout(int)} seconds to send a response or call {@code next()} on the context
   */
  @Override
  public Invocation invoke(Handler handler) throws InvocationTimeoutException {
    Request request = new DefaultRequest(requestHeaders, method, uri, requestBody);

    Registry registry = registryBuilder.build();

    return new DefaultInvocation(
      request,
      status,
      responseHeaders,
      registry,
      timeout,
      handler
    );
  }

  @Override
  public InvocationBuilder header(String name, String value) {
    requestHeaders.add(name, value);
    return this;
  }

  @Override
  public InvocationBuilder body(byte[] bytes, String contentType) {
    requestHeaders.add(HttpHeaders.Names.CONTENT_TYPE, contentType);
    requestHeaders.add(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
    requestBody.capacity(bytes.length).writeBytes(bytes);
    return this;
  }

  @Override
  public InvocationBuilder body(String text, String contentType) {
    return body(text.getBytes(), DefaultMediaType.utf8(contentType).toString());
  }

  @Override
  public InvocationBuilder responseHeader(String name, String value) {
    responseHeaders.add(name, value);
    return this;
  }

  @Override
  public InvocationBuilder method(String method) {
    if (method == null) {
      throw new IllegalArgumentException("method must not be null");
    }
    this.method = method.toUpperCase();
    return this;
  }

  @Override
  public InvocationBuilder uri(String uri) {
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
  public InvocationBuilder timeout(int timeout) {
    if (timeout < 0) {
      throw new IllegalArgumentException("timeout must be > 0");
    }
    this.timeout = timeout;
    return this;
  }

  @Override
  public InvocationBuilder register(Object object) {
    registryBuilder.add(object);
    return this;
  }
}
