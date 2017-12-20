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
import ratpack.retrofit.internal.ReceivedResponseConverterFactory;
import ratpack.util.Exceptions;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.net.URI;
import java.time.Duration;

/**
 * Builder for providing integration of Retrofit2 with Ratpack's {@link HttpClient}.
 * <p>
 * This class allows for creating declarative type-safe interfaces that represent remote HTTP APIs.
 * Using this adapter allows for defining the interfaces to return {@link ratpack.exec.Promise} types which will be fulfilled by Ratpack's http client.
 *
 * <pre class="java">{@code
 * import ratpack.exec.Promise;
 * import ratpack.retrofit.RatpackRetrofit;
 * import ratpack.test.embed.EmbeddedApp;
 * import retrofit2.http.GET;
 *
 * import static org.junit.Assert.*;
 *
 * public class ExampleRetrofitClient {
 *
 *   public interface HelloService {
 *
 *     {@literal @}GET("hello") Promise<String> hello();
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *
 *     EmbeddedApp api = EmbeddedApp.of(s -> s
 *      .handlers(chain -> chain
 *        .get("hello", ctx -> ctx.render("hello"))
 *      )
 *     );
 *     EmbeddedApp.of(s -> s
 *      .registryOf(r -> r
 *        .add(HelloService.class,
 *          RatpackRetrofit.client(api.getAddress()).build(HelloService.class)
 *        )
 *      )
 *      .handlers(chain -> {
 *         chain.get(ctx -> {
 *
 *           ctx.render(ctx.get(HelloService.class).hello());
 *         });
 *       })
 *     ).test(testHttpClient -> {
 *       assertEquals("hello", testHttpClient.getText());
 *       api.close();
 *     });
 *   }
 * }
 * }</pre>
 *
 * @since 1.4
 */
public abstract class RatpackRetrofit {

  /**
   * Creates a new builder for creating Retrofit clients.
   *
   * @param endpoint the endpoint for client implementations.
   * @return a client builder
   */
  public static Builder client(URI endpoint) {
    return new Builder(endpoint);
  }

  /**
   * Creates a new builder for creating Retrofit clients.
   *
   * @param endpoint the endpoint for client implementations. Converted to {@link URI}.
   * @return a client builder
   */
  public static Builder client(String endpoint) {
    return Exceptions.uncheck(() -> client(new URI(endpoint)));
  }

  public static class Builder {

    private final URI uri;
    private Action<? super Retrofit.Builder> builderAction = Action.noop();
    private Duration connectTimeout;
    private Duration readTimeout;

    private Builder(URI uri) {
      Preconditions.checkNotNull(uri, "Must provide the base uri.");
      this.uri = uri;
    }

    /**
     * Configure the underlying {@link retrofit2.Retrofit.Builder} instance.
     * <p>
     * This is used to customize the behavior of Retrofit.
     *
     * @param builderAction the actions to apply to the Retrofit builder
     * @return this
     * @see retrofit2.Converter.Factory
     * @see retrofit2.CallAdapter.Factory
     */
    public Builder configure(Action<? super Retrofit.Builder> builderAction) {
      this.builderAction = builderAction;
      return this;
    }

    /**
     * Configure the connect timeout for this client.
     *
     * @param connectTimeout connect timeout duration
     * @return this
     */
    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Configure the read timeout for this client.
     *
     * @param readTimeout read timeout duration
     * @return this
     */
    public Builder readTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    /**
     * Creates the underlying {@link Retrofit} instance and configures it to interface with {@link HttpClient} and {@link ratpack.exec.Promise}.
     * <p>
     * The resulting Retrofit instance can be re-used to generate multiple client interfaces which share the same base URI.
     * @return the Retrofit instance to create client interfaces
     */
    public Retrofit retrofit() {
      Retrofit.Builder builder = new Retrofit.Builder()
        .callFactory(RatpackCallFactory.builder().connectTimeout(this.connectTimeout).readTimeout(this.readTimeout).build())
        .addCallAdapterFactory(RatpackCallAdapterFactory.builder().connectTimeout(this.connectTimeout).readTimeout(this.readTimeout).build())
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(ReceivedResponseConverterFactory.INSTANCE);
      builder.baseUrl(uri.toString());
      Exceptions.uncheck(() -> builderAction.execute(builder));
      return builder.build();
    }

    /**
     * Uses this builder to create a Retrofit client implementation.
     * <p>
     * This is the short form of calling {@code client.retrofit().build(service)}.
     *
     * @param service the client interface to generate.
     * @param <T> the type of the client interface.
     * @return a generated instance of the client interface.
     */
    public <T> T build(Class<T> service) {
      return retrofit().create(service);
    }
  }

}
