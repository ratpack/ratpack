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


import org.ratpackframework.handler.Handler;

/**
 * The API for the routing file (i.e. ratpack.groovy)
 */
public interface Routing {

  String ALL_METHODS = "*";

  <T> T service(Class<T> type);

  /**
   * Adds a route, for the given method at the given path, to be handled by the given handler.
   *
   * You can specify {@value #ALL_METHODS} for the method to match all methods.
   *
   * @param method The HTTP method the handler is for
   * @param path The path to handle (must start with a /)
   * @param handler The handler for the request
   */
  void register(String method, String path, Handler<Response> handler);

  /**
   * Creates an instance of the given type, through the dependency injection mechanism.
   *
   * The created instance is then passed to {@link #register(String, String, Handler)}.
   *
   * @param method The HTTP method the handler is for
   * @param path The path to handle (must start with a /)
   * @param handlerType The type of the handler to be created with dependency injection.
   */
  void register(String method, String path, Class<? extends Handler<Response>> handlerType);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Handler)} with a method of "*"
   */
  void all(String path, Handler<Response> handler);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Class)} with a method of "*"
   */
  void all(String path, Class<? extends Handler<Response>> handlerType);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Handler)} with a method of "get"
   */
  void get(String path, Handler<Response> handler);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Class)} with a method of "get"
   */
  void get(String path, Class<? extends Handler<Response>> handlerType);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Handler)} with a method of "post"
   */
  void post(String path, Handler<Response> handler);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Class)} with a method of "post"
   */
  void post(String path, Class<? extends Handler<Response>> handlerType);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Handler)} with a method of "put"
   */
  void put(String path, Handler<Response> handler);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Class)} with a method of "put"
   */
  void put(String path, Class<? extends Handler<Response>> handlerType);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Handler)} with a method of "delete"
   */
  void delete(String path, Handler<Response> handler);

  /**
   * Delegates {@link #register(java.lang.String, java.lang.String, Class)} with a method of "delete"
   */
  void delete(String path, Class<? extends Handler<Response>> handlerType);

}
