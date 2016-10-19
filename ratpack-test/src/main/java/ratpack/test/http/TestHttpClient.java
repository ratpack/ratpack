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
import io.netty.handler.codec.http.cookie.Cookie;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
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
   * @param applicationUnderTest the Ratpack application to make requests against
   * @return a http client which is configured to make requests against the provided ApplicationUnderTest
   */
  static TestHttpClient testHttpClient(ApplicationUnderTest applicationUnderTest) {
    return testHttpClient(applicationUnderTest, null);
  }

  /**
   * A method to create an instance of the default implementation of TestHttpClient.
   * <p>
   * The settings provided can be overridden on a per request basis via {@link ratpack.test.http.TestHttpClient#requestSpec}.
   *
   * @param applicationUnderTest the Ratpack application to make requests against
   * @param requestConfigurer a {@link ratpack.func.Action} that will set up the {@link ratpack.http.client.RequestSpec} for all requests made through this instance of TestHttpClient
   * @return a http client which is configured to make requests against the provided ApplicationUnderTest
   */
  static TestHttpClient testHttpClient(ApplicationUnderTest applicationUnderTest, @Nullable Action<? super RequestSpec> requestConfigurer) {
    return new DefaultTestHttpClient(applicationUnderTest, Action.noopIfNull(requestConfigurer));
  }

  /**
   * @return the application requests are being made against
   */
  ApplicationUnderTest getApplicationUnderTest();

  /**
   * @param requestAction an {@link ratpack.func.Action} that will act on the {@link ratpack.http.client.RequestSpec} this is used to configure details of the next request
   * @return this
   */
  TestHttpClient requestSpec(Action<? super RequestSpec> requestAction);

  /**
   *
   * @param params the params that will be used with the HttpUrlBuilder passed into {@link ratpack.http.HttpUrlBuilder#params(Action)}
   * @return this
   */
  TestHttpClient params(Action<? super ImmutableMultimap.Builder<String, Object>> params);

  /**
   * Set the requestSpec back to a No Op default and clear the cookies.
   */
  void resetRequest();

  /**
   * @return the response from the last request sent
   */
  ReceivedResponse getResponse();

  /**
   * Make a HEAD request with a path of "" this is the same as calling head("").
   *
   * @return the response from the request
   */
  ReceivedResponse head();

  /**
   * Make a HEAD request to the specified path.
   *
   * @param path the path the request is made against
   * @return the response from the request
   */
  ReceivedResponse head(String path);

  /**
   * Make a OPTIONS request with a path of "" this is the same as calling options("").
   *
   * @return the response from the request
   */
  ReceivedResponse options();

  /**
   * Make a OPTIONS request to the specified path.
   *
   * @param path the path the request is made against
   * @return the response from the request
   */
  ReceivedResponse options(String path);

  /**
   *  A convenience method for doing a OPTIONS request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return the response body as a String
   * @since 1.1
   */
  String optionsText();

  /**
   * A convenience method for doing a OPTIONS request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path the path the request is made against
   * @return the response body as a String
   * @since 1.1
   */
  String optionsText(String path);

  /**
   * Make a GET request with a path of "" this is the same as calling get("").
   *
   * @return the response from the request
   */
  ReceivedResponse get();

  /**
   * Make a GET request to the specified path.
   *
   * @param path the path the request is made against
   * @return the response from the request
   */
  ReceivedResponse get(String path);

  /**
   * A convenience method for doing a GET request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * Useful if you need to only check details of the response body.
   *
   * @return the response body as a String
   */
  String getText();

  /**
   * A convenience method for doing a GET request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * Useful if you need to only check details of the response body.
   *
   * @param path the path the request is made against
   * @return the response body as a String
   */
  String getText(String path);

  /**
   * Make a POST request with a path of "" this is the same as calling post("").
   *
   * @return the response from the request
   */
  ReceivedResponse post();

  /**
   * Make a POST request to the specified path.
   *
   * @param path the path the request is made against
   * @return the response from the request
   */
  ReceivedResponse post(String path);

  /**
   * A convenience method for doing a POST request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return the response body as a String
   */
  String postText();

  /**
   * A convenience method for doing a POST request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path the path the request is made against
   * @return the response body as a String
   */
  String postText(String path);

  /**
   * Make a PUT request with a path of "" this is the same as calling put("").
   *
   * @return the response from the request
   */
  ReceivedResponse put();

  /**
   * Make a PUT request to the specified path.
   *
   * @param path the path the request is made against
   * @return the response from the request
   */
  ReceivedResponse put(String path);

  /**
   * A convenience method for doing a PUT request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return the response body as a String
   */
  String putText();

  /**
   * A convenience method for doing a PUT request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path the path the request is made against
   * @return the response body as a String
   */
  String putText(String path);

  /**
   * Make a PATCH request with a path of "" this is the same as calling patch("").
   *
   * @return the response from the request
   */
  ReceivedResponse patch();

  /**
   * Make a PATCH request to the specified path.
   *
   * @param path the path the request is made against
   * @return the response from the request
   */
  ReceivedResponse patch(String path);

  /**
   *  A convenience method for doing a PATCH request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return the response body as a String
   */
  String patchText();

  /**
   * A convenience method for doing a PATCH request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path the path the request is made against
   * @return the response body as a String
   */
  String patchText(String path);

  /**
   * Make a DELETE request with a path of "" this is the same as calling delete("").
   *
   * @return the response from the request
   */
  ReceivedResponse delete();

  /**
   * Make a DELETE request to the specified path.
   *
   * @param path the path the request is made against
   * @return the response from the request
   */
  ReceivedResponse delete(String path);

  /**
   * A convenience method for doing a DELETE request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @return the response body as a String
   */
  String deleteText();

  /**
   * A convenience method for doing a DELETE request then calling {@link ratpack.http.client.ReceivedResponse#getBody} then {@link ratpack.http.TypedData#getText}.
   *
   * @param path the path the request is made against
   * @return the response body as a String
   */
  String deleteText(String path);

  /**
   * Get cookies with {@code Path=} attribute equal to {@code path} and all its subpaths.
   * @param path a URI path attached to cookies
   * @return the list of cookies attached to the given {@code path}
   */
  List<Cookie> getCookies(String path);

  /**
   * Executes the request as specified by the provided {@link RequestSpec}.
   * <p>
   * If the request method is not specified by the provided action, it will default to being a GET request.
   * The action provided to this method is additive to that configured with the {@link #requestSpec} method.
   *
   * @param requestAction an action to configure this request
   * @return the response from the request
   * @since 1.2
   */
  ReceivedResponse request(Action<? super RequestSpec> requestAction);

  /**
   * Executes the request as specified by the provided {@link RequestSpec} against the provided path.
   * <p>
   * If the request method is not specified by the provided action, it will default to being a GET request.
   * The action provided to this method is additive to that configured with the {@link #requestSpec} method.
   *
   * @param path the path the request will be made against
   * @param requestAction an action to configure this request
   * @return the response from the request
   * @see #request
   * @since 1.2
   */
  ReceivedResponse request(String path, Action<? super RequestSpec> requestAction);
}
