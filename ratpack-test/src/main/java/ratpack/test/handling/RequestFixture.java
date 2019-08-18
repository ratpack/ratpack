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

import com.google.common.net.HostAndPort;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.registry.RegistrySpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.test.handling.internal.DefaultRequestFixture;
import ratpack.test.http.MultipartFileSpec;
import ratpack.test.http.MultipartFormSpec;

import java.util.Map;

/**
 * A contrived request environment, suitable for unit testing {@link Handler} implementations.
 * <p>
 * A request fixture emulates a request, <b>and</b> the effective state of the request handling in the handler pipeline.
 * <p>
 * A request fixture can be obtained by the {@link RequestFixture#requestFixture()} method.
 * However it is often more convenient to use the alternative {@link RequestFixture#handle(Handler, Action)} method.
 *
 * @see #handle(Handler)
 */
public interface RequestFixture {

  /**
   * Unit test a single {@link Handler}.
   *
   * <pre class="java">{@code
   * import ratpack.handling.Context;
   * import ratpack.handling.Handler;
   * import ratpack.test.handling.RequestFixture;
   * import ratpack.test.handling.HandlingResult;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static class MyHandler implements Handler {
   *     public void handle(Context ctx) throws Exception {
   *       String outputHeaderValue = ctx.getRequest().getHeaders().get("input-value") + ":bar";
   *       ctx.getResponse().getHeaders().set("output-value", outputHeaderValue);
   *       ctx.render("received: " + ctx.getRequest().getPath());
   *     }
   *   }
   *
   *   public static void main(String[] args) throws Exception {
   *     HandlingResult result = RequestFixture.handle(new MyHandler(), fixture ->
   *         fixture.header("input-value", "foo").uri("some/path")
   *     );
   *
   *     assertEquals("received: some/path", result.rendered(String.class));
   *     assertEquals("foo:bar", result.getHeaders().get("output-value"));
   *   }
   * }
   * }</pre>
   *
   * @param handler The handler to invoke
   * @param action The configuration of the context for the handler
   * @return A result object indicating what happened
   * @throws HandlerTimeoutException if the handler takes more than {@link RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code action}
   * @see #handle(Action, Action)
   */
  @SuppressWarnings("overloads")
  static HandlingResult handle(Handler handler, Action<? super RequestFixture> action) throws Exception {
    RequestFixture requestFixture = requestFixture();
    action.execute(requestFixture);
    return requestFixture.handle(handler);
  }

  /**
   * Unit test a {@link Handler} chain.
   *
   * <pre class="java">{@code
   * import ratpack.func.Action;
   * import ratpack.handling.Chain;
   * import ratpack.test.handling.RequestFixture;
   * import ratpack.test.handling.HandlingResult;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static class MyHandlers implements Action<Chain> {
   *     public void execute(Chain chain) throws Exception {
   *       chain.all(ctx -> {
   *         String outputHeaderValue = ctx.getRequest().getHeaders().get("input-value") + ":bar";
   *         ctx.getResponse().getHeaders().set("output-value", outputHeaderValue);
   *         ctx.next();
   *       });
   *       chain.all(ctx -> ctx.render("received: " + ctx.getRequest().getPath()) );
   *     }
   *   }
   *
   *   public static void main(String[] args) throws Exception {
   *     HandlingResult result = RequestFixture.handle(new MyHandlers(), fixture ->
   *         fixture.header("input-value", "foo").uri("some/path")
   *     );
   *
   *     assertEquals("received: some/path", result.rendered(String.class));
   *     assertEquals("foo:bar", result.getHeaders().get("output-value"));
   *   }
   * }
   * }</pre>
   *
   * @param chainAction the definition of a handler chain to test
   * @param requestFixtureAction the configuration of the request fixture
   * @return a result object indicating what happened
   * @throws HandlerTimeoutException if the handler takes more than {@link RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code chainAction} or {@code requestFixtureAction}
   * @see #handle(Handler, Action)
   */
  @SuppressWarnings("overloads")
  static HandlingResult handle(Action<? super Chain> chainAction, Action<? super RequestFixture> requestFixtureAction) throws Exception {
    RequestFixture requestFixture = requestFixture();
    requestFixtureAction.execute(requestFixture);
    return requestFixture.handleChain(chainAction);
  }

  /**
   * Create a request fixture, for unit testing of {@link Handler handlers}.
   *
   * @see #handle(Handler, Action)
   * @see #handle(Action, Action)
   * @return a request fixture
   */
  static RequestFixture requestFixture() {
    return new DefaultRequestFixture();
  }

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
   * A specification of a file to upload (see RFC2388)
   * <p>
   * Can be used to construct a multipart form with files
   *
   * @return a specification of a multipart file
   */
  MultipartFileSpec file();

