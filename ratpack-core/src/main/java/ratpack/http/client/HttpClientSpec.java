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
import io.netty.buffer.UnpooledByteBufAllocator;
import ratpack.exec.ExecController;
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
   * Sets the execution controller to use when making requests.
   * <p>
   * Defaults to {@link ExecController#current()}.
   * That is, if an execution controller is bound to the current thread, it is the default.
   * <p>
   * An execution controller MUST be specified for a client.
   * However, as the default value is the current thread-bound execution controller,
   * this rarely needs to be explicitly specified.
   *
   * @param execController the execution controller
   * @return {@code this}
   */
  HttpClientSpec execController(ExecController execController);

  /**
   * The buffer allocator to use.
   * <p>
   * Defaults to {@link UnpooledByteBufAllocator#DEFAULT}.
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

}
