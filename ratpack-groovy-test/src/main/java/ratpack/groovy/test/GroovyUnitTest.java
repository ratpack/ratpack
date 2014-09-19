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

package ratpack.groovy.test;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.test.handling.GroovyRequestFixture;
import ratpack.groovy.test.handling.internal.DefaultGroovyRequestFixture;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.test.UnitTest;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.RequestFixture;

/**
 * Static, Groovy friendly, methods for the unit testing of handlers.
 */
public abstract class GroovyUnitTest {

  private GroovyUnitTest() {
  }

  /**
   * Unit test a {@link Handler}.
   * <p>
   * Example:
   * <pre class="tested">
   * import ratpack.groovy.handling.GroovyHandler
   * import ratpack.groovy.handling.GroovyContext
   * import ratpack.groovy.test.GroovyUnitTest
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
   * def result = GroovyUnitTest.handle(new MyHandler()) {
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
   * @throws HandlerTimeoutException if the handler takes more than {@link RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code closure}
   */
  @SuppressWarnings("overloads")
  public static HandlingResult handle(Handler handler, @DelegatesTo(GroovyRequestFixture.class) final Closure<?> closure) throws Exception {
    return UnitTest.handle(handler, builder -> {
      GroovyRequestFixture groovyBuilder = new DefaultGroovyRequestFixture(builder);
      ClosureUtil.configureDelegateFirst(groovyBuilder, closure);
    });
  }


  /**
   * Unit test a chain of {@link Handler handlers}.
   * <p>
   * Example:
   * <pre class="tested">
   * import ratpack.groovy.handling.GroovyChainAction
   * import ratpack.groovy.test.GroovyUnitTest
   *
   * class MyHandlers extends GroovyChainAction {
   *   protected void execute() {
   *     handler {
   *       def outputHeaderValue = request.headers.get("input-value") + ":bar"
   *       response.headers.set("output-value", outputHeaderValue)
   *       next()
   *     }
   *     handler {
   *       render "received: " + request.path
   *     }
   *   }
   * }
   *
   * def result = GroovyUnitTest.handle(new MyHandlers()) {
   *   header "input-value", "foo"
   *   uri "some/path"
   * }
   *
   * assert result.rendered(String) == "received: some/path"
   * assert result.headers.get("output-value") == "foo:bar"
   * </pre>
   *
   * @param handlers the handlers to test
   * @param closure the configuration of the request fixture
   * @return The result of the invocation
   * @throws HandlerTimeoutException if the handler takes more than {@link RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code closure}
   */
  @SuppressWarnings("overloads")
  public static HandlingResult handle(Action<? super Chain> handlers, @DelegatesTo(GroovyRequestFixture.class) final Closure<?> closure) throws Exception {
    return UnitTest.handle(handlers, builder -> {
      GroovyRequestFixture groovyBuilder = new DefaultGroovyRequestFixture(builder);
      ClosureUtil.configureDelegateFirst(groovyBuilder, closure);
    });
  }

  /**
   * Create a Groovy request fixture, for unit testing a {@link Handler}.
   *
   * @return a Groovy request fixture
   */
  public static GroovyRequestFixture requestFixture() {
    return requestFixture(UnitTest.requestFixture());
  }

  /**
   * Create a Groovy request fixture, for unit testing a {@link Handler}, by wrapping the given {@link RequestFixture}.
   *
   * @param requestFixture The request fixture to wrap
   * @return a Groovy request fixture
   */
  public static GroovyRequestFixture requestFixture(RequestFixture requestFixture) {
    return new DefaultGroovyRequestFixture(requestFixture);
  }

}
