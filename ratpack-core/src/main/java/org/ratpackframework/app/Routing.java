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

import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * Builder for how requests should be routed to endpoints.
 */
public interface Routing {

  /**
   * A special HTTP method value ({@value} that means ALL methods.
   */
  String ALL_METHODS = "*";

  /**
   * The injector that backs this application.
   */
  Injector getInjector();

  /**
   * Retrieves the specified service from the services registered at startup.
   *
   * @param type The type of the service.
   * @param <T> The type of the service.
   * @return The service instance.
   */
  <T> T service(Class<T> type);

  /**
   * Retrieves the specified service from the services registered at startup.
   *
   * @param key The key of the service.
   * @param <T> The type of the service.
   * @return The service instance.
   */
  <T> T service(Key<T> key);

  /**
   * Create an endpoint that delegates to an injected instance of the given type each request, using {@link #getInjector()}.
   *
   * <pre>
   *  get("/login", inject(LogingHandler.class));
   * </pre>
   * <p>
   *
   * This facilitates dependency injection into endpoints and the use of {@link org.ratpackframework.app.RequestScoped} services.
   * A new child module of the given {@code injector} is created internally and the given {@code endpointType} bound.
   * As this module is a child of the main app module, any services registered at initialisation are candidates for injection.
   * <p>
   * The {@link Request} and {@link Response} objects are also available for injection (as the same instances that will be passed
   * to {@link Endpoint#respond(Request, Response)}) as the actual endpoint will be instantiated within the request scope.

   * @param endpointType The type of the endpoint to delegate to.
   * @return An endpoint that can be passed to one of the registration methods.
   */
  Endpoint inject(Class<? extends Endpoint> endpointType);

  /**
   * Adds a route for the given method at the given path to the given endpoint.
   *
   * The path may contain "tokens" that will become the {@link org.ratpackframework.app.Request#getPathParams()}
   * for the request given to the endpoint. Tokens are path components that are prefixed with a ":" character.
   *
   * <pre>
   * route("GET", "/products/:id", new Endpoint() {
   *   void respond(Request request, Response response) {
   *     assert request.getPathParams().get("id") != null;
   *     …
   *   }
   * });
   * </pre>
   *
   * @param method The HTTP method of the route
   * @param path The tokenised path to route (must begin with "/")
   * @param endpoint The endpoint to route to
   */
  void route(String method, String path, Endpoint endpoint);

  /**
   * Adds a route for the given method at the given path to the given endpoint.
   *
   * The pattern may contain capture groups, which will become the {@link org.ratpackframework.app.Request#getPathParams()}
   * for the request given to the endpoint. The path params are keyed by their index, but as a string.
   *
   * <pre>
   * routeRe("GET", "/products/(.+)", new Endpoint() {
   *   void respond(Request request, Response response) {
   *     assert request.getPathParams().get("0") != null;
   *     …
   *   }
   * });
   * </pre>
   *
   * Capture groups can span multiple path components.
   *
   * @param method The HTTP method of the route
   * @param pattern The pattern of the path to route (must begin with "/")
   * @param endpoint The endpoint to route to
   */
  void routeRe(String method, String pattern, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "{@link #ALL_METHODS}"
   */
  void all(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "{@link #ALL_METHODS}"
   */
  void allRe(String path, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "GET"
   */
  void get(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "GET"
   */
  void getRe(String pattern, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "POST"
   */
  void post(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "POST"
   */
  void postRe(String pattern, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "PUT"
   */
  void put(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "PUT"
   */
  void putRe(String pattern, Endpoint endpoint);

  /**
   * Delegates {@link #route(String, String, Endpoint)} with a method of "DELETE"
   */
  void delete(String path, Endpoint endpoint);

  /**
   * Delegates {@link #routeRe(String, String, Endpoint)} with a method of "DELETE"
   */
  void deleteRe(String pattern, Endpoint endpoint);
}
