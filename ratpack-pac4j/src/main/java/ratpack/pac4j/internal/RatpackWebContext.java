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

package ratpack.pac4j.internal;

import org.pac4j.core.context.WebContext;
import ratpack.handling.Context;
import ratpack.session.store.SessionStorage;
import ratpack.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts a {@link ratpack.handling.Context} object to be usable as a {@link org.pac4j.core.context.WebContext}.
 */
class RatpackWebContext implements WebContext {
  private final Context context;

  /**
   * Constructs a new instance.
   *
   * @param context The context to adapt
   */
  RatpackWebContext(Context context) {
    this.context = context;
  }

  @Override
  public String getRequestParameter(String name) {
    return context.getRequest().getQueryParams().get(name);
  }

  @Override
  public Map<String, String[]> getRequestParameters() {
    MultiValueMap<String, String> ratpackParams = context.getRequest().getQueryParams();
    Map<String, String[]> requestParameters = new HashMap<>();
    for (String key : ratpackParams.keySet()) {
      List<String> values = ratpackParams.getAll(key);
      requestParameters.put(key, values.toArray(new String[values.size()]));
    }
    return requestParameters;
  }

  @Override
  public String getRequestHeader(String name) {
    return context.getRequest().getHeaders().get(name);
  }

  @Override
  public void setSessionAttribute(String name, Object value) {
    context.get(SessionStorage.class).put(name, value);
  }

  @Override
  public Object getSessionAttribute(String name) {
    return context.get(SessionStorage.class).get(name);
  }

  @Override
  public String getRequestMethod() {
    return context.getRequest().getMethod().getName();
  }

  @Override
  public void writeResponseContent(String content) {
    context.getResponse().send(content);
  }

  @Override
  public void setResponseStatus(int code) {
    context.getResponse().getStatus().set(code);
  }

  @Override
  public void setResponseHeader(String name, String value) {
    context.getResponse().getHeaders().set(name, value);
  }
}
