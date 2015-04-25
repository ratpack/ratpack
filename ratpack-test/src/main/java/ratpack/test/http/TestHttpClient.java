/*
 * Copyright 2013 the original author or authors.
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

package ratpack.test.http;

import com.google.common.collect.ImmutableMultimap;
import io.netty.handler.codec.http.Cookie;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.server.RatpackServer;
import ratpack.test.ApplicationUnderTest;
import ratpack.test.http.internal.DefaultTestHttpClient;

import java.util.List;

/**
 * A Http Client focused on testing Ratpack applications.
 */
public interface TestHttpClient {

  /**
   *  A method to create an instance of the default implementation of TestHttpClient.
   *
   * @param applicationUnderTest Which Ratpack application to make requests against.
   * @return {@link ratpack.test.http.TestHttpClient} which is configured to make requests against the provided ApplicationUnderTest
   */
  public static TestHttpClient testHttpClient(ApplicationUnderTest applicationUnderTest) {
    return testHttpClient(applicationUnderTest, null);
  }

  /**
   * A method to create an instance of the default implementation of TestHttpClient.
   *
   * @param applicationUnderTest Which Ratpack application to make requests against.
   * @param requestConfigurer A {@link ratpack.func.Action} that will set up the {@link ratpack.http.client.RequestSpec} for all requests made through this instance of TestHttpClient. These settings can be overridden on a per request basis via {@link ratpack.test.http.TestHttpClient#requestSpec}.
   * @return {@link ratpack.test.http.TestHttpClient} which is configured to make requests against the provided ApplicationUnderTest
   */
  public static TestHttpClient testHttpClient(ApplicationUnderTest applicationUnderTest, @Nullable Action<? super RequestSpec> requestConfigurer) {
    return new DefaultTestHttpClient(applicationUnderTest, Action.noopIfNull(requestConfigurer));
  }

  public static TestHttpClient testHttpClient(RatpackServer server) {
    return testHttpClient(() -> server);
  }

  public static TestHttpClient testHttpClient(Factory<? extends RatpackServer> server) {
    return new DefaultTestHttpClient(ApplicationUnderTest.of(server), Action.noop());
  }

  /**
   * @return The {@link ratpack.test.ApplicationUnderTest} requests are being made against.
   */
  ApplicationUnderTest getApplicationUnderTest();


  /**
   * @param requestAction A {@link ratpack.func.Action} that will act on the {@link ratpack.http.client.RequestSpec} this is used to configure details of the next request.
   * @return this
   */
  TestHttpClient requestSpec(Action<? super RequestSpec> requestAction);

  /**
   *
   * @param params The params that will be used with the HttpUrlBuilder passed into {@link ratpack.http.HttpUrlBuilder#params(Action)}
   * @return this
   */
  TestHttpClient params(Action<? super ImmutableMultimap.Builder<String, Object>> params);

  /**
   * Set the requestSpec back to a No Op default and clear the cookies.
   */
  void resetRequest();

  /**
   * @return The {@link ratpack.http.client.ReceivedResponse} from the last request sent.
   */
  ReceivedResponse getResponse();

  /**
   * Make a HEAD request with a path of "" this is the same as calling head("")
   *
   * @return The {@link ratpack.http.client.ReceivedResponse} from the HEAD request.
   */
  ReceivedResponse head();

  /**
   * Make a HEAD request to the specified path.
   *
   * @param path What path the HEAD request will be made against.
   * @return The {@link ratpack.http.client.ReceivedResponse} from the HEAD request.
   */
  ReceivedResponse head(String path);

  /**
   * Make a OPTIONS request with a path of "" this is the same as calling options("")
   *
   * @return The {@link ratpack.http.client.ReceivedResponse} from the OPTIONS request.
   */
  ReceivedResponse options();

  /**
   * Make a OPTIONS request to the specified path.
   *
   * @param path What path the OPTIONS request will be made against.
   * @return The {@link ratpack.http.client.ReceivedResponse} from the OPTIONS request.
   */
  ReceivedResponse options(String path);

