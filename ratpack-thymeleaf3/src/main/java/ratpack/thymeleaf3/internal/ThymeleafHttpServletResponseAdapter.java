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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

public class ThymeleafHttpServletResponseAdapter implements HttpServletResponse {
  @Override
  public void addCookie(Cookie cookie) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean containsHeader(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String encodeURL(String url) {
    // FIXME: does the URL need to be encoded?
    return url;
  }

  @Override
  public String encodeRedirectURL(String url) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Deprecated
  public String encodeUrl(String url) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Deprecated
  public String encodeRedirectUrl(String url) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void sendError(int sc) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    throw new UnsupportedOperationException("Not implemented");

  }

  @Override
  public void setDateHeader(String name, long date) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addDateHeader(String name, long date) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setHeader(String name, String value) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addHeader(String name, String value) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setIntHeader(String name, int value) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addIntHeader(String name, int value) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setStatus(int sc) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Deprecated
  public void setStatus(int sc, String sm) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getStatus() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getHeader(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Collection<String> getHeaders(String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Collection<String> getHeaderNames() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getCharacterEncoding() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String getContentType() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setCharacterEncoding(String charset) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setContentLength(int len) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setContentLengthLong(long len) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setContentType(String type) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setBufferSize(int size) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getBufferSize() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void flushBuffer() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void resetBuffer() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isCommitted() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setLocale(Locale loc) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Locale getLocale() {
    throw new UnsupportedOperationException("Not implemented");
  }
}
