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
import org.pac4j.core.exception.RequiresHttpAction;
import ratpack.handling.Context;
import ratpack.http.MediaType;
import ratpack.server.PublicAddress;
import ratpack.session.store.SessionStorage;
import ratpack.util.MultiValueMap;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts a {@link ratpack.handling.Context} object to be usable as a {@link org.pac4j.core.context.WebContext}.
 * In order to separate foreground from background operations, methods that are part of {@code WebContext} should not
 * send the response; instead, they should store the information and only send the response as part of {@link #sendResponse()}.
 */
public class RatpackWebContext implements WebContext {
  private final Context context;
  private String responseContent = "";
  private String redirectLocation;

  /**
   * Constructs a new instance.
   *
   * @param context The context to adapt
   */
  public RatpackWebContext(Context context) {
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
    if (value == null) {
      getSessionStorage().remove(name);
    } else {
      getSessionStorage().put(name, value);
    }
  }

  @Override
  public Object getSessionAttribute(String name) {
    return getSessionStorage().get(name);
  }

  @Override
  public String getRequestMethod() {
    return context.getRequest().getMethod().getName();
  }

  @Override
  public void writeResponseContent(String responseContent) {
    this.responseContent = responseContent;
  }

  @Override
  public void setResponseStatus(int code) {
    context.getResponse().status(code);
  }

  @Override
  public void setResponseHeader(String name, String value) {
    context.getResponse().getHeaders().set(name, value);
  }

  @Override
  public String getServerName() {
    return getAddress().getHost();
  }

  @Override
  public int getServerPort() {
    return getAddress().getPort();
  }

  @Override
  public String getScheme() {
    return getAddress().getScheme();
  }

  @Override
  public String getFullRequestURL() {
    return context.getRequest().getUri();
  }

  @Override
  public void sendRedirect(String location) {
    this.redirectLocation = location;
  }

  public void sendResponse(RequiresHttpAction action) {
    context.getResponse().status(action.getCode(), action.getMessage());
    sendResponse();
  }

  public void sendResponse() {
    if (redirectLocation != null) {
      context.redirect(redirectLocation);
    } else {
      context.getResponse().send(MediaType.TEXT_HTML, responseContent);
    }
  }

  private SessionStorage getSessionStorage() {
    return context.getRequest().get(SessionStorage.class);
  }

  private URI getAddress() {
    return context.get(PublicAddress.class).getAddress(context);
  }
}
