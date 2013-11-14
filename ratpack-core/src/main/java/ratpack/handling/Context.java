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

package ratpack.handling;

import ratpack.api.NonBlocking;
import ratpack.block.Blocking;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.parse.Parse;
import ratpack.path.PathTokens;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.render.NoSuchRendererException;
import ratpack.server.BindAddress;
import ratpack.util.Action;
import ratpack.util.Factory;
import ratpack.util.ResultAction;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

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
 * For example, error handling is based on informing the contextual {@link ratpack.error.ServerErrorHandler} of exceptions.
 * The error handling strategy for an application can be changed by pushing a new implementation of this interface into the context that is used downstream.
 * <p>
 * See {@link #insert(java.util.List)} for more on how to do this.
 * <h5>Default contextual objects</h5>
 * <p>There is also a set of default objects that are made available via the Ratpack infrastructure:
 * <ul>
 * <li>The effective {@link ratpack.launch.LaunchConfig}</li>
 * <li>A {@link ratpack.file.FileSystemBinding} that is the application {@link ratpack.launch.LaunchConfig#getBaseDir()}</li>
 * <li>A {@link ratpack.file.MimeTypes} implementation</li>
 * <li>A {@link ratpack.error.ServerErrorHandler}</li>
 * <li>A {@link ratpack.error.ClientErrorHandler}</li>
 * <li>A {@link ratpack.file.FileRenderer}</li>
 * <li>A {@link ratpack.server.BindAddress}</li>
 * <li>A {@link ratpack.server.PublicAddress}</li>
 * <li>A {@link Redirector}</li>
 * </ul>
 */
@SuppressWarnings("UnusedDeclaration")
public interface Context extends Registry {

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
   * @param handlers The handlers to insert
   * @param registry The registry for the inserted handlers
   */
  @NonBlocking
  void insert(List<Handler> handlers, Registry registry);

  /**
   * Inserts some handlers into the pipeline to execute with the given object created by the factory made available, then delegates to the first.
   * <p>
   * The given object will take precedence over an existing contextual object advertised by the given advertised type.
   * <p>
   * The object will only be retrievable by the type that is given and will be created on demand (once) from the factory.
   *
   * @param handlers The handlers to insert
   * @param publicType The advertised type of the object (i.e. what it is retrievable by)
   * @param factory The factory that creates the object lazily
   */
  @NonBlocking
  <T> void insert(List<Handler> handlers, Class<T> publicType, Factory<? extends T> factory);

  /**
   * Inserts some handlers into the pipeline to execute with the given object made available, then delegates to the first.
   * <p>
   * The given object will take precedence over an existing contextual object advertised by the given advertised type.
   * <p>
   * The object will only be retrievable by the type that is given.
   *
   * @param handlers The handlers to insert
   * @param publicType The advertised type of the object (i.e. what it is retrievable by)
   * @param implementation The actual implementation
   */
  @NonBlocking
  <P, T extends P> void insert(List<Handler> handlers, Class<P> publicType, T implementation);

  /**
   * Inserts some handlers into the pipeline to execute with the the given object added to the service, then delegates to the first.
   * <p>
   * The given object will take precedence over any existing object available via its concrete type.
   *
   * @param handlers The handlers to insert
   * @param object The object to add to the service for the handlers
   */
  @NonBlocking
  void insert(List<Handler> handlers, Object object);

  /**
   * Convenience method for delegating to a single handler.
   * <p>
   * Designed to be used in conjunction with the {@link #getByMethod()} and {@link #getByContent()} methods.
   *
   * @param handler The handler to invoke
   * @see ByContentHandler
   * @see ByMethodHandler
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
   * Forwards the exception to the {@link ratpack.error.ServerErrorHandler} in this service.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.error.ServerErrorHandler} in all contexts.
   * A {@link ratpack.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param exception The exception that occurred
   * @throws NotInRegistryException if no {@link ratpack.error.ServerErrorHandler} can be found in the service
   */
  @NonBlocking
  void error(Exception exception) throws NotInRegistryException;

  /**
   * Forwards the error to the {@link ratpack.error.ClientErrorHandler} in this service.
   *
   * The default configuration of Ratpack includes a {@link ratpack.error.ClientErrorHandler} in all contexts.
   * A {@link ratpack.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param statusCode The 4xx range status code that indicates the error type
   * @throws NotInRegistryException if no {@link ratpack.error.ClientErrorHandler} can be found in the service
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
   * The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(PathBinding.class).getPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link ratpack.path.PathBinding} in the current service
   */
  PathTokens getPathTokens() throws NotInRegistryException;

  /**
   * The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(PathBinding.class).getAllPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link ratpack.path.PathBinding} in the current service
   */
  PathTokens getAllPathTokens() throws NotInRegistryException;

  /**
   * Gets the file relative to the contextual {@link ratpack.file.FileSystemBinding}.
   * <p>
   * Shorthand for {@code getServiceRegistry().get(FileSystemBinding.class).file(path)}.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.file.FileSystemBinding} in all contexts.
   * A {@link ratpack.registry.NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param path The path to pass to the {@link ratpack.file.FileSystemBinding#file(String)} method.
   * @return The file relative to the contextual {@link ratpack.file.FileSystemBinding}
   * @throws NotInRegistryException if there is no {@link ratpack.file.FileSystemBinding} in the current service
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
   * See {@link ratpack.render.Renderer} for more on the rendering framework.
   *
   * @param object The object to render
   * @throws NoSuchRendererException If there is no suitable renderer for the object
   */
  @NonBlocking
  void render(Object object) throws NoSuchRendererException;

  /**
   * An object to be used when executing blocking IO, or long operations.
   * <p>
   * Ratpack apps typically do not use a large thread pool for handling requests. By default there is about one thread per core.
   * This means that blocking IO operations cannot be done on the thread invokes a handler. Blocking IO operations must be
   * offloaded in order to free the request handling thread to handle other requests while the IO operation is performed.
   * The {@code Blocking} object makes it easy to do this.
   * <p>
   * A callable is submitted to the {@link Blocking#exec(Callable)} method. The implementation of this callable <b>can</b> block
   * as it will be executed on a non request handling thread. It should do not much more than initiate a blocking IO operation and return the result.
   * <p>
   * However, the callable is not executed immediately. The return value of {@link Blocking#exec(Callable)} must be used to specify
   * how to proceed after the blocking operation. The {@code then()} method must be called for the work to be performed.
   * </p>
   * Example usage (Java):
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.util.Action;
   * import java.util.concurrent.Callable;
   *
   * class MyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.getBlocking().exec(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "foo";
   *        }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.getResponse().send(result);
   *       }
   *     });
   *   }
   * }
   * </pre>
   *
   * <h4>Error Handling</h4>
   * <p>
   * Unless otherwise specified, any exceptions that are raised during the blocking operation callable are forwarded
   * to the {@link ratpack.handling.Context#error(Exception)} method of the current context.
   * Similarly, errors that occur during the result handler are forwarded.
   * </p>
   * <p>
   * To use a custom error handling strategy, use the {@link ratpack.block.Blocking.SuccessOrError#onError(Action)} method
   * of the return of {@link Blocking#exec(Callable)}.
   * </p>
   * <p>
   * Example usage:
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.util.Action;
   * import java.util.concurrent.Callable;
   *
   * class MyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.blocking(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "foo";
   *        }
   *     }).onError(new Action&lt;Exception&gt;() {
   *       public void execute(Exception exception) {
   *         // do something with the exception
   *       }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.getResponse().send(result);
   *       }
   *     });
   *   }
   * }
   * </pre>
   */
  Blocking getBlocking();

  /**
   * Shorthand for {@code getBlocking().exec(blockingOperation)}.
   *
   * @return A builder for specifying the result handling strategy for a blocking operation.
   * @see #getBlocking()
   */
  <T> Blocking.SuccessOrError<T> blocking(Callable<T> blockingOperation);

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

  /**
   * The address that this request was received on.
   *
   * @return The address that this request was received on.
   */
  BindAddress getBindAddress();

  <T> T parse(Parse<T> parse);
}