  /**
   * Make a GET request with a path of "" this is the same as calling get("")
   *
   * @return The {@link ratpack.http.client.ReceivedResponse} from the GET request.
   */
  ReceivedResponse get();

  /**
   * Make a GET request to the specified path.
   *
   * @param path What path the GET request will be made against.
   * @return The {@link ratpack.http.client.ReceivedResponse} from the GET request.
   */
  ReceivedResponse get(String path);

  /**
   * A convenience method for doing a GET request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * Useful if you need to only check details of the response body.
   *
   * @return A String that is the body of the response.
   */
  String getText();

  /**
   * A convenience method for doing a GET request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * Useful if you need to only check details of the response body.
   *
   * @param path What path the GET request will be made against.
   * @return A String that is the body of the response.
   */
  String getText(String path);

  /**
   * Make a POST request with a path of "" this is the same as calling post("")
   *
   * @return The {@link ratpack.http.client.ReceivedResponse} from the POST request.
   */
  ReceivedResponse post();

  /**
   * Make a POST request to the specified path.
   *
   * @param path What path the POST request will be made against.
   * @return The {@link ratpack.http.client.ReceivedResponse} from the POST request.
   */
  ReceivedResponse post(String path);

  /**
   * A convenience method for doing a POST request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return A String that is the body of the response.
   */
  String postText();

  /**
   * A convenience method for doing a GET request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path What path the POST request will be made against.
   * @return A String that is the body of the response.
   */
  String postText(String path);

  /**
   * Make a PUT request with a path of "" this is the same as calling put("")
   *
   * @return The {@link ratpack.http.client.ReceivedResponse} from the PUT request.
   */
  ReceivedResponse put();

  /**
   * Make a PUT request to the specified path.
   *
   * @param path What path the PUT request will be made against.
   * @return The {@link ratpack.http.client.ReceivedResponse} from the PUT request.
   */
  ReceivedResponse put(String path);

  /**
   * A convenience method for doing a PUT request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return A String that is the body of the response.
   */
  String putText();

  /**
   * A convenience method for doing a PUT request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path What path the PUT request will be made against.
   * @return A String that is the body of the response.
   */
  String putText(String path);

  /**
   * Make a PATCH request with a path of "" this is the same as calling patch("")
   *
   * @return The {@link ratpack.http.client.ReceivedResponse} from the PATCH request.
   */
  ReceivedResponse patch();

  /**
   * Make a PATCH request to the specified path.
   *
   * @param path Make a PATCH request to the specified path.
   * @return The {@link ratpack.http.client.ReceivedResponse} from the PATCH request.
   */
  ReceivedResponse patch(String path);

  /**
   *  A convenience method for doing a PATCH request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return A String that is the body of the response.
   */
  String patchText();

  /**
   * A convenience method for doing a PATCH request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path What path the PATCH request will be made against.
   * @return A String that is the body of the response.
   */
  String patchText(String path);

  /**
   * Make a DELETE request with a path of "" this is the same as calling delete("")
   *
   * @return The {@link ratpack.http.client.ReceivedResponse} from the DELETE request.
   */
  ReceivedResponse delete();

  /**
   * Make a DELETE request to the specified path.
   *
   * @param path What path the DELETE request will be made against.
   * @return The {@link ratpack.http.client.ReceivedResponse} from the DELETE request.
   */
  ReceivedResponse delete(String path);

  /**
   * A convenience method for doing a DELETE request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return A String that is the body of the response.
   */
  String deleteText();

  /**
   * A convenience method for doing a DELETE request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path What path the DELETE request will be made against.
   * @return A String that is the body of the response.
   */
  String deleteText(String path);

  /**
   * Get cookies with {@code Path=} attribute equal to {@code path} and all its subpaths.
   * @param path a URI path attached to cookies
   * @return the list of cookies attached to the given {@code path}
   */
  List<Cookie> getCookies(String path);
}
