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
import ratpack.groovy.test.handling.GroovyInvocationBuilder;
import ratpack.groovy.test.handling.internal.DefaultGroovyInvocationBuilder;
import ratpack.handling.Handler;
import ratpack.test.UnitTest;
import ratpack.test.handling.Invocation;
import ratpack.test.handling.InvocationBuilder;
import ratpack.test.handling.InvocationTimeoutException;

public abstract class GroovyUnitTest {

  /**
   * Unit test a {@link Handler}.
   * <p>
   * Example:
   * <pre class="tested">
   * import ratpack.handling.*
   * import static ratpack.groovy.test.GroovyUnitTest.invoke
   *
   * class MyHandler implements Handler {
   *   void handle(Context context) {
   *     context.with {
   *       def outputHeaderValue = request.headers.get("input-value") + ":bar"
   *
   *       response.headers.set("output-value", outputHeaderValue)
   *       render "received: " + request.path
   *     }
   *   }
   * }
   *
   * // The following code unit tests MyHandler, typically it would be written inside a test framework.
   *
   * def invocation = invoke(new MyHandler()) {
   *   header "input-value", "foo"
   *   uri "some/path"
   * }
   *
   * assert invocation.rendered(String) == "received: some/path"
   * assert invocation.headers.get("output-value") == "foo:bar"
   * </pre>
   *
   * @param handler The handler to unit test
   * @param closure The closure that configures the invocation builder
   * @return The result of the invocation
   * @throws InvocationTimeoutException if the handler takes more than {@link InvocationBuilder#timeout(int)} seconds to send a response or call {@code next()} on the context
   */
  public static Invocation invoke(Handler handler, @DelegatesTo(GroovyInvocationBuilder.class) final Closure<?> closure) throws InvocationTimeoutException {
    return UnitTest.invoke(handler, new Action<InvocationBuilder>() {
      @Override
      public void execute(InvocationBuilder builder) throws Exception {
        GroovyInvocationBuilder groovyBuilder = new DefaultGroovyInvocationBuilder(builder);
        ClosureUtil.configureDelegateFirst(groovyBuilder, closure);
      }
    });
  }

  /**
   * Create a Groovy invocation builder, for unit testing a {@link Handler}.
   *
   * @return An invocation builder.
   */
  public static GroovyInvocationBuilder invocationBuilder() {
    return invocationBuilder(UnitTest.invocationBuilder());
  }

  /**
   * Create a Groovy invocation builder, for unit testing a {@link Handler}, by wrapping the given {@link InvocationBuilder}.
   *
   * @return An invocation builder.
   */
  public static GroovyInvocationBuilder invocationBuilder(InvocationBuilder invocationBuilder) {
    return new DefaultGroovyInvocationBuilder(invocationBuilder);
  }

}
