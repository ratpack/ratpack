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

package ratpack.groovy.test.handling;

import com.google.common.net.HostAndPort;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.test.handling.internal.DefaultGroovyRequestFixture;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.registry.RegistryBuilder;
import ratpack.registry.RegistrySpec;
import ratpack.server.ServerConfigBuilder;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.RequestFixture;
import ratpack.test.http.MultipartFileSpec;
import ratpack.test.http.MultipartFormSpec;

import java.util.Map;

/**
 * A more Groovy friendly version of {@link RequestFixture}.
 */
public interface GroovyRequestFixture extends RequestFixture {

  /**
   * Unit test a {@link ratpack.handling.Handler}.
   * <p>
   * Example:
   * <pre class="tested">
   * import ratpack.groovy.handling.GroovyHandler
   * import ratpack.groovy.handling.GroovyContext
   * import ratpack.groovy.test.handling.GroovyRequestFixture
   *
   * class MyHandler extends GroovyHandler {
   *   void handle(GroovyContext context) {
   *     context.with {
   *       def outputHeaderValue = request.headers.get("input-value") + ":bar"
   *       response.headers.set("output-value", outputHeaderValue)
   *       render "received: " + request.path
   *     }
   *   }
   * }
   *
   * def result = GroovyRequestFixture.handle(new MyHandler()) {
   *   header "input-value", "foo"
   *   uri "some/path"
   * }
   *
   * assert result.rendered(String) == "received: some/path"
   * assert result.headers.get("output-value") == "foo:bar"
   * </pre>
   *
   * @param handler the handler to test
   * @param closure the configuration of the request fixture
   * @return The result of the invocation
   * @throws ratpack.test.handling.HandlerTimeoutException if the handler takes more than {@link RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code closure}
   */
  @SuppressWarnings("overloads")
  static HandlingResult handle(Handler handler, @DelegatesTo(GroovyRequestFixture.class) final Closure<?> closure) throws Exception {
    return RequestFixture.handle(handler, builder -> {
      GroovyRequestFixture groovyBuilder = new DefaultGroovyRequestFixture(builder);
      ClosureUtil.configureDelegateFirst(groovyBuilder, closure);
    });
  }

  /**
   * Unit test a chain of {@link Handler handlers}.
   * <p>
   * Example:
   * <pre class="tested">{@code
   * import ratpack.groovy.test.handling.GroovyRequestFixture
   * import ratpack.groovy.Groovy
   *
   * def handlers = Groovy.chain {
   *   all {
   *     def outputHeaderValue = request.headers.get("input-value") + ":bar"
   *     response.headers.set("output-value", outputHeaderValue)
   *     next()
   *   }
   *   all {
   *     render "received: " + request.path
   *   }
   * }
   *
   * def result = GroovyRequestFixture.handle(handlers) {
   *   header "input-value", "foo"
   *   uri "some/path"
   * }
   *
   * assert result.rendered(String) == "received: some/path"
   * assert result.headers.get("output-value") == "foo:bar"
   * }</pre>
   *
   * @param handlers the handlers to test
   * @param closure the configuration of the request fixture
   * @return The result of the invocation
   * @throws ratpack.test.handling.HandlerTimeoutException if the handler takes more than {@link RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code closure}
   */
  @SuppressWarnings("overloads")
  static HandlingResult handle(Action<? super Chain> handlers, @DelegatesTo(GroovyRequestFixture.class) final Closure<?> closure) throws Exception {
    return RequestFixture.handle(handlers, builder -> {
      GroovyRequestFixture groovyBuilder = new DefaultGroovyRequestFixture(builder);
      ClosureUtil.configureDelegateFirst(groovyBuilder, closure);
    });
  }

  /**
   * Create a Groovy request fixture, for unit testing a {@link Handler}.
   *
   * @return a Groovy request fixture
   */
  static GroovyRequestFixture requestFixture() {
    return requestFixture(RequestFixture.requestFixture());
  }

  /**
   * Create a Groovy request fixture, for unit testing a {@link Handler}, by wrapping the given {@link RequestFixture}.
   *
   * @param requestFixture The request fixture to wrap
   * @return a Groovy request fixture
   */
  static GroovyRequestFixture requestFixture(RequestFixture requestFixture) {
    return new DefaultGroovyRequestFixture(requestFixture);
  }

  /**
   * A closure friendly overload of {@link #registry(Action)}.
   *
   * @param closure the registry configuration
   * @return this
   * @see #registry(Action)
   */
  GroovyRequestFixture registry(@DelegatesTo(value = RegistryBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture header(CharSequence name, String value);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture body(byte[] bytes, String contentType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture body(String text, String contentType);

  /**
   * {@inheritDoc}
   */
  @Override
  MultipartFileSpec file();

  /**
   * {@inheritDoc}
   */
  @Override
  RequestFixture file(String field, String filename, String data);

  /**
   * {@inheritDoc}
   */
  @Override
  MultipartFormSpec form();

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture form(Map<String, String> fields);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture responseHeader(CharSequence name, String value);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture method(String method);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture uri(String uri);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture timeout(int timeoutSeconds);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture registry(Action<? super RegistrySpec> action) throws Exception;

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture pathBinding(Map<String, String> pathTokens);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture pathBinding(String boundTo, String pastBinding, Map<String, String> pathTokens, String description);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture serverConfig(Action<? super ServerConfigBuilder> action) throws Exception;

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture remoteAddress(HostAndPort remote);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyRequestFixture localAddress(HostAndPort local);

}
