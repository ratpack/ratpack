/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.handling;

import org.ratpackframework.api.NonBlocking;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.http.ByAcceptsResponder;
import org.ratpackframework.server.RatpackServerSettings;
import org.ratpackframework.service.NotInServiceRegistryException;
import org.ratpackframework.service.ServiceRegistry;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * An exchange represents to the processing of a particular request.
 * <p>
 * It provides access to the request and response, along with the current <i>service</i>.
 * It is also the fundamental routing mechanism.
 * <p>
 * Different handlers that participate in the processing of an exchange may actually receive different instances.
 * Handler implementations should not assume that there is only one {@code Exchange} object during processing.
 * </p>
 * <h3>Routing</h3>
 * <p>
 * Handlers either finalize the exchange by sending the response, or delegating to another handler.
 * This can be done via either the {@link #next()} method or one of the {@code insert()} methods.
 * <p>
 * The {@link #next()} method will simply delegate the processing to the next handler in line.
 * If a handler does not want to process the exchange it should call this method.
 * No further request/response processing should be done after this as the exchange is from that point on
 * the responsibility of the next handler. The last handler always simply sends a 404 back to the client.
 * <p>
 * The {@code insert()} methods can be used to insert one or more handlers in the pipeline and then delegate
 * to the first inserted handler.
 * The last inserted handler's “next” handler will become the “next” handler of this exchange.
 * That is, the “insert” operation is inserting before the current next handler.
 * </p>
 * <h4>Inserting with a different service</h4>
 * <p>
 * There are variants of the {@code insert()} method that allow the injection of a new service.
 * The given service will <b>only</b> be the service for the inserted handlers.
 * It is in effect “scoped” to just the inserted handlers.
 * The service for existing handlers that are already part of the pipeline cannot be influenced.
 * This gives processing a hierarchical nature.
 * Handlers can delegate to a tree of handlers (inserted handlers can insert other handlers) that
 * operate in different contexts.
 * This can be used to partition the application and do things such as use a different {@link org.ratpackframework.error.ServerErrorHandler}
 * for a portion of the application.
 * </p>
 */
public interface Exchange extends ServiceRegistry {

  /**
   * The HTTP request.
   *
   * @return The HTTP request.
   */
  Request getRequest();

  /**
   * The HTTP response.
   *
   * @return The HTTP response.
   */
  Response getResponse();

  /**
   * The server configuration.
   *
   * @return The server configuration.
   */
  RatpackServerSettings getServerSettings();

  /**
   * Shorthand for {@link org.ratpackframework.service.ServiceRegistry#get(Class) getServiceRegistry().get(type)}.
   * <p>
   * The service is dependent on processing that has happened prior in the handling pipeline.
   *
   * @param type The type of object to fetch from the service
   * @param <T> The type of object to fetch from the service
   * @return An object of the requested type
   * @throws org.ratpackframework.service.NotInServiceRegistryException if no object of that type could be supplied by the service
   */
  <T> T get(Class<T> type) throws NotInServiceRegistryException;

  /**
   * Shorthand for {@link org.ratpackframework.service.ServiceRegistry#maybeGet(Class) getServiceRegistry().maybeGet(type)}.
   * <p>
   * The service is dependent on processing that has happened prior in the handling pipeline.
   *
   * @param type The type of object to fetch from the service
   * @param <T> The type of object to fetch from the service
   * @return An object of the requested type, or {@code null} if an object of the given type could not be supplied by the service
   */
  @Nullable
  <T> T maybeGet(Class<T> type);

  /**
   * Delegate handling to the next handler in line.
   * <p>
   * The request and response of this object should not be accessed after this method is called.
   */
  @NonBlocking
  void next();

  /**
   * Inserts some handlers into the pipeline, then delegates to the first.
   * <p>
   * The request and response of this object should not be accessed after this method is called.
   *
   * @param handlers The handlers to insert.
   */
  @NonBlocking
  void insert(List<Handler> handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the given service registry, then delegates to the first.
   * <p>
   * The given service registry is only applicable to the inserted handlers.
   * <p>
   * Almost always, the service registry should be a super set of the current service registry.
   *
   * @param serviceRegistry The service registry for the inserted handlers
   * @param handlers The handlers to insert
   */
  @NonBlocking
  void insert(ServiceRegistry serviceRegistry, List<Handler> handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the given service, then delegates to the first.
   * <p>
   * The given object will take precedence over object advertised by the given advertised type.
   * <p>
   * The object will only be retrievable by the type that is given.
   *
   * @param <P> The public type of the object
   * @param <T> The concrete type of the object
   * @param publicType The advertised type of the object (i.e. what it is retrievable by)
   * @param implementation The actual implementation
   * @param handlers The handlers to insert
   */
  @NonBlocking
  <P, T extends P> void insert(Class<P> publicType, T implementation, List<Handler> handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the the given object added to the service, then delegates to the first.
   * <p>
   * The given object will take precedence over any existing object available via its concrete type.
   *
   * @param object The object to add to the service for the handlers
   * @param handlers The handlers to insert
   */
  @NonBlocking
  void insert(Object object, List<Handler> handlers);

  /**
   * A buildable processing chain for conditional processing based on the HTTP request method.
   *
   * @return A buildable processing chain for conditional processing based on the HTTP request method.
   */
  ByMethodChain getMethods();

  /**
   * A buildable responder useful for performing content negotiation.
   *
   * @return A buildable responder useful for performing content negotiation.
   */
  ByAcceptsResponder getAccepts();


  // Shorthands for common service lookups

  /**
   * Forwards the exception to the {@link org.ratpackframework.error.ServerErrorHandler} in this service.
   * <p>
   * The default configuration of Ratpack includes a {@link org.ratpackframework.error.ServerErrorHandler} in all contexts.
   * A {@link org.ratpackframework.service.NotInServiceRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param exception The exception that occurred
   * @throws org.ratpackframework.service.NotInServiceRegistryException if no {@link org.ratpackframework.error.ServerErrorHandler} can be found in the service
   */
  @NonBlocking
  void error(Exception exception) throws NotInServiceRegistryException;

  /**
   * Forwards the error to the {@link org.ratpackframework.error.ClientErrorHandler} in this service.
   *
   * The default configuration of Ratpack includes a {@link org.ratpackframework.error.ClientErrorHandler} in all contexts.
   * A {@link org.ratpackframework.service.NotInServiceRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param statusCode The 4xx range status code that indicates the error type
   * @throws org.ratpackframework.service.NotInServiceRegistryException if no {@link org.ratpackframework.error.ClientErrorHandler} can be found in the service
   */
  @NonBlocking
  void clientError(int statusCode) throws NotInServiceRegistryException;

  /**
   * Executes the given runnable in a try/catch, where exceptions are given to {@link #error(Exception)}.
   * <p>
   * This can be used by handlers when they are jumping off thread.
   * Exceptions raised on the thread that called the handler's {@linkplain Handler#handle(Exchange) handle} will always be caught.
   * If the handler “moves” to another thread, it should call this method no the new thread to ensure that any thrown exceptions
   * are caught and forwarded appropriately.
   *
   * @param runnable The code to surround with error handling
   */
  void withErrorHandling(Runnable runnable);

  /**
   * The path tokens of the current {@link org.ratpackframework.path.PathBinding} in this exchange's service.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(PathBinding.class).getPathTokens()}.
   *
   * @return The path tokens of the current {@link org.ratpackframework.path.PathBinding} in this exchange's service
   * @throws org.ratpackframework.service.NotInServiceRegistryException if there is no {@link org.ratpackframework.path.PathBinding} in the current service
   */
  Map<String, String> getPathTokens() throws NotInServiceRegistryException;

  /**
   * All of path tokens of the current {@link org.ratpackframework.path.PathBinding} in this exchange's service.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(PathBinding.class).getAllPathTokens()}.
   *
   * @return The path tokens of the current {@link org.ratpackframework.path.PathBinding} in this exchange's service
   * @throws org.ratpackframework.service.NotInServiceRegistryException if there is no {@link org.ratpackframework.path.PathBinding} in the current service
   */
  Map<String, String> getAllPathTokens() throws NotInServiceRegistryException;

  /**
   * Gets the file relative to the current {@link org.ratpackframework.file.FileSystemBinding} in this exchange's service.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(FileSystemBinding.class).file(path)}.
   * <p>
   * The default configuration of Ratpack includes a {@link org.ratpackframework.file.FileSystemBinding} in all contexts.
   * A {@link org.ratpackframework.service.NotInServiceRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param path The path to pass to the {@link org.ratpackframework.file.FileSystemBinding#file(String)} method.
   * @return The file relative to the current {@link org.ratpackframework.file.FileSystemBinding} in this exchange's service
   * @throws org.ratpackframework.service.NotInServiceRegistryException if there is no {@link org.ratpackframework.file.FileSystemBinding} in the current service
   */
  File file(String path) throws NotInServiceRegistryException;

}
