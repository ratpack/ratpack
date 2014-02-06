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

package ratpack.test;

import ratpack.handling.Handler;
import ratpack.test.handling.Invocation;
import ratpack.test.handling.InvocationBuilder;
import ratpack.test.handling.InvocationTimeoutException;
import ratpack.test.handling.internal.DefaultInvocationBuilder;
import ratpack.func.Action;

import static ratpack.util.ExceptionUtils.uncheck;

public class UnitTest {

  /**
   * Unit test a {@link Handler}.
   * <p>
   * Example:
   * <pre class="exec">
   * import ratpack.handling.*;
   * import ratpack.util.Action;
   * import ratpack.test.handling.Invocation;
   * import ratpack.test.handling.InvocationBuilder;
   *
   * import static ratpack.test.UnitTest.invoke;
   *
   * public class MyHandler implements Handler {
   *   public void handle(Context context) {
   *     String outputHeaderValue = context.getRequest().getHeaders().get("input-value") + ":bar";
   *     context.getResponse().getHeaders().set("output-value", outputHeaderValue);
   *     context.render("received: " + context.getRequest().getPath());
   *   }
   * }
   *
   * // The following code unit tests MyHandler, typically it would be written inside a test framework.
   *
   * Invocation invocation = invoke(new MyHandler(), new Action&lt;InvocationBuilder&gt;() {
   *   public void execute(InvocationBuilder builder) {
   *     builder.header("input-value", "foo");
   *     builder.uri("some/path");
   *   }
   * });
   *
   * assert invocation.rendered(String).equals("received: some/path");
   * assert invocation.headers.get("output-value").equals("foo:bar");
   * </pre>
   *
   * @param handler The handler to invoke
   * @param action The configuration of the context for the handler
   * @return A result object indicating what happened
   * @throws InvocationTimeoutException if the handler takes more than {@link ratpack.test.handling.InvocationBuilder#timeout(int)} seconds to send a response or call {@code next()} on the context
   */
  public static Invocation invoke(Handler handler, Action<? super InvocationBuilder> action) throws InvocationTimeoutException {
    InvocationBuilder builder = invocationBuilder();
    try {
      action.execute(builder);
    } catch (Exception e) {
      throw uncheck(e);
    }
    return builder.invoke(handler);
  }

  /**
   * Create an invocation builder, for unit testing a {@link Handler}.
   *
   * @see #invoke(ratpack.handling.Handler, ratpack.func.Action)
   * @return An invocation builder.
   */
  public static InvocationBuilder invocationBuilder() {
    return new DefaultInvocationBuilder();
  }
}
