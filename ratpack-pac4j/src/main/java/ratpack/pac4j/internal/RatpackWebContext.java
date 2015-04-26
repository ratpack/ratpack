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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import ratpack.exec.ExecResult;
import ratpack.exec.Execution;
import ratpack.exec.Result;
import ratpack.exec.internal.DefaultResult;
import ratpack.form.Form;
import ratpack.handling.Context;
import ratpack.http.HttpMethod;
import ratpack.http.MediaType;
import ratpack.http.Request;
import ratpack.server.PublicAddress;
import ratpack.session.store.SessionStorage;
import ratpack.util.Exceptions;
import ratpack.util.MultiValueMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Adapts a {@link ratpack.handling.Context} object to be usable as a {@link org.pac4j.core.context.WebContext}.
 * In order to separate foreground from background operations, methods that are part of {@code WebContext} should not
 * send the response; instead, they should store the information and only send the response as part of {@link #sendResponse()}.
 */
public class RatpackWebContext implements WebContext {
  private final Context context;
  private String responseContent = "";
  private Form form;

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
    String value = context.getRequest().getQueryParams().get(name);
    if (value == null && isFormAvailable()) {
      value = getForm().get(name);
    }
    return value;
  }

  @Override
  public Map<String, String[]> getRequestParameters() {
    if (isFormAvailable()) {
      return flattenMap(combineMaps(context.getRequest().getQueryParams(), getForm()));
    } else {
      return flattenMap(context.getRequest().getQueryParams().getAll());
    }
  }

  @Override
  public String getRequestHeader(String name) {
    return context.getRequest().getHeaders().get(name);
  }

  @Override
  public void setSessionAttribute(String name, Object value) {
    if (value == null) {
      getSessionStorage().remove(name).then((numberRemoved) -> {
        //TODO Log
      });
    } else {
      getSessionStorage().set(name, value).then((success) -> {
        //TODO Log
      });
    }
  }

  @Override
  public Object getSessionAttribute(String name) {
    //FIXME This needs to actually work, I'm in an unmanaged thread.
    List<Object> result = new ArrayList<>(1);
    CountDownLatch countDownLatch = new CountDownLatch(1);

    SessionStorage sessionStorage =getSessionStorage();

    context.getController().getControl().exec().start((execution -> {
      sessionStorage.get(name, Object.class).then((obj) -> {
        if (obj.isPresent()) {
          result.add(obj.get());
        }
        countDownLatch.countDown();
      });
    }));

    try {
      countDownLatch.await();
    } catch (InterruptedException ex) {
      //todo log
    }

    if (result.size() > 0) {
      return result.get(0);
    } else {
      return null;
    }
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
    return getAddress().toString() + context.getRequest().getUri();
  }

  public void sendResponse(RequiresHttpAction action) {
    context.getResponse().status(action.getCode());
    sendResponse();
  }

  public void sendResponse() {
    int statusCode = context.getResponse().getStatus().getCode();
    if (statusCode >= 400) {
      context.clientError(statusCode);
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

  private boolean isFormAvailable() {
    Request request = context.getRequest();
    HttpMethod method = request.getMethod();
    return request.getBody().getContentType().isForm() && (method.isPost() || method.isPut());
  }

  private Form getForm() {
    if (form == null) {
      form = Exceptions.uncheck(() -> context.parse(Form.class));
    }
    return form;
  }

  private Map<String, List<String>> combineMaps(MultiValueMap<String, String> first, MultiValueMap<String, String> second) {
    Map<String, List<String>> result = Maps.newLinkedHashMap();
    Set<String> keys = Sets.newLinkedHashSet(Iterables.concat(first.keySet(), second.keySet()));
    for (String key : keys) {
      result.put(key, Lists.newArrayList(Iterables.concat(first.getAll(key), second.getAll(key))));
    }
    return result;
  }

  private Map<String, String[]> flattenMap(Map<String, List<String>> map) {
    Map<String, String[]> result = Maps.newLinkedHashMap();
    for (String key : map.keySet()) {
      result.put(key, Iterables.toArray(map.get(key), String.class));
    }
    return result;
  }
}
