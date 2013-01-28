/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework

import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.Handler
import org.vertx.java.core.buffer.Buffer

/**
 * A response to a request.
 */
interface Response {

  Request getRequest()

  Map<String, ?> getHeaders()

  int getStatus()

  void setStatus(int status)

  void setContentType(String contentType)

  Handler<Buffer> renderer()

  void render(Map<String, ?> model, String templateName)

  void render(String templateName)

  void renderJson(Object o)

  void renderString(String str)

  /**
   * Sends a temporary redirect response (i.e. statusCode 301) to the client using the specified redirect location URL.
   *
   * To use a different status code, call {@link #setStatus(int)} after calling this method.
   *
   * @param location the redirect location URL
   */
  void sendRedirect(String location)

  public <T> Handler<T> errorHandler(Handler<T> handler)

  public <T> AsyncResultHandler<T> asyncErrorHandler(Handler<T> handler)

  public void error(Exception e)

}
