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

package ratpack.retrofit.internal;

import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.http.MutableHeaders;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RatpackCallFactory implements okhttp3.Call.Factory {

  private Duration connectTimeout;
  private Duration readTimeout;

  private RatpackCallFactory(Duration connectTimeout, Duration readTimeout) {
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  public static class Builder {

    private Duration connectTimeout;
    private Duration readTimeout;

    private Builder() {
    }

    /**
     * Configure the connect timeout for this client.
     *
     * @param connectTimeout connect timeout duration
     * @return this
     */
    public RatpackCallFactory.Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Configure the read timeout for this client.
     *
     * @param readTimeout read timeout duration
     * @return this
     */
    public RatpackCallFactory.Builder readTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    /**
     * Build the instance.
     *
     * @return The instance.
     */
    public RatpackCallFactory build() {
      return new RatpackCallFactory(this.connectTimeout, this.readTimeout);
    }
  }

  /**
   * Creates a new builder.
   *
   * @return The builder.
   */
  public static RatpackCallFactory.Builder builder() {
    return new RatpackCallFactory.Builder();
  }

  @Override
  public Call newCall(Request request) {
    return new RatpackCall(request, connectTimeout, readTimeout);
  }

  static class RatpackCall implements Call {

    private final Request request;
    private Duration connectTimeout;
    private Duration readTimeout;

    public RatpackCall(Request request, Duration connectTimeout, Duration readTimeout) {
      this.request = request;
      this.connectTimeout = connectTimeout;
      this.readTimeout = readTimeout;
    }

    @Override
    public Request request() {
      return request;
    }

    @Override
    public Response execute() throws IOException {
      throw new UnsupportedOperationException("execute() not implemented for Ratpack");
    }

    @Override
    public void enqueue(okhttp3.Callback responseCallback) {
      final Call thisCall = this;
      promise().onError(t -> {
        if (t instanceof IOException) {
          responseCallback.onFailure(thisCall, (IOException) t);
        } else {
          responseCallback.onFailure(thisCall, new IOException(t));
        }
      }).then(r ->
        responseCallback.onResponse(thisCall, mapReceivedResponse(r))
      );
    }

    Promise<ReceivedResponse> promise() {
      HttpClient client = Execution.current()
        .get(Context.class)
        .get(HttpClient.class);
      return client.request(request.url().uri(), this::configureRequest);
    }

    private void configureRequest(RequestSpec spec) throws Exception {
      spec.method(request.method());
      spec.headers(this::configureHeaders);
      if (request.body() != null) {
        spec.body(this::configureBody);
      }
      if (connectTimeout != null) {
        spec.connectTimeout(connectTimeout);
      }
      if (readTimeout != null) {
        spec.readTimeout(readTimeout);
      }
    }

    private void configureHeaders(MutableHeaders h) {
      for (Map.Entry<String, List<String>> entry : request.headers().toMultimap().entrySet()) {
        h.set(entry.getKey(), entry.getValue());
      }
    }

    private void configureBody(RequestSpec.Body b) throws Exception {
      Buffer buffer = new Buffer();
      request.body().writeTo(buffer);
      b.stream(buffer::writeTo);
      b.type(request.body().contentType().toString());
    }

    private Response mapReceivedResponse(ReceivedResponse r) {
      Response.Builder builder = new Response.Builder();
      builder.request(request)
        .code(r.getStatusCode())
        .body(new ResponseBody() {
          @Override
          public MediaType contentType() {
            return MediaType.parse(r.getBody().getContentType().toString());
          }

          @Override
          public long contentLength() {
            return r.getBody().getBytes().length;
          }

          @Override
          public BufferedSource source() {
            return new Buffer().write(r.getBody().getBytes());
          }
        });
      for (Map.Entry<String, Collection<String>> entry : r.getHeaders().asMultiValueMap().asMultimap().asMap().entrySet()) {
        for (String value : entry.getValue()) {
          builder.addHeader(entry.getKey(), value);
        }
      }
      builder.protocol(Protocol.HTTP_1_1);
      return builder.build();
    }

    @Override
    public void cancel() {
      throw new UnsupportedOperationException("cancel() not implemented for Ratpack");
    }

    @Override
    public boolean isExecuted() {
      return false;
    }

    @Override
    public boolean isCanceled() {
      return false;
    }
  }

}
