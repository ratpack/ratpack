/*
 * Copyright 2014 the original author or authors.
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
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.*;

import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

public class DefaultHttpClient implements HttpClient {
  private final ByteBufAllocator byteBufAllocator;
  private final int maxContentLengthBytes;
  private final Supplier<Iterable<? extends HttpClientRequestInterceptor>> requestInterceptor;
  private final Supplier<Iterable<? extends HttpClientResponseInterceptor>> responseInterceptor;
  private final Supplier<Optional<RequestSpecConfigurer>> requestConfigurer;

  public DefaultHttpClient(final ByteBufAllocator byteBufAllocator,
                           final int maxContentLengthBytes,
                           final Supplier<Iterable<? extends HttpClientRequestInterceptor>> requestInterceptor,
                           final Supplier<Iterable<? extends HttpClientResponseInterceptor>> responseInterceptor,
                           final Supplier<Optional<RequestSpecConfigurer>> requestConfigurer) {
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLengthBytes = maxContentLengthBytes;
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.requestConfigurer = requestConfigurer;
  }

  public DefaultHttpClient(final ByteBufAllocator byteBufAllocator,
                           final int maxContentLengthBytes) {
    this(byteBufAllocator,
          maxContentLengthBytes,
          () -> Execution.current().getAll(HttpClientRequestInterceptor.class),
          () -> Execution.current().getAll(HttpClientResponseInterceptor.class),
          () -> Execution.current().maybeGet(RequestSpecConfigurer.class));
  }

  @Override
  public Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer);
  }

  private static class Post implements Action<RequestSpec> {
    @Override
    public void execute(RequestSpec requestSpec) throws Exception {
      requestSpec.method("POST");
    }
  }

  @Override
  public Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action) {
    return request(uri, new Post().append(action));
  }

  @Override
  public Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    Execution execution = Execution.current();
    RequestSpecConfigurer configurer = this.requestConfigurer
      .get()
      .orElse(requestSpec -> Action.noop());

    return Promise.async(f -> new ContentAggregatingRequestAction(
      requestConfigurer.prepend(configurer::configure),
      uri,
      execution,
      byteBufAllocator,
      maxContentLengthBytes,
      0,
      requestInterceptor.get(),
      responseInterceptor.get()).connect(f));
  }

  @Override
  public Promise<StreamedResponse> requestStream(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    return Promise.async(f -> new ContentStreamingRequestAction(requestConfigurer, uri, Execution.current(), byteBufAllocator, 0).connect(f));
  }

}
