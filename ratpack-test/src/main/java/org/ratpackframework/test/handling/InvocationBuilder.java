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

package org.ratpackframework.test.handling;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.http.MutableHeaders;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Status;
import org.ratpackframework.http.internal.DefaultRequest;
import org.ratpackframework.http.internal.DefaultStatus;
import org.ratpackframework.http.internal.NettyHeadersBackedMutableHeaders;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.RootRegistry;
import org.ratpackframework.test.handling.internal.DefaultInvocation;
import org.ratpackframework.util.Action;

import static io.netty.buffer.Unpooled.*;

/**
 * @see #invoke(org.ratpackframework.handling.Handler, org.ratpackframework.util.Action)
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

  private ImmutableList.Builder<Object> registryContents = ImmutableList.builder();

  protected InvocationBuilder() {
  }

  /**
   * Invokes a handler in a controlled way, allowing it to be tested.
   *
   * @param handler The handler to invoke
   * @return A result object indicating what happened
   * @throws InvocationTimeoutException if the handler takes more than {@link #getTimeout()} seconds to send a response or call {@code next()} on the context
   */
  public Invocation invoke(Handler handler) throws InvocationTimeoutException {
    Request request = new DefaultRequest(requestHeaders, method, uri, requestBody);

    Registry<Object> registry = new RootRegistry<>(registryContents.build());

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
   * @throws InvocationTimeoutException if the handler takes more than {@link #getTimeout()} seconds to send a response or call {@code next()} on the context
   */
  public static Invocation invoke(Handler handler, Action<? super InvocationBuilder> action) throws InvocationTimeoutException {
    InvocationBuilder builder = new InvocationBuilder();

    action.execute(builder);

    return builder.invoke(handler);
  }

  public ByteBuf getRequestBody() {
    return requestBody;
  }

  public MutableHeaders getRequestHeaders() {
    return requestHeaders;
  }

  public ByteBuf getResponseBody() {
    return responseBody;
  }

  public MutableHeaders getResponseHeaders() {
    return responseHeaders;
  }

  public Status getStatus() {
    return status;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    if (method == null) {
      throw new IllegalArgumentException("method must not be null");
    }
    this.method = method.toUpperCase();
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    if (uri == null) {
      throw new NullPointerException("uri cannot be null");
    }
    if (!uri.startsWith("/")) {
      uri = "/" + uri;
    }

    this.uri = uri;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    if (timeout < 0) {
      throw new IllegalArgumentException("timeout must be > 0");
    }
    this.timeout = timeout;
  }

  public void register(Object object) {
    registryContents.add(object);
  }
}
