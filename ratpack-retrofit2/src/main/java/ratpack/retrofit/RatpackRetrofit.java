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
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.retrofit.internal.RatpackCallAdapterFactory;
import ratpack.retrofit.internal.RatpackCallFactory;
import ratpack.util.Exceptions;
import retrofit2.Retrofit;

import java.net.URI;

/**
 * Builder for providing integration of Retrofit2 with Ratpack's {@link HttpClient}.
 * <p>
 * This class allows for creating declarative type-safe interfaces that represent remote HTTP APIs.
 * Using this adapter allows for defining the interfaces to return {@link ratpack.exec.Promise} types which will be fulfilled by Ratpack's http client.
 *
 * <pre class="java">{@code
 * import retrofit2.http.BODY;
 * import retrofit2.http.GET;
 * import retrofit2.http.POST;
 *
 * public class ExampleRetrofitClient {
 *
 *   public interface HelloService {
 *
 *     {@literal @}GET("/hello") Promise<String> hello();
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *
 *     EmbeddedApp.fromHandlers(chain -> {
 *       chain
 *         .get(ctx -> {
 *           PublicAddress address = ctx.get(PublicAddress.class);
 *           HttpClient httpClient = ctx.get(HttpClient.class);
 *           HelloService service = RatpackRetrofit.builder(httpClient)
 *             .uri(address.get())
 *             .build()
 *             .create(HelloService.class);
 *
 *           ctx.render service.hello();
 *         })
 *         .get("hello", ctx -> {
 *           ctx.render("hello")
 *         })
 *       }
 *     ).test(testHttpClient -> {
 *       assertEquals("hello", testHttpClient.getText());
 *     }
 *   }
 * }
 * }</pre>
 *
 */
public class RatpackRetrofit {

  public static Builder builder(HttpClient httpClient) {
    return new Builder(httpClient);
  }

  static class Builder {

    private HttpClient httpClient;
    private Action<? super Retrofit.Builder> builderAction = Action.noop();
    private URI uri;

    public Builder(HttpClient httpClient) {
      Preconditions.checkNotNull(httpClient);
      this.httpClient = httpClient;
    }

    public Builder configure(Action<? super Retrofit.Builder> builderAction) {
      this.builderAction = builderAction;
      return this;
    }

    public Builder client(HttpClient client) {
      Preconditions.checkNotNull(client);
      this.httpClient = client;
      return this;
    }

    public Builder uri(URI uri) {
      this.uri = uri;
      return this;
    }

    public Builder uri(String uri) {
      return Exceptions.uncheck(() -> uri(new URI(uri)));
    }

    public Retrofit build() throws Exception {
      Retrofit.Builder builder = new Retrofit.Builder()
        .callFactory(new RatpackCallFactory(httpClient))
        .addCallAdapterFactory(RatpackCallAdapterFactory.INSTANCE);
      builder.baseUrl(uri.toString());
      builderAction.execute(builder);
      return builder.build();
    }
  }

}
