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
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import ratpack.exec.Promise;
import ratpack.form.Form;
import ratpack.form.internal.DefaultForm;
import ratpack.form.internal.FormDecoder;
import ratpack.func.Block;
import ratpack.handling.Context;
import ratpack.http.*;
import ratpack.server.PublicAddress;
import ratpack.session.Session;
import ratpack.session.SessionData;
import ratpack.util.Exceptions;
import ratpack.util.MultiValueMap;

import java.net.URI;
import java.util.*;

public class RatpackWebContext implements WebContext {

  private final Context context;
  private final SessionData session;
  private final Request request;
  private final Response response;
  private final Form form;

  private String responseContent = "";

  public RatpackWebContext(Context ctx, TypedData body, SessionData session) {
    this.context = ctx;
    this.session = session;
    this.request = ctx.getRequest();
    this.response = ctx.getResponse();

    if (isFormAvailable(request, body)) {
      this.form = FormDecoder.parseForm(ctx, body, MultiValueMap.empty());
    } else {
      this.form = new DefaultForm(MultiValueMap.empty(), MultiValueMap.empty());
    }
  }

  public static Promise<RatpackWebContext> from(Context ctx, boolean bodyBacked) {
    Promise<SessionData> sessionDataPromise = ctx.get(Session.class).getData();
    if (bodyBacked) {
      return ctx.getRequest().getBody().flatMap(body ->
        sessionDataPromise.map(sessionData -> new RatpackWebContext(ctx, body, sessionData))
      );
    } else {
      return sessionDataPromise.map(sessionData -> new RatpackWebContext(ctx, null, sessionData));
    }
  }

  @Override
  public String getRequestParameter(String name) {
    return Optional.ofNullable(request.getQueryParams().get(name))
      .orElseGet(() -> form.get(name));
  }

  @Override
  public Map<String, String[]> getRequestParameters() {
    return flattenMap(combineMaps(request.getQueryParams(), form));
  }

  private RequestAttributes getRequestAttributes() {
    return request.maybeGet(RequestAttributes.class).orElseGet(this::addEmptyRequestAttributes);
  }

  private RequestAttributes addEmptyRequestAttributes() {
    RequestAttributes attributes = new RequestAttributes();
    request.add(attributes);
    return attributes;
  }

  @Override
  public Object getRequestAttribute(String name) {
    return getRequestAttributes().getAttributes().get(name);
  }

  @Override
  public void setRequestAttribute(String name, Object value) {
    getRequestAttributes().getAttributes().put(name, value);
  }

  @Override
  public String getRequestHeader(String name) {
    return request.getHeaders().get(name);
  }

  @Override
  public void setSessionAttribute(String name, Object value) {
    if (value == null) {
      session.remove(name);
    } else {
      Exceptions.uncheck((Block)() -> session.set(name, value, session.getJavaSerializer()));
    }
  }

  @Override
  public Object getSessionAttribute(String name) {
    return Exceptions.uncheck(() -> session.get(name, session.getJavaSerializer()).orElse(null));
  }

  @Override
  public Object getSessionIdentifier() {
    return session.getSession().getId();
  }

  @Override
  public String getRequestMethod() {
    return request.getMethod().getName();
  }

  @Override
  public String getRemoteAddr() {
    return request.getRemoteAddress().getHost();
  }

  @Override
  public void writeResponseContent(String responseContent) {
    this.responseContent = responseContent;
  }

  @Override
  public void setResponseStatus(int code) {
    response.status(code);
  }

  @Override
  public void setResponseHeader(String name, String value) {
    response.getHeaders().set(name, value);
  }

  @Override
  public void setResponseContentType(String content) {
    response.contentType(content);
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
  public boolean isSecure() {
    return "HTTPS".equalsIgnoreCase(getScheme());
  }

  @Override
  public String getFullRequestURL() {
    return getAddress().toString() + request.getUri();
  }

  public void sendResponse(RequiresHttpAction action) {
    response.status(action.getCode());
    sendResponse();
  }

  public void sendResponse() {
    int statusCode = response.getStatus().getCode();
    if (statusCode >= 400) {
      context.clientError(statusCode);
    } else {
      response.send(MediaType.TEXT_HTML, responseContent);
    }
  }

  @Override
  public Collection<Cookie> getRequestCookies() {
    final List<Cookie> newCookies = new ArrayList<>();
    final Set<io.netty.handler.codec.http.cookie.Cookie> cookies = request.getCookies();
    for (final io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
      final Cookie newCookie = new Cookie(cookie.name(), cookie.value());
      newCookie.setDomain(cookie.domain());
      newCookie.setPath(cookie.path());
      newCookie.setMaxAge((int) cookie.maxAge());
      newCookie.setSecure(cookie.isSecure());
      newCookie.setHttpOnly(cookie.isHttpOnly());
      newCookies.add(newCookie);
    }
    return newCookies;
  }

  @Override
  public void addResponseCookie(Cookie cookie) {
    final DefaultCookie newCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
    newCookie.setDomain(cookie.getDomain());
    newCookie.setPath(cookie.getPath());
    if (cookie.getMaxAge() >= 0) {
      newCookie.setMaxAge(cookie.getMaxAge());
    }
    newCookie.setSecure(cookie.isSecure());
    newCookie.setHttpOnly(cookie.isHttpOnly());
    response.getCookies().add(newCookie);
  }

  @Override
  public String getPath() {
    return request.getPath();
  }

  public SessionData getSession() {
    return session;
  }

  private URI getAddress() {
    return context.get(PublicAddress.class).get();
  }

  private static boolean isFormAvailable(Request request, TypedData body) {
    HttpMethod method = request.getMethod();
    return body != null && body.getContentType().isForm() && (method.isPost() || method.isPut());
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

  private class RequestAttributes {
    private Map<String, Object> attributes = new HashMap<>();

    public Map<String, Object> getAttributes() {
      return attributes;
    }
  }
}
