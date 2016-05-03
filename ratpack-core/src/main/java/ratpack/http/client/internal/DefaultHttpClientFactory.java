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
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClientFactory;
import ratpack.http.client.HttpClientRequestInterceptor;
import ratpack.http.client.HttpClientResponseInterceptor;

import java.util.Optional;

/**
 * {@link HttpClientFactory} implementation that creates instances of {@link DefaultHttpClient}.
 */
public class DefaultHttpClientFactory implements HttpClientFactory {
  private final ByteBufAllocator byteBufAllocator;
  private final int maxContentLengthBytes;

  /**
   * Constructor.
   *
   * @param byteBufAllocator the byte buf allocator
   * @param maxContentLengthBytes the max content length
   */
  public DefaultHttpClientFactory(final ByteBufAllocator byteBufAllocator, final int
    maxContentLengthBytes) {
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLengthBytes = maxContentLengthBytes;
  }

  @Override
  public HttpClient create(final HttpClientRequestInterceptor requestInterceptor,
                           final HttpClientResponseInterceptor responseInterceptor) {
    return new DefaultHttpClient(byteBufAllocator,
      maxContentLengthBytes,
      () -> Optional.of(requestInterceptor),
      () -> Optional.of(responseInterceptor));
  }
}
