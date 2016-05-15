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

import io.netty.buffer.ByteBufAllocator;
import ratpack.http.client.*;

import java.util.Collections;
import java.util.Optional;

/**
 * {@link HttpClientBuilder} implementation that creates instances of {@link DefaultHttpClient}.
 */
public class DefaultHttpClientBuilder implements HttpClientBuilder {
  private final ByteBufAllocator byteBufAllocator;
  private final int maxContentLengthBytes;

  /**
   * Constructor.
   *
   * @param byteBufAllocator the byte buf allocator
   * @param maxContentLengthBytes the max content length
   */
  public DefaultHttpClientBuilder(final ByteBufAllocator byteBufAllocator, final int
    maxContentLengthBytes) {
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLengthBytes = maxContentLengthBytes;
  }

  private Iterable<? extends HttpClientRequestInterceptor> requestInterceptor = Collections.emptyList();
  private Iterable<? extends HttpClientResponseInterceptor> responseInterceptor = Collections.emptyList();
  private Optional<RequestSpecConfigurer> requestConfigurer = Optional.empty();

  @Override
  public HttpClientBuilder requestInterceptor(final HttpClientRequestInterceptor
                                                  requestInterceptor) {
    this.requestInterceptor = Collections.singleton(requestInterceptor);
    return this;
  }

  @Override
  public HttpClientBuilder responseInterceptor(final HttpClientResponseInterceptor responseInterceptor) {
    this.responseInterceptor = Collections.singleton(responseInterceptor);
    return this;
  }

  @Override
  public HttpClientBuilder requestSpecConfigurer(final RequestSpecConfigurer
                                                     requestSpecConfigurer) {
    this.requestConfigurer = Optional.of(requestSpecConfigurer);
    return this;
  }

  @Override
  public HttpClient build() {
    return new DefaultHttpClient(byteBufAllocator,
      maxContentLengthBytes,
      () -> requestInterceptor,
      () -> responseInterceptor,
      () -> requestConfigurer);
  }
}
