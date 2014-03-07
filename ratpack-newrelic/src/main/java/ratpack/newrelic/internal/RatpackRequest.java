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

import com.google.common.collect.Iterables;
import com.newrelic.api.agent.HeaderType;
import ratpack.http.Request;

import java.util.Collections;
import java.util.Enumeration;

public class RatpackRequest implements com.newrelic.api.agent.Request {

  private final Request request;

  public RatpackRequest(Request request) {
    this.request = request;
  }

  @Override
  public String getRequestURI() {
    return request.getUri();
  }

  @Override
  public String getRemoteUser() {
    return null;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Enumeration getParameterNames() {
    return Collections.enumeration(request.getQueryParams().keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return Iterables.toArray(request.getQueryParams().getAll(name), String.class);
  }

  @Override
  public Object getAttribute(String name) {
    return null;
  }

  @Override
  public String getCookieValue(String name) {
    return request.oneCookie(name);
  }

  @Override
  public String getHeader(String name) {
    return request.getHeaders().get(name);
  }

  @Override
  public HeaderType getHeaderType() {
    return HeaderType.HTTP;
  }

}
