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

package ratpack.http.client;

import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.internal.DefaultHttpClient;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;
import ratpack.util.Exceptions;

import java.net.URI;
import java.time.Duration;

/**
 * An asynchronous HTTP client.
 * <p>
 * A default instance is always available in an application through the server registry.
 * The default instance does not use connection pooling and has conservative defaults.
 * Alternative instances can be created via {@link #of(Action)}.
 *
 * <pre class="java">{@code
 * import ratpack.http.client.HttpClient;
 * import ratpack.server.PublicAddress;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.net.URI;
 * import static org.junit.Assert.*;
 *
 * public class ExampleHttpClient {
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(chain -> {
 *         chain
 *           .get("simpleGet", ctx -> {
 *             PublicAddress address = ctx.get(PublicAddress.class);         //find local ip address
 *             HttpClient httpClient = ctx.get(HttpClient.class);            //get httpClient
 *             URI uri = address.get("httpClientGet");
 *
 *             httpClient.get(uri).then(response ->
 *                 ctx.render(response.getBody().getText())  //Render the response from the httpClient GET request
 *             );
 *           })
 *           .get("simplePost", ctx -> {
 *             PublicAddress address = ctx.get(PublicAddress.class);  //find local ip address
 *             HttpClient httpClient = ctx.get(HttpClient.class);     //get httpClient
 *             URI uri = address.get("httpClientPost");
 *
 *             httpClient.post(uri, s -> s.getBody().text("foo")).then(response ->
 *               ctx.render(response.getBody().getText())   //Render the response from the httpClient POST request
 *             );
 *           })
 *           .get("httpClientGet", ctx -> ctx.render("httpClientGet"))
 *           .post("httpClientPost", ctx -> ctx.render(ctx.getRequest().getBody().map(b -> b.getText().toUpperCase())));
 *       }
 *     ).test(testHttpClient -> {
 *       assertEquals("httpClientGet", testHttpClient.getText("/simpleGet"));
 *       assertEquals("FOO", testHttpClient.getText("/simplePost"));
 *     });
 *   }
 * }
 *
 * }</pre>
 */
public interface HttpClient extends AutoCloseable {

  /**
   * Creates a new HTTP client.
   *
   * @param action configuration for the client
   * @return a HTTP client
   * @throws Exception any thrown by {@code action}
   * @see HttpClientSpec
   * @since 1.4
   */
  static HttpClient of(Action<? super HttpClientSpec> action) throws Exception {
    return DefaultHttpClient.of(action);
  }

  /**
   * An asynchronous method to do a GET HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec, but the method will be defaulted to a GET.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param action An action that will act on the {@link RequestSpec}
   * @return A promise for a {@link ReceivedResponse}
   */
  Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action);

  default Promise<ReceivedResponse> get(URI uri) {
    return get(uri, Action.noop());
  }

  /**
   * The buffer allocator used by the client.
   *
   * @since 1.4
   */
  ByteBufAllocator getByteBufAllocator();

  /**
   * The number of connections that the client will pool for any given server.
   *
   * @since 1.4
   */
  int getPoolSize();

  /**
   * The default read timeout value.
   *
   * @since 1.4
   */
  Duration getReadTimeout();

  /**
   * The maximum response length accepted by the client.
   *
   * @since 1.4
   */
  int getMaxContentLength();

  /**
   * Closes any pooled connections.
   *
   * @since 1.4
   */
  @Override
  void close();

  /**
   * Create a new HttpClient by appending the provided configuration to this client.
   *
   * @param action The additional configuration to apply to the new client
   * @return a http client
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   */
  HttpClient copyWith(Action<? super HttpClientSpec> action) throws Exception;

  /**
   * An asynchronous method to do a POST HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec, but the method will be defaulted to a POST.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param action An action that will act on the {@link RequestSpec}
   * @return A promise for a {@link ReceivedResponse}
   */
  Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action);

  /**
   * An asynchronous method to do a HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param action An action that will act on the {@link RequestSpec}
   * @return A promise for a {@link ReceivedResponse}
   */
  Promise<ReceivedResponse> request(URI uri, Action<? super RequestSpec> action);

  /**
   * An asynchronous method to do a HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec,
   * the received response content will be streamed.
   * <p>
   * In order to access the response content stream either subscribe to the {@link Publisher} returned from {@link StreamedResponse#getBody()}
   * or use {@link StreamedResponse#forwardTo(ratpack.http.Response, Action)} to directly stream the content as a server response.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param requestConfigurer an action that will act on the {@link RequestSpec}
   * @return a promise for a {@link StreamedResponse}
   *
   * @see StreamedResponse
   */
  Promise<StreamedResponse> requestStream(URI uri, final Action<? super RequestSpec> requestConfigurer);

  /**
   * @deprecated since 1.4, use {@link #of(Action)}
   */
  @Deprecated
  static HttpClient httpClient(ServerConfig serverConfig, Registry registry) {
    return Exceptions.uncheck(() -> HttpClient.of(s -> s
      .poolSize(0)
      .byteBufAllocator(registry.get(ByteBufAllocator.class))
      .maxContentLength(serverConfig.getMaxContentLength())
    ));
  }

  /**
   * @deprecated since 1.4, use {@link #of(Action)}
   */
  @Deprecated
  static HttpClient httpClient(ByteBufAllocator byteBufAllocator, int maxContentLengthBytes) {
    return Exceptions.uncheck(() -> HttpClient.of(s -> s
      .poolSize(0)
      .byteBufAllocator(byteBufAllocator)
      .maxContentLength(maxContentLengthBytes)
    ));
  }

}
