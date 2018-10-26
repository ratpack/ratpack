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

package ratpack.retrofit;

import io.netty.buffer.Unpooled;
import okhttp3.Request;
import ratpack.http.Status;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.internal.DefaultReceivedResponse;
import ratpack.http.internal.ByteBufBackedTypedData;
import ratpack.http.internal.DefaultMediaType;
import ratpack.retrofit.internal.OkHttpHeadersBackedHeaders;
import ratpack.util.Exceptions;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * Exception throw from Retrofit clients when using simple types instead of {@link Response} and the request is not successful.
 */
public class RatpackRetrofitCallException extends Exception {

  private final Response<?> response;

  /**
   * Create a wrapped Retrofit exception
   * @param call the Retrofit {@link Call} to wrap.
   * @param error the error message
   * @deprecated since 1.6.0
   * @see #RatpackRetrofitCallException(Request, Response)
   */
  @Deprecated
  public RatpackRetrofitCallException(Call<?> call, String error) {
    super(call.request().url().toString() + ": " + error);
    this.response = null;
  }

  /**
   * Create a wrapped Retrofit exception
   * @param request the Retrofit {@link Request} that initiated HTTP request.
   * @param response the underlying Retrofit {@link Response} from the HTTP request.
   * @since 1.6.0
   */
  public RatpackRetrofitCallException(Request request, Response<?> response) {
    super(request.url().toString() + ": " + Exceptions.uncheck(() -> response.errorBody().string()));
    this.response = response;
  }

  /**
   * Get the underlying response that resulted in the exception for this HTTP request.
   * This is useful for mapping the exception based on the response information.
   * @return The response for the HTTP call.
   * @since 1.6.0
   */
  public ReceivedResponse getResponse() {
    return new DefaultReceivedResponse(
      Status.of(response.code(), response.message()),
      new OkHttpHeadersBackedHeaders(response.headers()),
      new ByteBufBackedTypedData(
        Unpooled.wrappedBuffer(Exceptions.uncheck(() -> response.errorBody().bytes())),
        DefaultMediaType.get(response.errorBody().contentType() != null ? response.errorBody().contentType().toString() : null)
      )
    );
  }

  public static RatpackRetrofitCallException cause(Call<?> call, Response<?> response) throws IOException {
    return new RatpackRetrofitCallException(call.request(), response);
  }
}
