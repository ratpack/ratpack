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
import org.ratpackframework.block.Blocking;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.path.PathTokens;
import org.ratpackframework.registry.NotInRegistryException;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.render.controller.NoSuchRendererException;
import org.ratpackframework.util.Action;
import org.ratpackframework.util.ResultAction;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * An context represents the context of an individual handler invocation, which is conceptually a reaction to a request.
 * <p>
 * It provides:
 * <ul>
 * <li>Access the HTTP request and response</li>
 * <li>Processing/delegation control (via the {@link #next} and {@link #insert} family of methods)</li>
 * <li>Access to <i>contextual objects</i> (see below)</li>
 * <li>Convenience mechanism for common handler operations</li>
 * </ul>
 * </p>
 * <p>
 * See {@link Handler} for information on how to work with a context object for request processing.
 * </p>
 * <h4>Contextual objects</h4>
 * <p>
 * A context is also a {@link Registry} of objects.
 * Arbitrary objects can be "pushed" into the context by <i>upstream</i> handlers for use by <i>downstream</i> handlers.
 * <p>
 * There are some significant contextual objects that drive key infrastructure.
 * For example, error handling is based on informing the contextual {@link org.ratpackframework.error.ServerErrorHandler} of exceptions.
 * The error handling strategy for an application can be changed by pushing a new implementation of this interface into the context that is used downstream.
 * <p>
 * See {@link #insert(Class, Object, java.util.List)} for more on how to do this.
 * <h5>Default contextual objects</h5>
 * <p>There is also a set of default objects that are made available via the Ratpack infrastructure:
 * <ul>
 * <li>The effective {@link org.ratpackframework.launch.LaunchConfig}</li>
 * <li>A {@link org.ratpackframework.file.FileSystemBinding} that is the application {@link org.ratpackframework.launch.LaunchConfig#getBaseDir()}</li>
 * <li>A {@link org.ratpackframework.file.MimeTypes} implementation</li>
 * <li>A {@link org.ratpackframework.error.ServerErrorHandler}</li>
 * <li>A {@link org.ratpackframework.error.ClientErrorHandler}</li>
 * <li>A {@link org.ratpackframework.file.FileRenderer}</li>
 * <li>A {@link org.ratpackframework.server.BindAddress}</li>
 * <li>A {@link org.ratpackframework.server.PublicAddress}</li>
 * <li>A {@link Redirector}</li>
 * </ul>
 */
@SuppressWarnings("UnusedDeclaration")
public interface Context extends Registry<Object> {

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
   * Inserts some handlers into the pipeline to execute with the given registry, then delegates to the first.
   * <p>
   * The given registry is only applicable to the inserted handlers.
   * <p>
   * Almost always, the registry should be a super set of the current registry.
   *
   * @param registry The registry for the inserted handlers
   * @param handlers The handlers to insert
   */
  @NonBlocking
  void insert(Registry<Object> registry, List<Handler> handlers);

  /**
   * Inserts some handlers into the pipeline to execute with the given object made available, then delegates to the first.
   * <p>
   * The given object will take precedence over an existing contextual object advertised by the given advertised type.
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
   * Convenience method for delegating to a single handler.
   * <p>
   * Designed to be used in conjunction with the {@link #getByMethod()} and {@link #getByContent()} methods.
   *
   * @see ByContentHandler
   * @see ByMethodHandler
   * @param handler The handler to invoke
   */
  @NonBlocking
  void respond(Handler handler);

  /**
   * A buildable handler for conditional processing based on the HTTP request method.
   *
   * @return A buildable handler for conditional processing based on the HTTP request method.
   */
  ByMethodHandler getByMethod();

  /**
   * A buildable handler useful for performing content negotiation.
   *
   * @return A buildable handler useful for performing content negotiation.
   */
  ByContentHandler getByContent();


  // Shorthands for common service lookups

  /**
   * Forwards the exception to the {@link org.ratpackframework.error.ServerErrorHandler} in this service.
   * <p>
   * The default configuration of Ratpack includes a {@link org.ratpackframework.error.ServerErrorHandler} in all contexts.
   * A {@link org.ratpackframework.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param exception The exception that occurred
   * @throws NotInRegistryException if no {@link org.ratpackframework.error.ServerErrorHandler} can be found in the service
   */
  @NonBlocking
  void error(Exception exception) throws NotInRegistryException;

  /**
   * Forwards the error to the {@link org.ratpackframework.error.ClientErrorHandler} in this service.
   *
   * The default configuration of Ratpack includes a {@link org.ratpackframework.error.ClientErrorHandler} in all contexts.
   * A {@link org.ratpackframework.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param statusCode The 4xx range status code that indicates the error type
   * @throws NotInRegistryException if no {@link org.ratpackframework.error.ClientErrorHandler} can be found in the service
   */
  @NonBlocking
  void clientError(int statusCode) throws NotInRegistryException;

  /**
   * Executes the given runnable in a try/catch, where exceptions are given to {@link #error(Exception)}.
   * <p>
   * This can be used by handlers when they are jumping off thread.
   * Exceptions raised on the thread that called the handler's {@linkplain Handler#handle(Context) handle} will always be caught.
   * If the handler “moves” to another thread, it should call this method no the new thread to ensure that any thrown exceptions
   * are caught and forwarded appropriately.
   *
   * @param runnable The code to surround with error handling
   */
  void withErrorHandling(Runnable runnable);

  /**
   * Creates a result action that uses the contextual error handler if the result is failure.
   * <p>
   * The given action is invoked if the result is successful.
   *
   * @param action The action to invoke on a successful result.
   * @param <T> The type of the successful result value
   * @return An action that takes {@code Result<T>}
   */
  <T> ResultAction<T> resultAction(Action<T> action);

  /**
   * The contextual path tokens of the current {@link org.ratpackframework.path.PathBinding}.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(PathBinding.class).getPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link org.ratpackframework.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link org.ratpackframework.path.PathBinding} in the current service
   */
  PathTokens getPathTokens() throws NotInRegistryException;

  /**
   * The contextual path tokens of the current {@link org.ratpackframework.path.PathBinding}.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(PathBinding.class).getAllPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link org.ratpackframework.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link org.ratpackframework.path.PathBinding} in the current service
   */
  PathTokens getAllPathTokens() throws NotInRegistryException;

  /**
   * Gets the file relative to the contextual {@link org.ratpackframework.file.FileSystemBinding}.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(FileSystemBinding.class).file(path)}.
   * <p>
   * The default configuration of Ratpack includes a {@link org.ratpackframework.file.FileSystemBinding} in all contexts.
   * A {@link org.ratpackframework.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param path The path to pass to the {@link org.ratpackframework.file.FileSystemBinding#file(String)} method.
   * @return The file relative to the contextual {@link org.ratpackframework.file.FileSystemBinding}
   * @throws NotInRegistryException if there is no {@link org.ratpackframework.file.FileSystemBinding} in the current service
   */
  File file(String path) throws NotInRegistryException;

  /**
   * Render the given object, using the rendering framework.
   * <p>
   * This will retrieve all contextual renderers, and use the first one that can render the object.
   * <p>
   * This will finalize the response, no further processing should be done.
   * <p>
   * The default configuration of Ratpack always makes a renderer for {@link File} objects available.
   * <p>
   * See {@link org.ratpackframework.render.Renderer} for more on the rendering framework.
   *
   * @param object The object to render
   * @throws NoSuchRendererException If there is no suitable renderer for the object
   */
  @NonBlocking
  void render(Object object) throws NoSuchRendererException;

  /**
   * Provides a mechanism for executing blocking IO operations.
   *
   * @see Blocking
   * @return A new instance of {@link Blocking}
   */
  Blocking getBlocking();

  /**
   * Sends a temporary redirect response (i.e. statusCode 302) to the client using the specified redirect location URL.
   *
   * @param location the redirect location URL
   * @throws NotInRegistryException if there is no {@link Redirector} in the current service but one is provided by default
   */
  void redirect(String location) throws NotInRegistryException;

  /**
   * Sends a redirect response location URL and status code (which should be in the 3xx range).
   *
   * @param code The status code of the redirect
   * @param location the redirect location URL
   * @throws NotInRegistryException if there is no {@link Redirector} in the current service but one is provided by default
   */
  void redirect(int code, String location) throws NotInRegistryException;

  /**
   * Convenience method for handling last-modified based HTTP caching.
   * <p>
   * The given date is the "last modified" value of the response.
   * If the client sent an "If-Modified-Since" header that is of equal or greater value than date, a 304
   * will be returned to the client. Otherwise, the given runnable will be executed (it should send a response)
   * and the "Last-Modified" header will be set by this method.
   *
   * @param date The effective last modified date of the response
   * @param runnable The response sending action if the response needs to be sent
   */
  @NonBlocking
  void lastModified(Date date, Runnable runnable);

}
