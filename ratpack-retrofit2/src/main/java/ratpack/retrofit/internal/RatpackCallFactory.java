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
import okhttp3.Call;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;
import ratpack.exec.Promise;
import ratpack.func.Factory;
import ratpack.http.MutableHeaders;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static ratpack.util.Exceptions.uncheck;

public class RatpackCallFactory implements okhttp3.Call.Factory {

  private final Factory<? extends HttpClient> factory;

  private RatpackCallFactory(Factory<? extends HttpClient> factory) {
    this.factory = factory;
  }

  public static RatpackCallFactory with(Factory<? extends HttpClient> factory) {
    return new RatpackCallFactory(factory);
  }

  @Override
  public Call newCall(Request request) {
    return new RatpackCall(request, factory);
  }

  static class RatpackCall implements Call {

    private final Request request;
    private final ratpack.func.Factory<? extends HttpClient> clientFactory;

    public RatpackCall(Request request, ratpack.func.Factory<? extends HttpClient> clientFactory) {
      this.request = request;
      this.clientFactory = clientFactory;
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
      HttpClient client = uncheck(clientFactory);
      return client.request(request.url().uri(), this::configureRequest);
    }

    private void configureRequest(RequestSpec spec) throws Exception {
      spec.method(request.method());
      spec.headers(this::configureHeaders);
      if (request.body() != null) {
        spec.body(this::configureBody);
      }
    }

    private void configureHeaders(MutableHeaders h) {
      for(Map.Entry<String, List<String>> entry : request.headers().toMultimap().entrySet()) {
        h.set(entry.getKey(), entry.getValue());
      }
    }

    private void configureBody(RequestSpec.Body b) throws Exception {
      Buffer buffer = new Buffer();
      request.body().writeTo(buffer);
      b.stream(buffer::writeTo);
      if(request.body().contentType() != null) {
        b.type(request.body().contentType().toString());
      }
    }

    private Response mapReceivedResponse(ReceivedResponse r) {
      Response.Builder builder = new Response.Builder();
      builder.request(request)
        .code(r.getStatusCode())
        .message(r.getStatus().getMessage())
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
      for (Map.Entry<String, String> entry : r.getHeaders().asMultiValueMap().entrySet()) {
        builder.addHeader(entry.getKey(), entry.getKey());
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

    //CHECKSTYLE:OFF
    @Override
    public RatpackCall clone() {
      //CHECKSTYLE:ON
      return new RatpackCall(request, clientFactory);
    }
  }

}
