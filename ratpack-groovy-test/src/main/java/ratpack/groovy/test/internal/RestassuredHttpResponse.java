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

package ratpack.groovy.test.internal;

import com.jayway.restassured.response.Response;
import ratpack.groovy.test.HttpHeader;
import ratpack.groovy.test.HttpResponse;

import java.util.List;
import java.util.Map;


public class RestassuredHttpResponse implements HttpResponse {

  Response response;

  RestassuredHttpResponse(Response response) {
    this.response = response;
  }

  @Override
  public int getStatusCode() {
    return response.getStatusCode();
  }

  @Override
  public String getBody() {
    return response.asString();
  }

  @Override
  public byte[] asByteArray() {
    return response.asByteArray();
  }

  @Override
  public String getHeader(String header) {
    return response.getHeader(header);
  }

  @Override
  public String header(String header) {
    return this.getHeader(header);
  }

  @Override
  public Map<String, String> getCookies() {
    return response.getCookies();
  }


}
