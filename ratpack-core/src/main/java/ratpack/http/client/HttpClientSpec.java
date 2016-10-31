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

package ratpack.http.client;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.func.Action;
import ratpack.server.ServerConfig;

import java.time.Duration;

/**
 * An additive specification of a HTTP client.
 * <p>
 * See {@link HttpClient#of(Action)}.
 *
 * @since 1.4
 */
public interface HttpClientSpec {

  /**
   * The buffer allocator to use.
   * <p>
   * Defaults to {@link PooledByteBufAllocator#DEFAULT}.
   *
   * @param byteBufAllocator the buffer allocator
   * @return {@code this}
   */
  HttpClientSpec byteBufAllocator(ByteBufAllocator byteBufAllocator);

  /**
   * The maximum number of connections to maintain to a given protocol/host/port.
   * <p>
   * Defaults to 0.
   * <p>
   * Setting this number to > 0 enables connection pooling (a.k.a. HTTP Keep Alive).
   * The given value dictates the number of connections to a given target, not the overall size.
   * Calling {@link HttpClient#close()} will close all current connections.
   *
   * @param poolSize the connection pool size
   * @return {@code this}
   */
  HttpClientSpec poolSize(int poolSize);

  /**
   * The maximum size to allow for responses.
   * <p>
   * Defaults to {@link ServerConfig#DEFAULT_MAX_CONTENT_LENGTH}.
   *
   * @param maxContentLength the maximum response content length
   * @return {@code this}
   */
  HttpClientSpec maxContentLength(int maxContentLength);

  /**
   * The read timeout value for responses.
   * <p>
   * Defaults to 30 seconds.
   *
   * @param readTimeout the read timeout value for responses
   * @return {@code this}
   */
  HttpClientSpec readTimeout(Duration readTimeout);

  /**
   * The connect timeout value for requests.
   * <p>
   * Defaults to 30 seconds.
   *
   * @param connectTimeout the connect timeout value for requests
   * @return {@code this}
   * @since 1.5
   */
  HttpClientSpec connectTimeout(Duration connectTimeout);

    /**
     * The max size of the chunks to emit when reading a response as a stream.
     * <p>
     * Defaults to 8192.
     * <p>
     * Increasing this value can increase throughput at the expense of memory use.
     *
     * @param numBytes the max number of bytes to emit
     * @return {@code this}
     * @since 1.5
     */
  HttpClientSpec responseMaxChunkSize(int numBytes);
  
  /**
   * Add an interceptor for all requests handled by this client.
   * <p>
   * This function is additive.
   *
   * @param interceptor the action to perform on the spec before transmitting.
   * @return {@code} this
   * @since 1.6
   */
  HttpClientSpec requestIntercept(Action<? super RequestSpec> interceptor);

  /**
   * Add an interceptor for all responses returned by this client.
   * <p>
   * This function is additive.
   *
   * @param interceptor the action to perform on the response before returning.
   * @return {@code} this
   * @since 1.6
   */
  HttpClientSpec responseIntercept(Action<? super ReceivedResponse> interceptor);

}
