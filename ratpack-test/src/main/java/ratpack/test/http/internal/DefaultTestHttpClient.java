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
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import ratpack.func.Action;
import ratpack.http.HttpUrlBuilder;
import ratpack.http.MutableHeaders;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.test.ApplicationUnderTest;
import ratpack.test.http.TestHttpClient;
import ratpack.test.internal.BlockingHttpClient;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static ratpack.util.Exceptions.uncheck;

public class DefaultTestHttpClient implements TestHttpClient {

  private final ApplicationUnderTest applicationUnderTest;
  private final BlockingHttpClient client = new BlockingHttpClient();
  private final Action<? super RequestSpec> defaultRequestConfig;
  private final Map<String, List<Cookie>> cookies = Maps.newLinkedHashMap();

  private Action<? super RequestSpec> request = Action.noop();
  private Action<? super ImmutableMultimap.Builder<String, Object>> params = Action.noop();

  private RequestSpecCapturingDelegate requestSpecCapturingDelegate;

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
    request = (requestSpec) -> {
      requestSpecCapturingDelegate = new RequestSpecCapturingDelegate(requestSpec);
      requestAction.execute(requestSpecCapturingDelegate);
    };
    return this;
  }

  class RequestSpecCapturingDelegate implements RequestSpec {

    final RequestSpec requestSpec;

    public RequestSpecCapturingDelegate(RequestSpec requestSpec) {
      this.requestSpec = requestSpec;
    }

    String method = "GET";
    int maxRedirects = 10;
    boolean decompressResponse;

    @Override
    public RequestSpec redirects(int maxRedirects) {
      this.maxRedirects = maxRedirects;
      return requestSpec.redirects(maxRedirects);
    }

    @Override
    public RequestSpec sslContext(SSLContext sslContext) {
      return requestSpec.sslContext(sslContext);
    }

    @Override
    public MutableHeaders getHeaders() {
      return requestSpec.getHeaders();
    }

    @Override
    public RequestSpec headers(Action<? super MutableHeaders> action) throws Exception {
      return requestSpec.headers(action);
    }

    @Override
    public RequestSpec method(String method) {
      this.method = method;
      return requestSpec.method(method);
    }

    @Override
    public RequestSpec decompressResponse(boolean shouldDecompress) {
      this.decompressResponse = shouldDecompress;
      return requestSpec.decompressResponse(shouldDecompress);
    }

    @Override
    public URI getUrl() {
      return requestSpec.getUrl();
    }

    @Override
    public RequestSpec readTimeoutSeconds(int seconds) {
      return requestSpec.readTimeoutSeconds(seconds);
    }

    @Override
    public RequestSpec readTimeout(Duration duration) {
      return requestSpec.readTimeout(duration);
    }

    @Override
    public Body getBody() {
      return requestSpec.getBody();
    }

    @Override
    public RequestSpec body(Action<? super Body> action) throws Exception {
      return requestSpec.body(action);
    }

    @Override
    public RequestSpec basicAuth(String username, String password) {
      return requestSpec.basicAuth(username, password);
    }
  }

  @Override
  public TestHttpClient params(Action<? super ImmutableMultimap.Builder<String, Object>> params) {
    this.params = params;
    return this;
  }

  @Override
  public void resetRequest() {
    request = Action.noop();
    requestSpecCapturingDelegate = null;
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
    AtomicInteger maxRedirects = new AtomicInteger();

    try {
      URI uri = builder(path).params(params).build();

      response = client.request(uri, Duration.ofMinutes(60), Action.join(defaultRequestConfig, request, requestSpec -> {
        requestSpec.method(method);

        if (requestSpecCapturingDelegate != null) {
          maxRedirects.set(requestSpecCapturingDelegate.maxRedirects);
        }

        requestSpec.redirects(0); // we will take care of redirect here to handle cookies

        List<Cookie> requestCookies = getCookies(path);

        String encodedCookie = requestCookies.isEmpty() ? "" : ClientCookieEncoder.STRICT.encode(requestCookies);

        requestSpec.getHeaders().add(HttpHeaderConstants.COOKIE, encodedCookie);

        int port = uri.getPort() > 0 ? uri.getPort() : 80;
        requestSpec.getHeaders().add(HttpHeaderConstants.HOST, HostAndPort.fromParts(uri.getHost(), port).toString());
      }));
    } catch (Throwable throwable) {
      throw uncheck(throwable);
    }

    List<String> cookieHeaders = response.getHeaders().getAll("Set-Cookie");
    for (String cookieHeader : cookieHeaders) {
      Cookie decodedCookie = ClientCookieDecoder.STRICT.decode(cookieHeader);
      if (decodedCookie != null) {
        if (decodedCookie.value() == null || decodedCookie.value().isEmpty()) {
          // clear cookie with the given name, skip the other parameters (path, domain) in compare to
          cookies.forEach((key, list) -> {
            for (Iterator<Cookie> iter = list.listIterator(); iter.hasNext();) {
              if (iter.next().name().equals(decodedCookie.name())) {
                iter.remove();
              }
            }
          });
        } else {
          String cookiePath = decodedCookie.path();
          cookiePath = (cookiePath != null && !("".equals(cookiePath))) ? cookiePath : "/";
          List<Cookie> pathCookies = cookies.get(cookiePath);
          if (pathCookies == null) {
            pathCookies = Lists.newLinkedList();
            cookies.put(cookiePath, pathCookies);
          }
          if (pathCookies.contains(decodedCookie)) {
            pathCookies.remove(decodedCookie);
          }
          pathCookies.add(decodedCookie);
        }
      }
    }

    int redirects = maxRedirects.get();
    boolean isRedirect = redirects > 0 && isRedirect(method, response.getStatusCode());
    if (isRedirect) {
      String redirectPath = response.getHeaders().get(HttpHeaderConstants.LOCATION);
      String redirectMethod = getRedirectMethod(method, response.getStatusCode());
      if (redirectPath != null) {
        response = sendRequest(redirectMethod, redirectPath);
      }
    }

    return response;
  }

  private String getRedirectMethod(String method, int statusCode) {
    switch(statusCode) {
      case 303:
        return HttpMethod.GET.name();
      default:
        return method;
    }
  }

  private boolean isRedirect(String httpMethod, int responseCode) {
    HttpMethod method = HttpMethod.valueOf(httpMethod);
    switch(responseCode) {
      case 300:
        return true;
      case 301:
      case 302:
        return HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method);
      case 303:
        return true;
      case 304:
        return false;
      case 305:
        return true;
      case 306:
        return false;
      case 307:
      case 308:
        return true;
      default:
        return false;
    }
  }

  private HttpUrlBuilder builder(String path) {
    try {
      URI basePath = new URI(path);
      if (basePath.isAbsolute()) {
        return HttpUrlBuilder.base(basePath);
      } else {
        path = path.startsWith("/") ? path.substring(1) : path;
        return HttpUrlBuilder.base(new URI(applicationUnderTest.getAddress().toString() + path));
      }
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }
  }

  public List<Cookie> getCookies(String path) {
    List<Cookie> clonedList = Lists.newLinkedList();
    if (cookies == null) {
      return clonedList;
    }
    if (path == null || "".equals(path) || "/".equals(path)) {
      List<Cookie> list = cookies.get("/");
      if (list != null) {
        clonedList.addAll(list);
      }
    } else {
      cookies.forEach((key, list) -> {
        if ("/".equals(key)) {
          clonedList.addAll(list);
        } else if (path.startsWith(key)) {
          clonedList.addAll(list);
        }
      });
    }
    return clonedList;
  }
}
