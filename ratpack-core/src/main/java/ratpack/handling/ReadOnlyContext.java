/*
 * Copyright 2014 the original author or authors.
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
import ratpack.func.Action;
import ratpack.http.Request;
import ratpack.http.client.HttpClient;
import ratpack.path.PathTokens;
import ratpack.promise.Fulfiller;
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.server.BindAddress;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * A context that does not provide access to the {@link ratpack.http.Response} or flow control methods.
 */
public interface ReadOnlyContext extends Registry {

  /**
   * Returns this.
   *
   * @return this.
   */
  ReadOnlyContext getContext();

  /**
   * The HTTP request.
   *
   * @return The HTTP request.
   */
  Request getRequest();

  /**
   * Forwards the exception to the {@link ratpack.error.ServerErrorHandler} in this service.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.error.ServerErrorHandler} in all contexts.
   * A {@link NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   * @param exception The exception that occurred
   * @throws NotInRegistryException if no {@link ratpack.error.ServerErrorHandler} can be found in the service
   */
  @NonBlocking
  void error(Exception exception) throws NotInRegistryException;

  /**
   * The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * <p>
   * Shorthand for {@code get(PathBinding.class).getPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link ratpack.path.PathBinding} in the current service
   */
  PathTokens getPathTokens() throws NotInRegistryException;

  /**
   * The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * <p>
   * Shorthand for {@code get(PathBinding.class).getAllPathTokens()}.
   *
   * @return The contextual path tokens of the current {@link ratpack.path.PathBinding}.
   * @throws NotInRegistryException if there is no {@link ratpack.path.PathBinding} in the current service
   */
  PathTokens getAllPathTokens() throws NotInRegistryException;

  /**
   * Gets the file relative to the contextual {@link ratpack.file.FileSystemBinding}.
   * <p>
   * Shorthand for {@code get(FileSystemBinding.class).file(path)}.
   * <p>
   * The default configuration of Ratpack includes a {@link ratpack.file.FileSystemBinding} in all contexts.
   * A {@link NotInRegistryException} will only be thrown if a very custom service setup is being used.
   *
   *
   * @param path The path to pass to the {@link ratpack.file.FileSystemBinding#file(String)} method.
   * @return The file relative to the contextual {@link ratpack.file.FileSystemBinding}
   * @throws NotInRegistryException if there is no {@link ratpack.file.FileSystemBinding} in the current service
   */
  Path file(String path) throws NotInRegistryException;

  /**
   * An object to be used when executing blocking IO, or long operations.
   *
   * @return An object to be used when executing blocking IO, or long operations.
   * @see #background(Callable)
   */
  Background getBackground();

  /**
   * The application foreground.
   *
   * @return the application foreground
   * @see ratpack.handling.Foreground
   */
  Foreground getForeground();

  /**
   * Performs the given {@code callable} in a non request thread so that it can perform blocking IO.
   * <p>
   * This method is merely a convenience for calling {@link #getBackground() getBackground()}.{@link Background#exec(Callable) exec(callable)}.
   *
   * @param backgroundOperation The blocking operation to perform in the background
   * @param <T> The type of object returned by the background operation
   * @return A promise for the result of the background operation
   * @see Background
   */
  <T> SuccessOrErrorPromise<T> background(Callable<T> backgroundOperation);

  /**
   * Creates a promise of a value that will made available asynchronously.
   * <p>
   * The {@code action} given to this method receives a {@link Fulfiller}, which can be used to fulfill the promise at any time in the future.
   * The {@code action} is not required to fulfill the promise during the execution of the {@code execute()} method (i.e. it can be asynchronous).
   * The {@code action} MUST call one of the fulfillment methods.
   * Otherwise, the promise will go unfulfilled.
   * There is no time limit or timeout on fulfillment.
   * <p>
   * The promise returned has a default error handling strategy of forwarding exceptions to {@link #error(Exception)} of this context.
   * To use a different error strategy, supply it to the {@link SuccessOrErrorPromise#onError(Action)} method.
   * <p>
   * <pre class="tested">
   * import ratpack.handling.*;
   * import ratpack.promise.Fulfiller;
   * import ratpack.func.Action;
   *
   * import java.util.concurrent.TimeUnit;
   *
   * public class PromiseUsingJavaHandler implements Handler {
   *   public void handle(final Context context) {
   *     context.promise(new Action&lt;Fulfiller&lt;String&gt;&gt;() {
   *       public void execute(final Fulfiller&lt;String&gt; fulfiller) {
   *         context.getForeground().getExecutor().schedule(new Runnable() {
   *           public void run() {
   *             fulfiller.success("hello world!");
   *           }
   *         }, 200, TimeUnit.MILLISECONDS);
   *       }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String string) {
   *         context.render(string);
   *       }
   *     });
   *   }
   * }
   *
   * class PromiseUsingGroovyHandler implements Handler {
   *   void handle(Context context) {
   *     context.promise { Fulfiller&lt;String&gt; fulfiller ->
   *       context.foreground.executor.schedule({
   *         fulfiller.success("hello world!")
   *       }, 200, TimeUnit.MILLISECONDS)
   *     } then { String string ->
   *       context.render(string)
   *     }
   *   }
   * }
   *
   * // Test (Groovy) &hellip;
   *
   * import ratpack.test.embed.PathBaseDirBuilder
   * import ratpack.groovy.test.TestHttpClients
   * import ratpack.groovy.test.embed.ClosureBackedEmbeddedApplication
   *
   * def baseDir = new PathBaseDirBuilder(new File("some/path"))
   * def app = new ClosureBackedEmbeddedApplication(baseDir)
   *
   * app.handlers {
   *   get("java", new PromiseUsingJavaHandler())
   *   get("groovy", new PromiseUsingGroovyHandler())
   * }
   *
   * def client = TestHttpClients.testHttpClient(app)
   *
   * assert client.getText("java") == "hello world!"
   * assert client.getText("groovy") == "hello world!"
   *
   * app.close()
   * </pre>
   * @param <T> the type of value promised
   */
  <T> SuccessOrErrorPromise<T> promise(Action<? super Fulfiller<T>> action);

  /**
   * The address that this request was received on.
   *
   * @return The address that this request was received on.
   */
  BindAddress getBindAddress();

  /**
   * Registers a callback to be notified when the request for this context is “closed” (i.e. responded to).
   *
   * @param onClose A notification callback
   */
  void onClose(Action<? super RequestOutcome> onClose);

  HttpClient getHttpClient();

}
