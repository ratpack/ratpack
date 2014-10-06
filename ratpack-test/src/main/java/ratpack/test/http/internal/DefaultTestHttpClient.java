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

package ratpack.test.http.internal;


import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import ratpack.func.Action;
import ratpack.http.HttpUrlBuilder;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.test.ApplicationUnderTest;
import ratpack.test.http.TestHttpClient;
import ratpack.test.internal.BlockingHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultTestHttpClient implements TestHttpClient {

  private final ApplicationUnderTest applicationUnderTest;
  private final BlockingHttpClient client = new BlockingHttpClient();
  private final Action<? super RequestSpec> defaultRequestConfig;
  private final List<Cookie> cookies = Lists.newLinkedList();

  private Action<? super RequestSpec> request = Action.noop();
  private Action<? super ImmutableMultimap.Builder<String, Object>> params = Action.noop();

  private ReceivedResponse response;

  public DefaultTestHttpClient(ApplicationUnderTest applicationUnderTest, Action<? super RequestSpec> defaultRequestConfig) {
    this.applicationUnderTest = applicationUnderTest;
    this.defaultRequestConfig = defaultRequestConfig;
  }

  @Override
  public ApplicationUnderTest getApplicationUnderTest() {
    return applicationUnderTest;
  }

  @Override
  public void requestSpec(Action<? super RequestSpec> requestAction) {
    request = requestAction;
  }

  @Override
  public void params(Action<? super ImmutableMultimap.Builder<String, Object>> params) {
    this.params = params;
  }

  @Override
  public void resetRequest() {
    request = Action.noop();
    cookies.clear();
  }

  @Override
  public ReceivedResponse getResponse() {
    return response;
  }

  @Override
  public ReceivedResponse head() {
    return head("");
  }

  @Override
  public ReceivedResponse head(String path) {
    return sendRequest("HEAD", path);
  }

  @Override
  public ReceivedResponse options() {
    return options("");
  }

  @Override
  public ReceivedResponse options(String path) {
    return sendRequest("OPTIONS", path);
  }

  @Override
  public ReceivedResponse get() {
    return get("");
  }

  @Override
  public ReceivedResponse get(String path) {
    return sendRequest("GET", path);
  }

  @Override
  public String getText() {
    return getText("");
  }

  @Override
  public String getText(String path) {
    return get(path).getBody().getText();
  }

  @Override
  public ReceivedResponse post() {
    return post("");
  }

  @Override
  public ReceivedResponse post(String path) {
    return sendRequest("POST", path);
  }

  @Override
  public String postText() {
    return postText("");
  }

  @Override
  public String postText(String path) {
    post(path);
    return response.getBody().getText();
  }

  @Override
  public ReceivedResponse put() {
    return put("");
  }

  @Override
  public ReceivedResponse put(String path) {
    return sendRequest("PUT", path);
  }

  @Override
  public String putText() {
    return putText("");
  }

  @Override
  public String putText(String path) {
    return put(path).getBody().getText();
  }


  @Override
  public ReceivedResponse patch() {
    return patch("");
  }

  @Override
  public ReceivedResponse patch(String path) {
    return sendRequest("PATCH", path);
  }

  @Override
  public String patchText() {
    return patchText("");
  }

  @Override
  public String patchText(String path) {
    return patch(path).getBody().getText();
  }

  @Override
  public ReceivedResponse delete() {
    return delete("");
  }

  @Override
  public ReceivedResponse delete(String path) {
    return sendRequest("DELETE", path);
  }

  @Override
  public String deleteText() {
    return deleteText("");
  }

  @Override
  public String deleteText(String path) {
    return delete(path).getBody().getText();
  }

  private ReceivedResponse sendRequest(final String method, String path) {
    try {
      URI uri = builder(path).params(params).build();

      response = client.request(uri, 1, TimeUnit.MINUTES, Action.join(defaultRequestConfig, request, requestSpec -> {
        requestSpec.method(method);
        requestSpec.getHeaders().add(HttpHeaderConstants.COOKIE, ClientCookieEncoder.encode(cookies));
        requestSpec.getHeaders().add(HttpHeaderConstants.HOST, HostAndPort.fromParts(uri.getHost(), uri.getPort()).toString());
      }));
    } catch (Throwable throwable) {
      throw uncheck(throwable);
    }

    List<String> cookieHeaders = response.getHeaders().getAll("Set-Cookie");
    for (String cookieHeader : cookieHeaders) {
      Set<Cookie> decodedCookies = CookieDecoder.decode(cookieHeader);
      for (Cookie decodedCookie : decodedCookies) {
        if (cookies.contains(decodedCookie)) {
          cookies.remove(decodedCookie);
        }
        if (!decodedCookie.isDiscard()) {
          cookies.add(decodedCookie);
        }
      }
    }

    return response;
  }

  private HttpUrlBuilder builder(String path) {
    try {
      URI basePath = new URI(path);
      if (basePath.isAbsolute()) {
        return HttpUrlBuilder.base(basePath);
      } else {
        return HttpUrlBuilder.base(new URI(applicationUnderTest.getAddress().toString() + path));
      }
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }
  }

}
