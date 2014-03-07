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

package ratpack.newrelic.internal;

import com.google.common.net.HttpHeaders;
import com.newrelic.api.agent.HeaderType;
import ratpack.http.Response;

public class RatpackResponse implements com.newrelic.api.agent.Response {

  private final Response response;

  public RatpackResponse(Response response) {
    this.response = response;
  }

  @Override
  public int getStatus() throws Exception {
    return response.getStatus().getCode();
  }

  @Override
  public String getStatusMessage() throws Exception {
    return response.getStatus().getMessage();
  }

  @Override
  public String getContentType() {
    return response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public void setHeader(String name, String value) {
    response.getHeaders().set(name, value);
  }

  @Override
  public HeaderType getHeaderType() {
    return HeaderType.HTTP;
  }

}
