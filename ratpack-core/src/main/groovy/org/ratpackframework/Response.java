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

package org.ratpackframework;

import groovy.lang.Closure;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import java.util.Map;

/**
 * A response to a request.
 */
@SuppressWarnings("UnusedDeclaration")
public interface Response {

  /**
   * The request that this response is for.
   *
   * @return the request that this response is for.
   */
  Request getRequest();

  /**
   * The headers to send out.
   *
   * Any non iterable values will be toString()'d. Iterable values indicate a multi value header, and each element will be toString()'d. Headers are not sent immediately. They will be sent when the
   * response is finalised (e.g. after one of the render*() methods).
   *
   * @return the headers.
   */
  Map<String, ?> getHeaders();

  /**
   * The status code that will be sent.
   *
   * Defaults to 200. Will be sent when the response is finalised.
   *
   * @return the status code that will be sent.
   */
  int getStatus();

  /**
   * The status code that will be sent.
   *
   * @param status the status code to send.
   */
  void setStatus(int status);

  /**
   * The response payload content type.
   *
   * Defaults to null.
   *
   * @param contentType the response payload content type.
   */
  void setContentType(String contentType);

  /**
   * Returns a handler that can be given a {@link Buffer}, that will finalise the request.
   *
   * @return a handler that can be given a {@link Buffer}, that will finalise the request.
   */
  Handler<Buffer> renderer();

  /**
   * Render the template at the given path (relative to the configure templates dir) with the given model.
   *
   * This will finalize the response. Note that rendering happens asynchronously, this will return immediately. Within the template, the given model is available as the “model” variable and is of type
   * {@link org.ratpackframework.templating.TemplateModel}. If the content type has not been set, sets it to “text/html;charset=utf-8”
   *
   * @param model the model data.
   * @param templatePath the relative path to the template.
   */
  void render(Map<String, ?> model, String templatePath);

  /**
   * Calls {@link #render(java.util.Map, String)} with an empty map for the model.
   *
   * @param templatePath the relative path to the template.
   */
  void render(String templatePath);

  /**
   * Attempts to render the given object as JSON.
   *
   * If the content type has not been set, sets it to “application/json;charset=utf-8 Finalizes the response.
   *
   * @param object object to render as JSON.
   */
  void renderJson(Object object);

  /**
   * Renders the toString() of the given object as plain text.
   *
   * If the content type has not been set, sets it to “text/plain;charset=utf-8” Finalizes the response.
   */
  void renderText(Object str);

  /**
   * Sends a temporary redirect response (i.e. statusCode 302) to the client using the specified redirect location URL.
   *
   * To use a different status code, call {@link #setStatus(int)} after calling this method.
   *
   * @param location the redirect location URL
   */
  void redirect(String location);

  /**
   * Returns a handler that simply catches any exceptions thrown by the given handler and forwards them to {@link #error(Exception)}
   *
   * @param handler The handler to wrap.
   * @param <T> The payload type
   * @return A handler that wraps the given one, adding exception handling.
   */
  public <T> Handler<T> errorHandler(Handler<T> handler);

  /**
   * Returns an async handler that wraps the given handler, delegating to {@link #error(Exception)} if the result is an error.
   *
   * The given handler will be wrapped by {@link #errorHandler(org.vertx.java.core.Handler)}.
   *
   * @param handler The success handler.
   * @param <T> The type of result.
   * @return An async result handler that handles errors and delegates success to the given handler.
   */
  public <T> AsyncResultHandler<T> asyncErrorHandler(Handler<T> handler);

  /**
   * End the response with an error (500), by passing the exception to the error handler.
   *
   * @param error What went wrong
   */
  public void error(Exception error);

  /**
   * Catches any exceptions thrown during the closure execution and handles appropriately.
   *
   * If you go off the first thread, you need to wrap any error throwing code in this.
   *
   * @param closure The code that may throw exceptions.
   */
  public void handleErrors(Closure<?> closure);
}
