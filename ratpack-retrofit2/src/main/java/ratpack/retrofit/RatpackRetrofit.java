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
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.handling.Context;
import ratpack.http.client.HttpClient;
import ratpack.retrofit.internal.RatpackCallAdapterFactory;
import ratpack.retrofit.internal.RatpackCallFactory;
import ratpack.retrofit.internal.ReceivedResponseConverterFactory;
import ratpack.util.Exceptions;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.net.URI;

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
    private Factory<? extends HttpClient> clientFactory = () -> {
      Execution exec = Execution.current();
      return exec
        .maybeGet(HttpClient.class)
        .orElseGet(() ->
          exec.get(Context.class).get(HttpClient.class)
        );
    };

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
     * @return {@code this}
     * @see retrofit2.Converter.Factory
     * @see retrofit2.CallAdapter.Factory
     */
    public Builder configure(Action<? super Retrofit.Builder> builderAction) {
      this.builderAction = builderAction;
      return this;
    }

    /**
     * Configures a {@link Factory} that supplies the underlying {@link HttpClient} to back
     * client interfaces generated from the return of {@link #retrofit()}
     * <p>
     * By default, the following locations are searched in order, with the first {@link HttpClient} found used to back
     * the client interfaces.
     * <ul>
     *   <li>Current {@link Execution}</li>
     *   <li>{@link Context} in the current {@link Execution}</li>
     * </ul>
     * <p>
     * If no {@link HttpClient} is found, a {@link ratpack.registry.NotInRegistryException} is thrown.
     *
     * @param clientFactory a factory that generates a HttpClient to be used
     * @return {@code this}
     * @since 1.6
     */
    public Builder httpClient(Factory<? extends HttpClient> clientFactory) {
      this.clientFactory = clientFactory;
      return this;
    }


    /**
     * Creates the underlying {@link Retrofit} instance and configures it to interface with {@link HttpClient} and {@link ratpack.exec.Promise}.
     * <p>
     * The resulting Retrofit instance can be re-used to generate multiple client interfaces which share the same base URI.
     *
     * @return the Retrofit instance to create client interfaces
     */
    public Retrofit retrofit() {
      Retrofit.Builder builder = new Retrofit.Builder()
        .callFactory(RatpackCallFactory.with(clientFactory))
        .addCallAdapterFactory(RatpackCallAdapterFactory.with(clientFactory))
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(ReceivedResponseConverterFactory.INSTANCE);
      builder.baseUrl(uri.toString());
      Exceptions.uncheck(() -> builderAction.execute(builder));
      return builder.build();
    }

    /**
     * Uses this builder to create a Retrofit client implementation.
     * <p>
     * This is the short form of calling {@code builder.retrofit().create(service)}.
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
