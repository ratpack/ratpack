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

package org.ratpackframework.groovy.app


public interface Routing extends org.ratpackframework.app.Routing {

  /**
   * Returns this object, for better IDE assist in the script files.
   *
   * @return this
   */
  Routing getRouting()

  /**
   * Adds a route, for the given method at the given path, to be handled by the given handler.
   *
   * The handler receives one parameter, the {@link org.ratpackframework.app.Request}.
   * <p>
   * The {@code path} must always start with a {@code /}.
   * <p>
   * The path may contain tokens, which are prefixed with a colon.
   * <pre>
   * register("get", "/:a") { Request request ->
   *    text "path = request.urlParams.a"
   * }
   * </pre>
   * <p>
   * You can specify "*" for the method to match all methods.
   *
   * @param method The HTTP method the handler is for
   * @param path The path to handle (must start with a /)
   * @param handler The closure to handle the request
   */
  void register(String method, String path, @HandlerClosure Closure<?> handler)

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "*"
   */
  void all(String path, @HandlerClosure Closure<?> handler)

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "get"
   */
  void get(String path, @HandlerClosure Closure<?> handler)

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "post"
   */
  void post(String path, @HandlerClosure Closure<?> handler)

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "put"
   */
  void put(String path, @HandlerClosure Closure<?> handler)

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "delete"
   */
  void delete(String path, @HandlerClosure Closure<?> handler)

}
