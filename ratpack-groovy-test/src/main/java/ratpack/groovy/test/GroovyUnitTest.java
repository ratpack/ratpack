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
import ratpack.groovy.internal.Util;
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
   * <pre class="exec">
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
   * @param builder The closure that configures
   * @return The result of the invocation
   * @throws InvocationTimeoutException if the handler takes more than {@link ratpack.test.handling.InvocationBuilder#timeout(int)} seconds to send a response or call {@code next()} on the context
   */
  public static Invocation invoke(Handler handler, @DelegatesTo(InvocationBuilder.class) Closure<?> builder) throws InvocationTimeoutException {
    return UnitTest.invoke(handler, Util.delegatingAction(builder));
  }

}
