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


import com.google.common.net.HostAndPort;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.func.Actions;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.test.ApplicationUnderTest;
import ratpack.test.http.TestHttpClient;
import ratpack.test.internal.BlockingHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultTestHttpClient implements TestHttpClient {

  private final ApplicationUnderTest applicationUnderTest;
  private Action<? super RequestSpec> request = Actions.noop();
  private BlockingHttpClient client = new BlockingHttpClient();
  private ReceivedResponse response;
  private Action<? super RequestSpec> requestConfigurer;
  private List<Cookie> cookies;

  public DefaultTestHttpClient(ApplicationUnderTest applicationUnderTest, @Nullable Action<? super RequestSpec> requestConfigurer) {
    this.applicationUnderTest = applicationUnderTest;
    if (requestConfigurer != null) {
      this.requestConfigurer = requestConfigurer;
    } else {
      this.requestConfigurer = Actions.noop();
    }

    cookies = new ArrayList<Cookie>();

    request = this.requestConfigurer;

  }

  @Override
  public ApplicationUnderTest getApplicationUnderTest() {
    return applicationUnderTest;
  }

  @Override
  public void requestSpec(Action<? super RequestSpec> requestAction) {
    if (requestAction != null) {
      request = Actions.join(requestConfigurer, requestAction);
    }
  }

  @Override
  public void resetRequest() {
    request = requestConfigurer;
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
    preRequest();
    response = sendRequest("HEAD", path);
    return postRequest();
  }

  @Override
  public ReceivedResponse options() {
    return options("");
  }

  @Override
  public ReceivedResponse options(String path) {
    preRequest();
    response = sendRequest("OPTIONS", path);
    return postRequest();
  }

  @Override
  public ReceivedResponse get() {
    return get("");
  }

  @Override
  public ReceivedResponse get(String path) {
    preRequest();
    response = sendRequest("GET", path);
    return postRequest();
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
    preRequest();
    response = sendRequest("POST", path);
    return postRequest();
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
    preRequest();
    response = sendRequest("PUT", path);
    return postRequest();
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
    preRequest();
    response = sendRequest("PATCH", path);
    return postRequest();
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
    preRequest();
    response = sendRequest("DELETE", path);
    return postRequest();
  }

  @Override
  public String deleteText() {
    return deleteText("");
  }

  @Override
  public String deleteText(String path) {
    return delete(path).getBody().getText();
  }

  private void preRequest() {
  }

  private ReceivedResponse postRequest() {
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

  private ReceivedResponse sendRequest(final String method, String path) {

    ReceivedResponse receivedResponse = null;


    try {
      receivedResponse = client.request(Actions.join(request, new FinalRequestConfig(method, toAbsolute(path), cookies)));
    } catch (Throwable throwable) {
      throw uncheck(throwable);
    }

    response = receivedResponse;
    return receivedResponse;
  }


  private static class FinalRequestConfig implements Action<RequestSpec> {
    private String method;
    private URI path;
    List<Cookie> cookies;

    public FinalRequestConfig(String method, URI path, List<Cookie> cookies) {
      this.method = method;
      this.path = path;
      this.cookies = cookies;
    }

    @Override
    public void execute(RequestSpec requestSpec) throws Exception {
      requestSpec.method(method);
      requestSpec.getUrl().set(path);
      requestSpec.getHeaders().add(HttpHeaderConstants.COOKIE, ClientCookieEncoder.encode(cookies));
      requestSpec.getHeaders().add(HttpHeaderConstants.HOST, HostAndPort.fromParts(path.getHost(), path.getPort()).toString());
    }
  }

  private URI toAbsolute(String path) {
    try {
      URI basePath = new URI(path);
      if (basePath.isAbsolute()) {
        return basePath;
      } else {
        URI address = applicationUnderTest.getAddress();
        return new URI(address.toString() + path);
      }
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }
  }

  @Override
  public void close() {
    client.close();
  }
}
