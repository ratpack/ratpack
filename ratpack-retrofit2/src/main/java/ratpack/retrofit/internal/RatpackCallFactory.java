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
import ratpack.http.client.HttpClient;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RatpackCallFactory implements okhttp3.Call.Factory {

  private final HttpClient client;

  public RatpackCallFactory(HttpClient client) {
    this.client = client;
  }

  @Override
  public Call newCall(Request request) {
    return new RatpackCall(client, request);
  }

  private static class RatpackCall implements Call {

    private final HttpClient client;
    private final Request request;

    public RatpackCall(HttpClient client, Request request) {
      this.client = client;
      this.request = request;
    }

    @Override
    public Request request() {
      return request;
    }

    @Override
    public Response execute() throws IOException {
      throw new UnsupportedOperationException("Not implemented for Ratpack");
    }

    @Override
    public void enqueue(okhttp3.Callback responseCallback) {
      final Call $this = this;
      client.request(request.url().uri(), spec -> {
        spec.method(request.method());
        spec.headers(h -> {
          for(Map.Entry<String, List<String>> entry : request.headers().toMultimap().entrySet()) {
            h.set(entry.getKey(), entry.getValue());
          }
        });
        if (request.body() != null) {
          spec.body(b -> {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            b.stream(buffer::writeTo);
            b.type(request.body().contentType().toString());
          });
        }
      }).then(r -> {
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
              Buffer buffer = new Buffer();
                buffer.write(r.getBody().getBytes());
              return buffer;
            }
          });
        for (Map.Entry<String, Collection<String>> entry : r.getHeaders().asMultiValueMap().asMultimap().asMap().entrySet()) {
          for (String value : entry.getValue()) {
            builder.addHeader(entry.getKey(), value);
          }
        }
        builder.protocol(Protocol.HTTP_1_1);
        responseCallback.onResponse($this, builder.build());
      });
    }

    @Override
    public void cancel() {

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
