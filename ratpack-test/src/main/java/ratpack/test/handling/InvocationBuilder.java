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

package ratpack.test.handling;

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
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.test.handling.internal.DefaultInvocation;
import ratpack.util.Action;

import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;

/**
 * @see #invoke(ratpack.handling.Handler, ratpack.util.Action)
 */
@SuppressWarnings("UnusedDeclaration")
public class InvocationBuilder {

  private final ByteBuf requestBody = unreleasableBuffer(buffer());
  private final MutableHeaders requestHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());

  private final ByteBuf responseBody = unreleasableBuffer(buffer());
  private final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());

  private final Status status = new DefaultStatus();

  private String method = "GET";
  private String uri = "/";

  private int timeout = 5;

  private RegistryBuilder registryBuilder = RegistryBuilder.builder();

  public InvocationBuilder() {
  }

  /**
   * Invokes a handler in a controlled way, allowing it to be tested.
   *
   * @param handler The handler to invoke
   * @return A result object indicating what happened
   *
   * @throws InvocationTimeoutException if the handler takes more than {@link #timeout(int)} seconds to send a response or call {@code next()} on the context
   */
  public Invocation invoke(Handler handler) throws InvocationTimeoutException {
    Request request = new DefaultRequest(requestHeaders, method, uri, requestBody);

    Registry registry = registryBuilder.build();

    return new DefaultInvocation(
      request,
      status,
      responseHeaders,
      responseBody.copy(),
      registry,
      timeout,
      handler
    );
  }

  /**
   * Invokes a handler in a controlled way, allowing it to be tested.
   *
   * @param handler The handler to invoke
   * @param action The configuration of the context for the handler
   * @return A result object indicating what happened
   *
   * @throws InvocationTimeoutException if the handler takes more than {@link #timeout(int)} seconds to send a response or call {@code next()} on the context
   */
  public static Invocation invoke(Handler handler, Action<? super InvocationBuilder> action) throws InvocationTimeoutException {
    InvocationBuilder builder = new InvocationBuilder();

    action.execute(builder);

    return builder.invoke(handler);
  }

  public InvocationBuilder header(String name, String value) {
    requestHeaders.add(name, value);
    return this;
  }

  public InvocationBuilder body(byte[] bytes, String contentType) {
    requestHeaders.add(HttpHeaders.Names.CONTENT_TYPE, contentType);
    requestHeaders.add(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
    requestBody.capacity(bytes.length).writeBytes(bytes);
    return this;
  }

  public InvocationBuilder body(String text, String contentType) {
    return body(text.getBytes(), DefaultMediaType.utf8(contentType).toString());
  }

  public InvocationBuilder responseHeader(String name, String value) {
    responseHeaders.add(name, value);
    return this;
  }

  public InvocationBuilder responseBody(byte[] bytes, String contentType) {
    responseHeaders.add(HttpHeaders.Names.CONTENT_TYPE, contentType);
    responseBody.capacity(bytes.length).writeBytes(bytes);
    return this;
  }

  public InvocationBuilder responseBody(String text, String contentType) {
    return responseBody(text.getBytes(), DefaultMediaType.utf8(contentType).toString());
  }

  public InvocationBuilder method(String method) {
    if (method == null) {
      throw new IllegalArgumentException("method must not be null");
    }
    this.method = method.toUpperCase();
    return this;
  }

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

  public InvocationBuilder timeout(int timeout) {
    if (timeout < 0) {
      throw new IllegalArgumentException("timeout must be > 0");
    }
    this.timeout = timeout;
    return this;
  }

  public InvocationBuilder register(Object object) {
    registryBuilder.add(object);
    return this;
  }
}
