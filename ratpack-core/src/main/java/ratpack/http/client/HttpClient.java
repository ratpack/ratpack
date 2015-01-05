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

import ratpack.exec.Promise;
import ratpack.func.Action;

import java.net.URI;

/**
 * A http client that makes all HTTP requests asynchronous and returns a {@link ratpack.exec.Promise}.
 * <p>
 * All details of the request are configured by the {@link ratpack.func.Action} acting on the {@link ratpack.http.client.RequestSpec}.
 * <p>
 * Example of a simple GET and POST request.
 *
 * <pre class="java">{@code
 *
 * import ratpack.server.PublicAddress;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.net.URI;
 *
 * public class ExampleHttpClient {
 *
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.fromChain(chain -> {
 *         chain
 *           .get("/simpleGet", context -> {
 *             PublicAddress address = context.get(PublicAddress.class);         //find local ip address
 *             HttpClient httpClient = context.get(HttpClient.class);            //get httpClient
 *
 *             httpClient.get(new URI(address.getAddress(context).toString() + "/httpClientGet")).then(response -> {
 *                 context.render(response.getBody().getText());  //Render the response from the httpClient GET request
 *               }
 *             );
 *           })
 *           .get("/simplePost", context -> {
 *             PublicAddress address = context.get(PublicAddress.class);  //find local ip address
 *             HttpClient httpClient = context.get(HttpClient.class);     //get httpClient
 *
 *             httpClient.post(new URI(address.getAddress(context).toString() + "/httpClientPost"), (action -> {
 *               action.body((body -> {
 *                 body.text("foo");   //Configure the POST body
 *               }));
 *             })).then(response -> {
 *               context.render(response.getBody().getText());   //Render the response from the httpClient POST request
 *             });
 *           })
 *           .get("httpClientGet", context -> {
 *             context.render("httpClientGet");
 *           })
 *           .post("httpClientPost", context -> {
 *             context.render(context.getRequest().getBody().getText().toUpperCase());
 *           });
 *       }
 *     ).test(testHttpClient -> {
 *       assert testHttpClient.get("/simpleGet").getBody().getText().equals("httpClientGet");
 *       assert testHttpClient.get("/simplePost").getBody().getText().equals("FOO");
 *     });
 *   }
 * }
 * }</pre>
 */
public interface HttpClient {

  /**
   * An asynchronous method to do a GET HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec, but the method will be defaulted to a GET.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param action An action that will act on the {@link RequestSpec}
   * @return A promise for a {@link ratpack.http.client.ReceivedResponse}
   */
  Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> action);

  default Promise<ReceivedResponse> get(URI uri) {
    return get(uri, Action.noop());
  }

  /**
   * An asynchronous method to do a POST HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec, but the method will be defaulted to a POST.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param action An action that will act on the {@link RequestSpec}
   * @return A promise for a {@link ratpack.http.client.ReceivedResponse}
   */
  Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action);

  /**
   * An asynchronous method to do a HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param action An action that will act on the {@link RequestSpec}
   * @return A promise for a {@link ratpack.http.client.ReceivedResponse}
   */
  Promise<ReceivedResponse> request(URI uri, Action<? super RequestSpec> action);

  /**
   * An asynchronous method to do a HTTP request, the URL and all details of the request are configured by the Action acting on the RequestSpec,
   * the received response content will be streamed.
   * <p>
   * In order to access the response content stream either subscribe to the {@link org.reactivestreams.Publisher} returned from {@link StreamedResponse#getBody()}
   * or use {@link ratpack.http.client.StreamedResponse#send(ratpack.http.Response, ratpack.func.Action)} to directly stream the content as a server response.
   *
   * @param uri the request URL (as a URI), must be of the {@code http} or {@code https} protocol
   * @param requestConfigurer an action that will act on the {@link RequestSpec}
   * @return a promise for a {@link ratpack.http.client.StreamedResponse}
   *
   * @see ratpack.http.client.StreamedResponse
   */
  Promise<StreamedResponse> requestStream(URI uri, final Action<? super RequestSpec> requestConfigurer);

}
