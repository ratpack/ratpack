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


import com.google.common.collect.*;
import com.google.common.net.HostAndPort;
import io.netty.handler.codec.http.*;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ratpack.util.Exceptions.uncheck;

public class DefaultTestHttpClient implements TestHttpClient {

  private final ApplicationUnderTest applicationUnderTest;
  private final BlockingHttpClient client = new BlockingHttpClient();
  private final Action<? super RequestSpec> defaultRequestConfig;
  private final List<Cookie> cookies = Lists.newLinkedList();
  private final Map<String,List<Cookie>> cookies2 = Maps.newLinkedHashMap();

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
  public TestHttpClient requestSpec(Action<? super RequestSpec> requestAction) {
    request = requestAction;
    return this;
  }

  @Override
  public TestHttpClient params(Action<? super ImmutableMultimap.Builder<String, Object>> params) {
    this.params = params;
    return this;
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

      response = client.request(uri, Duration.ofMinutes(60), Action.join(defaultRequestConfig, request, requestSpec -> {
        requestSpec.method(method);

        // filter cookies only for the given path and its subpaths or that are assigned to root path
        List<Cookie> requestCookies = Lists.newLinkedList();
        if (path == null || "".equals(path) || "/".equals(path)) {
          List<Cookie> list = cookies2.get("/");
          if (list != null) {
            requestCookies.addAll(list);
          }
        } else {
          cookies2.forEach((key, list) -> {
            if ("/".equals(key)) {
              requestCookies.addAll(list);
            } else if (path.startsWith(key)) {
              requestCookies.addAll(list);
            }
          });
        }

        String encodedCookie = requestCookies.isEmpty() ? "" : ClientCookieEncoder.encode(requestCookies);

        requestSpec.getHeaders().add(HttpHeaderConstants.COOKIE, encodedCookie);
        requestSpec.getHeaders().add(HttpHeaderConstants.HOST, HostAndPort.fromParts(uri.getHost(), uri.getPort()).toString());
      }));
    } catch (Throwable throwable) {
      throw uncheck(throwable);
    }

    List<String> cookieHeaders = response.getHeaders().getAll("Set-Cookie");
    for (String cookieHeader : cookieHeaders) {
      System.out.println("TEST CLIENT COOKIE HEADER: " + cookieHeader);
      Cookie decodedCookie = ClientCookieDecoder.decode(cookieHeader);
      if (decodedCookie != null) {
        System.out.println("TEST CLIENT DECODED COOKIE: " + decodedCookie.name() + " PATH: " + decodedCookie.path());
        if (cookies.contains(decodedCookie)) {
          cookies.remove(decodedCookie);
        }
        if (!decodedCookie.isDiscard()) {
          cookies.add(decodedCookie);
        }
        String cookiePath = decodedCookie.path();
        cookiePath = (cookiePath != null && !("".equals(cookiePath))) ? cookiePath : "/";
        List<Cookie> pathCookies = cookies2.get(cookiePath);
        if (pathCookies == null) {
          pathCookies = Lists.newLinkedList();
          cookies2.put(cookiePath, pathCookies);
        }
        if (pathCookies.contains(decodedCookie)) {
          pathCookies.remove(decodedCookie);
        }
        if (!decodedCookie.isDiscard()) {
          pathCookies.add(decodedCookie);
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

  public List<Cookie> getCookies() {
    List<Cookie> clonedList = new ArrayList<>();
    if (cookies != null) {
      cookies.stream().forEach(c -> clonedList.add(new DefaultCookie(c.name(), c.value())));
    }
    return clonedList;
  }

  public List<Cookie> getCookies(String path) {
    List<Cookie> clonedList = Lists.newLinkedList();
    if (cookies2 == null) {
      return clonedList;
    }
    List<Cookie> pathCookies = path == null ? cookies2.get("/") : cookies2.get(path);
    if (pathCookies != null) {
      clonedList.addAll(pathCookies);
    }
    return clonedList;
  }
}
