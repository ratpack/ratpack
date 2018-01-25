/*
 * Copyright 2018 the original author or authors.
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

package ratpack.thymeleaf3.internal;

import ognl.IteratorEnumeration;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

public class ThymeleafHttpServletRequestAdapter implements HttpServletRequest {
  private Map<String, Object> attributes = new HashMap<>();

  @Override
  public String getAuthType() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Cookie[] getCookies() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public long getDateHeader(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getHeader(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getIntHeader(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getMethod() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getPathInfo() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getPathTranslated() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getContextPath() {
    return "" /* root context */;
  }

  @Override
  public String getQueryString() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getRemoteUser() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isUserInRole(String role) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Principal getUserPrincipal() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getRequestedSessionId() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getRequestURI() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public StringBuffer getRequestURL() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getServletPath() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public HttpSession getSession(boolean create) {
    if (create) {
      throw new UnsupportedOperationException("Not implemented");
    } else {
      return null /* no current session */;
    }
  }

  @Override
  public HttpSession getSession() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String changeSessionId() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Deprecated
  public boolean isRequestedSessionIdFromUrl() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void login(String username, String password) throws ServletException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void logout() throws ServletException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Enumeration<String> getAttributeNames() {
    return (Enumeration<String>) new IteratorEnumeration(attributes.keySet().iterator());
  }

  @Override
  public String getCharacterEncoding() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getContentLength() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public long getContentLengthLong() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getContentType() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getParameter(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Enumeration<String> getParameterNames() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String[] getParameterValues(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return Collections.emptyMap();
  }

  @Override
  public String getProtocol() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getScheme() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getServerName() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getServerPort() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public BufferedReader getReader() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getRemoteAddr() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getRemoteHost() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setAttribute(String name, Object o) {
    attributes.put(name, o);
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public Locale getLocale() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Enumeration<Locale> getLocales() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isSecure() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Deprecated
  public String getRealPath(String path) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getRemotePort() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getLocalName() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getLocalAddr() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getLocalPort() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public ServletContext getServletContext() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isAsyncStarted() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isAsyncSupported() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public DispatcherType getDispatcherType() {
    throw new UnsupportedOperationException("Not implemented");
  }
}
