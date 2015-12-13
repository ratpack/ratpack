/*
 * Copyright 2015 the original author or authors.
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

import com.google.common.base.Preconditions;
import com.squareup.okhttp.*;
import okio.Buffer;
import okio.BufferedSource;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.retrofit.internal.RatpackCallAdapterFactory;
import retrofit.Retrofit;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RatpackRetrofit {

  static Builder builder(HttpClient httpClient) {
    return new Builder(httpClient);
  }

  static class Builder {

    private OkHttpClient client = new OkHttpClient();
    private HttpClient httpClient;
    private Action<? super Retrofit.Builder> builderAction = Action.noop();

    public Builder(HttpClient httpClient) {
      Preconditions.checkNotNull(httpClient);
      this.httpClient = httpClient;
    }

    Builder client(OkHttpClient client) {
      this.client = client;
      return this;
    }

    Builder builder(Action<? super Retrofit.Builder> builderAction) {
      this.builderAction = builderAction;
      return this;
    }

    Builder uri(URI uri) {
      builderAction = builderAction.append(b -> b.baseUrl(uri.toString()));
      return this;
    }

    Retrofit build() throws Exception {
      client.networkInterceptors().add(new RatpackInterceptor(httpClient));
      Retrofit.Builder builder = new Retrofit.Builder()
        .addCallAdapterFactory(RatpackCallAdapterFactory.INSTANCE)
        .client(client);
      builderAction.execute(builder);
      return builder.build();
    }
  }

  static class RatpackInterceptor implements Interceptor {

    private final HttpClient httpClient;

    public RatpackInterceptor(HttpClient httpClient) {
      this.httpClient = httpClient;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request = chain.request();
      httpClient.request(request.uri(), spec -> {
        spec.method(request.method());
        spec.headers(h -> {
          for(Map.Entry<String, List<String>> entry : request.headers().toMultimap().entrySet()) {
            h.set(entry.getKey(), entry.getValue());
          }
        });
        spec.body(b -> {
          Buffer buffer = new Buffer();
          request.body().writeTo(buffer);
          b.stream(buffer::writeTo);
          b.type(request.body().contentType().toString());
        });
      }).map(r -> {
        Response.Builder builder = new Response.Builder();
        builder.request(request)
          .code(r.getStatusCode())
          .body(new ResponseBody() {
            @Override
            public MediaType contentType() {
              return MediaType.parse(r.getBody().getContentType().toString());
            }

            @Override
            public long contentLength() throws IOException {
              return r.getBody().getBytes().length;
            }

            @Override
            public BufferedSource source() throws IOException {
              Buffer buffer = new Buffer();
              buffer.readFrom(r.getBody().getInputStream());
              return buffer;
            }
          });
        for (Map.Entry<String, Collection<String>> entry : r.getHeaders().asMultiValueMap().asMultimap().asMap().entrySet()) {
          for (String value : entry.getValue()) {
            builder.addHeader(entry.getKey(), value);
          }
        }
        return builder.build();
      });
      return null;
    }
  }
}
