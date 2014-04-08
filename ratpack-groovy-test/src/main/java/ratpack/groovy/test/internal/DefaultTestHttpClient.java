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

package ratpack.groovy.test.internal;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import ratpack.api.Nullable;
import ratpack.groovy.test.HttpResponse;
import ratpack.groovy.test.TestHttpClient;
import ratpack.test.ApplicationUnderTest;
import ratpack.func.Action;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultTestHttpClient implements TestHttpClient {

  private final ApplicationUnderTest applicationUnderTest;
  private final Action<RequestSpecification> requestConfigurer;

  public DefaultTestHttpClient(ApplicationUnderTest applicationUnderTest, @Nullable Action<RequestSpecification> requestConfigurer) {
    this.applicationUnderTest = applicationUnderTest;
    this.requestConfigurer = requestConfigurer;
  }

  private RequestSpecification request;

  private Response response;
  private List<Cookie> cookies = new LinkedList<>();

  @Override
  public ApplicationUnderTest getApplicationUnderTest() {
    return applicationUnderTest;
  }

  @Override
  public RequestSpecification getRequest() {
    if (request == null) {
      request = createRequest();
    }
    return request;
  }

  @Override
  public HttpResponse getResponse() {
    return new RestassuredHttpResponse(response);
  }

  @Override
  public RequestSpecification resetRequest() {
    request = createRequest();
    return request;
  }

  @Override
  public HttpResponse head() {
    return head("");
  }

  @Override
  public HttpResponse head(String path) {
    preRequest();
    response = request.head(toAbsolute(path));
    return new RestassuredHttpResponse(postRequest());
  }

  @Override
  public HttpResponse options() {
    return options("");
  }

  @Override
  public HttpResponse options(String path) {
    preRequest();
    response = request.options(toAbsolute(path));
    return new RestassuredHttpResponse(postRequest());
  }

  @Override
  public HttpResponse get() {
    return get("");
  }

  @Override
  public HttpResponse get(String path) {
    preRequest();
    response = request.get(toAbsolute(path));
    return new RestassuredHttpResponse(postRequest());
  }

  @Override
  public String getText() {
    return getText("");
  }

  @Override
  public String getText(String path) {
    get(path);
    return response.asString();
  }

  @Override
  public HttpResponse post() {
    return post("");
  }

  @Override
  public HttpResponse post(String path) {
    preRequest();
    response = request.post(toAbsolute(path));
    return new RestassuredHttpResponse(postRequest());
  }

  @Override
  public String postText() {
    return postText("");
  }

  @Override
  public String postText(String path) {
    post(path);
    return response.asString();
  }

  @Override
  public HttpResponse put() {
    return put("");
  }

  @Override
  public HttpResponse put(String path) {
    preRequest();
    response = request.put(toAbsolute(path));
    return new RestassuredHttpResponse(postRequest());
  }

  @Override
  public String putText() {
    return putText("");
  }

  @Override
  public String putText(String path) {
    return put(path).getBody();
  }


  @Override
  public HttpResponse patch() {
    return patch("");
  }

  @Override
  public HttpResponse patch(String path) {
    preRequest();
    response = request.patch(toAbsolute(path));
    return new RestassuredHttpResponse(postRequest());
  }

  @Override
  public String patchText() {
    return patchText("");
  }

  @Override
  public String patchText(String path) {
    return patch(path).getBody();
  }  

  @Override
  public HttpResponse delete() {
    return delete("");
  }

  @Override
  public HttpResponse delete(String path) {
    preRequest();
    response = request.delete(toAbsolute(path));
    return new RestassuredHttpResponse(postRequest());
  }

  @Override
  public String deleteText() {
    return deleteText("");
  }

  @Override
  public String deleteText(String path) {
    return delete(path).getBody();
  }

  @Override
  public RequestSpecification createRequest() {
    RequestSpecification request = RestAssured.with().urlEncodingEnabled(false);
    if (requestConfigurer != null) {
      try {
        requestConfigurer.execute(request);
      } catch (Exception e) {
        throw uncheck(e);
      }
    }
    return request;
  }

  private void preRequest() {
    if (request == null) {
      request = createRequest();
    }
    try {
      Field field = request.getClass().getDeclaredField("cookies");
      field.setAccessible(true);
      field.set(request, new Cookies(cookies));
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  private Response postRequest() {
    for (Cookie setCookie : response.getDetailedCookies()) {
      Date date = setCookie.getExpiryDate();
      for (Cookie priorCookie : new LinkedList<>(cookies)) {
        if (priorCookie.getName().equals(setCookie.getName())) {
          cookies.remove(priorCookie);
        }
      }
      if (date == null || date.compareTo(new Date()) > 0) {
        cookies.add(setCookie);
      }
    }

    return response;
  }

  private String toAbsolute(String path) {
    try {
      if (new URI(path).isAbsolute()) {
        return path;
      } else {
        URI address = applicationUnderTest.getAddress();
        return address.toString() + path;
      }
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }
  }

}
