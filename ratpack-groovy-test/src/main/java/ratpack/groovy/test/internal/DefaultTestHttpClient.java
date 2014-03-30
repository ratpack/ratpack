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


import ratpack.api.Nullable;
import ratpack.func.Actions;
import ratpack.groovy.test.TestHttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.test.ApplicationUnderTest;
import ratpack.func.Action;
import ratpack.test.internal.BlockingHttpClient;

import java.net.URI;
import java.net.URISyntaxException;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultTestHttpClient implements TestHttpClient {

  private final ApplicationUnderTest applicationUnderTest;
  private Action<? super RequestSpec> request = Actions.noop();
  private BlockingHttpClient client = new BlockingHttpClient();
  private ReceivedResponse response;
  private Action<RequestSpec> requestConfigurer;

  public DefaultTestHttpClient(ApplicationUnderTest applicationUnderTest, @Nullable Action<RequestSpec> requestConfigurer) {
    this.applicationUnderTest = applicationUnderTest;
    if (requestConfigurer != null) {
      this.requestConfigurer = requestConfigurer;
    } else {
      this.requestConfigurer = Actions.noop();
    }

    request = requestConfigurer;

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
    //TODO Consider Removal
  }

  private ReceivedResponse postRequest() {

    return response;
  }

  private ReceivedResponse sendRequest(final String method, String path) {

    ReceivedResponse receivedResponse = null;


    try {
      receivedResponse = client.request(toAbsolute(path), Actions.join(request, new SetMethod(method)));
    } catch (Throwable throwable) {
      throwable.printStackTrace();

    }

    response = receivedResponse;
    return receivedResponse;
  }

  private static class SetMethod implements Action<RequestSpec> {
    private String method;

    public SetMethod(String method) {
      this.method = method;
    }

    @Override
    public void execute(RequestSpec requestSpec) throws Exception {
      requestSpec.method(method);
    }
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

  @Override
  public void close() {
    client.close();
  }
}
