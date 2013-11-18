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

package ratpack.handling.internal;

import ratpack.http.Headers;
import ratpack.http.HttpMethod;
import ratpack.http.Status;

public class ContextClose {

  private String requestUri;
  private HttpMethod requestMethod;
  private Headers responseHeaders;
  private Status status;
  private long closeTime;

  public ContextClose(long closeTime, String requestUri, HttpMethod requestMethod, Headers responseHeaders, Status status) {
    this.closeTime = closeTime;
    this.requestUri = requestUri;
    this.requestMethod = requestMethod;
    this.responseHeaders = responseHeaders;
    this.status = status;
  }

  public String getRequestUri() {
    return requestUri;
  }

  public HttpMethod getRequestMethod() {
    return requestMethod;
  }

  public Headers getResponseHeaders() {
    return responseHeaders;
  }

  public Status getStatus() {
    return status;
  }

  public long getCloseTime() {
    return closeTime;
  }

}
