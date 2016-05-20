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
import ratpack.exec.Execution;
import ratpack.handling.Context;
import ratpack.http.client.HttpClient;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RatpackCallFactory implements okhttp3.Call.Factory {

  public static final RatpackCallFactory INSTANCE = new RatpackCallFactory();

  public RatpackCallFactory() {
  }

  @Override
  public Call newCall(Request request) {
    return new RatpackCall(request);
  }

  private static class RatpackCall implements Call {

    private final Request request;

    public RatpackCall(Request request) {
      this.request = request;
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
      HttpClient client = Execution.current().get(Context.class).get(HttpClient.class);
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
      }).onError(t -> {
        if (t instanceof IOException) {
          responseCallback.onFailure(thisCall, (IOException) t);
        } else {
          responseCallback.onFailure(thisCall, new IOException(t));
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
              return new Buffer().write(r.getBody().getBytes());
            }
          });
        for (Map.Entry<String, Collection<String>> entry : r.getHeaders().asMultiValueMap().asMultimap().asMap().entrySet()) {
          for (String value : entry.getValue()) {
            builder.addHeader(entry.getKey(), value);
          }
        }
        builder.protocol(Protocol.HTTP_1_1);
        responseCallback.onResponse(thisCall, builder.build());
      });
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