  /**
   * Uploads a file via a multipart form (see RFC2388)
   *
   * @param field form field name
   * @param filename filename of uploaded file
   * @param data content of file
   * @return this
   */
  RequestFixture file(String field, String filename, String data);

  /**
   * A specification of a multipart form (see RFC2388)
   * <p>
   * Can be used to construct a multipart form with name value pairs and files
   * <p>
   * Note that more than one value and more than one file can be associated with a single field
   *
   * @return a specification of a multipart form
   */
  MultipartFormSpec form();

  /**
   * Sets the fields on a multipart form (see RFC2388)
   *
   * @param fields map of field name to field value
   * @return this
   */
  RequestFixture form(Map<String, String> fields);

  /**
   * A specification of the context registry.
   * <p>
   * Can be used to make objects (e.g. support services) available via context registry lookup.
   * <p>
   * By default, only a {@link ServerErrorHandler} and {@link ClientErrorHandler} are in the context registry.
   *
   * @return a specification of the context registry
   */
  RegistrySpec getRegistry();

  /**
   * Invokes the given handler with a newly created {@link Context} based on the state of this fixture.
   * <p>
   * The return value can be used to examine the effective result of the handler.
   * <p>
   * A result may be one of the following:
   * <ul>
   * <li>The sending of a response via one of the {@link Response#send} methods</li>
   * <li>Rendering to the response via the {@link Context#render(Object)}</li>
   * <li>Raising of a client error via {@link Context#clientError(int)}</li>
   * <li>Raising of a server error via {@link Context#error(Throwable)}</li>
   * <li>Raising of a server error by the throwing of an exception</li>
   * <li>Delegating to the next handler by invoking one of the {@link Context#next} methods</li>
   * </ul>
   * Note that any handlers <i>{@link Context#insert inserted}</i> by the handler under test will be invoked.
   * If the last inserted handler delegates to the next handler, the handling will terminate with a result indicating that the effective result was delegating to the next handler.
   * <p>
   * This method blocks until a result is achieved, even if the handler performs an asynchronous operation (such as performing {@link ratpack.exec.Blocking#get(ratpack.func.Factory) blocking IO}).
   * As such, a time limit on the execution is imposed which by default is 5 seconds.
   * The time limit can be changed via the {@link #timeout(int)} method.
   * If the time limit is reached, a {@link HandlerTimeoutException} is thrown.
   *
   * @param handler the handler to test
   * @return the effective result of the handling
   * @throws HandlerTimeoutException if the handler does not produce a result in the time limit defined by this fixture
   */
  HandlingResult handle(Handler handler) throws HandlerTimeoutException;

  /**
   * Similar to {@link #handle(Handler)}, but for testing a handler chain.
   *
   * @param chainAction the handler chain to test
   * @return the effective result of the handling
   * @throws HandlerTimeoutException if the handler does not produce a result in the time limit defined by this fixture
   * @throws Exception any thrown by {@code chainAction}
   */
  HandlingResult handleChain(Action<? super Chain> chainAction) throws Exception;

  /**
   * Set a request header value.
   * <p>
   * By default there are no request headers.
   *
   * @param name the header name
   * @param value the header value
   * @return this
   */
  RequestFixture header(CharSequence name, String value);

  /**
   * Configures the server config to have no base dir and given configuration.
   * <p>
   * By default the server config is equivalent to {@link ServerConfig#builder() ServerConfig.builder()}.{@link ServerConfigBuilder#build() build()}.
   *
   * @param action configuration of the server config
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  RequestFixture serverConfig(Action<? super ServerConfigBuilder> action) throws Exception;

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
   * Adds a path binding, with the given path tokens and parts.
   * <p>
   * By default, there are no path tokens and no path binding.
   *
   * @param boundTo the part of the request path that the binding bound to
   * @param pastBinding the part of the request path past {@code boundTo}
   * @param pathTokens the path tokens and binding to make available to the handler(s) under test
   * @param description the description of the request path binding
   * @return this
   */
  RequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens, String description);

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
  RequestFixture responseHeader(CharSequence name, String value);

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

  /**
   * Set the remote address from which the request is made.
   * <p>
   * Effectively the return value of {@link Request#getRemoteAddress()}.

   * @param remote the remote host and port address
   * @return this
   */
  RequestFixture remoteAddress(HostAndPort remote);

  /**
   * Set the local address to which this request is made.
   * <p>
   * Effectively the return value of {@link Request#getLocalAddress()}.
   *
   * @param local the local host and port address
   * @return this
   */
  RequestFixture localAddress(HostAndPort local);

  /**
   * Set the HTTP protocol for the request.
   *
   * @param protocol The string representation of the HTTP protocol.
   * @return this
   */
  RequestFixture protocol(String protocol);

}
