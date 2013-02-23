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

package org.ratpackframework.app;

public interface Routing {

  String ALL_METHODS = "*";

  /**
   * Retrieves the specified service from the services registered at startup.
   *
   * @param type The type of the service.
   * @param <T> The type of the service.
   * @return The service instance.
   */
  <T> T service(Class<T> type);

  /**
   * Create an endpoint that delegates to an injected instance of the given type each request.
   *
   * @param endpointType The type of the endpoint to delegate to.
   * @return An endpoint that can be passed to one of the registration methods.
   */
  Endpoint inject(Class<? extends Endpoint> endpointType);

  /**
   * Adds a route, for the given method at the given path, to be handled by the given handler.
   *
   * You can specify {@value #ALL_METHODS} for the method to match all methods.
   *
   * @param method The HTTP method the handler is for
   * @param path The path to handle
   * @param endpoint The endpoint for the request
   */
  void route(String method, String path, Endpoint endpoint);

  /**
   * Adds a route, for the given method at the given path, to be handled by the given handler.
   *
   * You can specify {@value #ALL_METHODS} for the method to match all methods.
   *
   * @param method The HTTP method the handler is for
   * @param regex The regex pattern 
   * @param endpoint The endpoint for the request
   */
  void routeRe(String method, String regex, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "*"
   */
  void all(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "*"
   */
  void allRe(String path, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "get"
   */
  void get(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "get"
   */
  void getRe(String pattern, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "post"
   */
  void post(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "post"
   */
  void postRe(String pattern, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "put"
   */
  void put(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "put"
   */
  void putRe(String pattern, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "delete"
   */
  void delete(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "delete"
   */
  void deleteRe(String pattern, Endpoint endpoint);

}
