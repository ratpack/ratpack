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

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.test.handling.HandlerTimeoutException;
import ratpack.test.handling.HandlingResult;
import ratpack.test.handling.RequestFixture;
import ratpack.test.handling.internal.DefaultRequestFixture;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * Static methods for the unit testing of handlers.
 */
public abstract class UnitTest {

  private UnitTest() {
  }

  /**
   * Unit test a single {@link Handler}.
   * <p>
   * <pre class="java">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.test.handling.HandlingResult;
   * import ratpack.test.handling.RequestFixture;
   * import ratpack.test.handling.RequestFixtureAction;
   * import ratpack.test.UnitTest;
   *
   * public class Example {
   *
   *   public static class MyHandler implements Handler {
   *     public void handle(Context context) {
   *       String outputHeaderValue = context.getRequest().getHeaders().get("input-value") + ":bar";
   *       context.getResponse().getHeaders().set("output-value", outputHeaderValue);
   *       context.render("received: " + context.getRequest().getPath());
   *     }
   *   }
   *
   *   public static void main(String[] args) {
   *     HandlingResult result = UnitTest.handle(new MyHandler(), new RequestFixtureAction() {
   *       public void execute() {
   *         header("input-value", "foo");
   *         uri("some/path");
   *       }
   *     });
   *
   *     assert result.rendered(String.class).equals("received: some/path");
   *     assert result.getHeaders().get("output-value").equals("foo:bar");
   *   }
   * }
   * </pre>
   *
   * @param handler The handler to invoke
   * @param action The configuration of the context for the handler
   * @return A result object indicating what happened
   * @throws HandlerTimeoutException if the handler takes more than {@link ratpack.test.handling.RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @see #handle(Action, Action)
   * @see ratpack.test.handling.RequestFixtureAction
   */
  public static HandlingResult handle(Handler handler, Action<? super RequestFixture> action) throws HandlerTimeoutException {
    return buildFixture(action).handle(handler);
  }

  /**
   * Unit test a {@link Handler} chain.
   * <p>
   * <pre class="java">
   * import ratpack.handling.Context;
   * import ratpack.handling.Handler;
   * import ratpack.handling.ChainAction;
   * import ratpack.test.handling.HandlingResult;
   * import ratpack.test.handling.RequestFixtureAction;
   * import ratpack.test.UnitTest;
   *
   * public class Example {
   *
   *   public static class MyHandlers extends ChainAction {
   *     protected void execute() {
   *       handler(new Handler() {
   *         public void handle(Context context) {
   *           String outputHeaderValue = context.getRequest().getHeaders().get("input-value") + ":bar";
   *           context.getResponse().getHeaders().set("output-value", outputHeaderValue);
   *           context.next();
   *         }
   *       });
   *       handler(new Handler() {
   *         public void handle(Context context) {
   *           context.render("received: " + context.getRequest().getPath());
   *         }
   *       });
   *     }
   *   }
   *
   *   public static void main(String[] args) {
   *     HandlingResult result = UnitTest.handle(new MyHandlers(), new RequestFixtureAction() {
   *       public void execute() {
   *         header("input-value", "foo");
   *         uri("some/path");
   *       }
   *     });
   *
   *     assert result.rendered(String.class).equals("received: some/path");
   *     assert result.getHeaders().get("output-value").equals("foo:bar");
   *   }
   * }
   * </pre>
   *
   * @param chainAction the definition of a handler chain to test
   * @param requestFixtureAction the configuration of the request fixture
   * @return a result object indicating what happened
   * @throws HandlerTimeoutException if the handler takes more than {@link ratpack.test.handling.RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @see #handle(Handler, Action)
   * @see ratpack.test.handling.RequestFixtureAction
   */
  public static HandlingResult handle(Action<? super Chain> chainAction, Action<? super RequestFixture> requestFixtureAction) throws HandlerTimeoutException {
    return buildFixture(requestFixtureAction).handle(chainAction);
  }

  /**
   * Create a request fixture, for unit testing of {@link Handler handlers}.
   *
   * @see #handle(Handler, Action)
   * @see #handle(Action, Action)
   * @return a request fixture
   */
  public static RequestFixture requestFixture() {
    return new DefaultRequestFixture();
  }

  private static RequestFixture buildFixture(Action<? super RequestFixture> action) {
    RequestFixture fixture = requestFixture();
    try {
      action.execute(fixture);
    } catch (Exception e) {
      throw uncheck(e);
    }
    return fixture;
  }

}
