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

package ratpack.test.handling;

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.registry.RegistrySpec;

import java.nio.file.Path;
import java.util.Map;

/**
 * A contrived request environment, suitable for unit testing {@link Handler} implementations.
 * <p>
 * A request fixture emulates a request, <b>and</b> the effective state of the request handling in the handler pipeline.
 * <p>
 * A request fixture can be obtained by the {@link ratpack.test.UnitTest#requestFixture()} method.
 * However it is often more convenient to use the alternative {@link ratpack.test.UnitTest#handle(ratpack.handling.Handler, ratpack.func.Action)} method.
 * <p>
 * See {@link ratpack.test.UnitTest} for usage examples.
 *
 * @see ratpack.test.UnitTest
 * @see #handle(ratpack.handling.Handler)
 */
public interface RequestFixture {

  /**
   * Sets the request body to be the given bytes, and adds a {@code Content-Type} request header of the given value.
   * <p>
   * By default the body is empty.
   *
   * @param bytes the request body in bytes
   * @param contentType the content type of the request body
   * @return this
   */
  RequestFixture body(byte[] bytes, String contentType);

  /**
   * Sets the request body to be the given string in utf8 bytes, and adds a {@code Content-Type} request header of the given value.
   * <p>
   * By default the body is empty.
   *
   * @param text the request body as a string
   * @param contentType the content type of the request body
   * @return this
   */
  RequestFixture body(String text, String contentType);

  /**
   * A specification of the context registry.
   * <p>
   * Can be used to make objects (e.g. support services) available via context registry lookup.
   * <p>
   * By default, only a {@link ratpack.error.ServerErrorHandler} and {@link ratpack.error.ClientErrorHandler} are in the context registry.
   *
   * @return a specification of the context registry
   */
  RegistrySpec getRegistry();

  /**
   * Invokes the given handler with a newly created {@link ratpack.handling.Context} based on the state of this fixture.
   * <p>
   * The return value can be used to examine the effective result of the handler.
   * <p>
   * A result may be one of the following:
   * <ul>
   * <li>The sending of a response via one of the {@link ratpack.http.Response#send} methods</li>
   * <li>Rendering to the response via the {@link ratpack.handling.Context#render(Object)}</li>
   * <li>Raising of a client error via {@link ratpack.handling.Context#clientError(int)}</li>
   * <li>Raising of a server error via {@link ratpack.handling.Context#error(Throwable)}</li>
   * <li>Raising of a server error by the throwing of an exception</li>
   * <li>Delegating to the next handler by invoking one of the {@link ratpack.handling.Context#next} methods</li>
   * </ul>
   * Note that any handlers <i>{@link ratpack.handling.Context#insert inserted}</i> by the handler under test will be invoked.
   * If the last inserted handler delegates to the next handler, the handling will terminate with a result indicating that the effective result was delegating to the next handler.
   * <p>
   * This method blocks until a result is achieved, even if the handler performs an asynchronous operation (such as performing {@link ratpack.handling.Context#blocking(java.util.concurrent.Callable) blocking IO}).
   * As such, a time limit on the execution is imposed which by default is 5 seconds.
   * The time limit can be changed via the {@link #timeout(int)} method.
   * If the time limit is reached, a {@link HandlerTimeoutException} is thrown.
   *
   * @param handler the handler to test
   * @return the effective result of the handling
   * @throws HandlerTimeoutException if the handler does not produce a result in the time limit defined by this fixture
   */
  @SuppressWarnings("overloads")
  HandlingResult handle(Handler handler) throws HandlerTimeoutException;

  /**
   * Similar to {@link #handle(ratpack.handling.Handler)}, but for testing a handler chain.
   *
   * @param chainAction the handler chain to test
   * @return the effective result of the handling
   * @throws HandlerTimeoutException if the handler does not produce a result in the time limit defined by this fixture
   * @throws Exception any thrown by {@code chainAction}
   */
  @SuppressWarnings("overloads")
  HandlingResult handle(Action<? super Chain> chainAction) throws Exception;

  /**
   * Set a request header value.
   * <p>
   * By default there are no request headers.
   *
   * @param name the header name
   * @param value the header value
   * @return this
   */
  RequestFixture header(String name, String value);

  /**
   * Configures the launch config to have the given base dir and given configuration.
   * <p>
   * By default the launch config is equivalent to {@link ratpack.launch.LaunchConfigBuilder#noBaseDir() LaunchConfigBuilder.noBaseDir()}.{@link ratpack.launch.LaunchConfigBuilder#build() build()}.
   *
   * @param baseDir the launch config base dir
   * @param action configuration of the launch config
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  RequestFixture launchConfig(Path baseDir, Action<? super LaunchConfigBuilder> action) throws Exception;

  /**
   * Configures the launch config to have no base dir and given configuration.
   * <p>
   * By default the launch config is equivalent to {@link ratpack.launch.LaunchConfigBuilder#noBaseDir() LaunchConfigBuilder.noBaseDir()}.{@link ratpack.launch.LaunchConfigBuilder#build() build()}.
   *
   * @param action configuration of the launch config
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  RequestFixture launchConfig(Action<? super LaunchConfigBuilder> action) throws Exception;

  /**
   * Set the request method (case insensitive).
   * <p>
   * The default method is {@code "GET"}.
   *
   * @param method the request method
   * @return this
   */
  RequestFixture method(String method);

  /**
   * Adds a path binding, with the given path tokens.
   * <p>
   * By default, there are no path tokens and no path binding.
   *
   * @param pathTokens the path tokens to make available to the handler(s) under test
   * @return this
   */
  RequestFixture pathBinding(Map<String, String> pathTokens);

  /**
   * Adds a path binding, with the given path tokens and parts.
   * <p>
   * By default, there are no path tokens and no path binding.
   *
   * @param boundTo the part of the request path that the binding bound to
   * @param pastBinding the part of the request path past {@code boundTo}
   * @param pathTokens the path tokens and binding to make available to the handler(s) under test
   * @return this
   */
  RequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens);

  /**
   * Configures the context registry.
   *
   * @param action a registry specification action
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  RequestFixture registry(Action<? super RegistrySpec> action) throws Exception;

  /**
   * Set a response header value.
   * <p>
   * Can be used to simulate the setting of a response header by an upstream handler.
   * <p>
   * By default there are no request headers.
   *
   * @param name the header name
   * @param value the header value
   * @return this
   */
  RequestFixture responseHeader(String name, String value);

  /**
   * Sets the maximum time to allow the handler under test to produce a result.
   * <p>
   * As handlers may execute asynchronously, a maximum time limit must be used to guard against never ending handlers.
   *
   * @param timeoutSeconds the maximum number of seconds to allow the handler(s) under test to produce a result
   * @return this
   */
  RequestFixture timeout(int timeoutSeconds);

  /**
   * The URI of the request.
   * <p>
   * No encoding is performed on the given value.
   * It is expected to be a well formed URI path string (potentially including query and fragment strings)
   *
   * @param uri the URI of the request
   * @return this
   */
  RequestFixture uri(String uri);

}
