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

/**
 * Static methods for the unit testing of handlers.
 */
public abstract class UnitTest {

  private UnitTest() {
  }

  /**
   * Unit test a single {@link Handler}.
   *
   * <pre class="java">{@code
   * import ratpack.handling.Context;
   * import ratpack.handling.Handler;
   * import ratpack.test.UnitTest;
   * import ratpack.test.handling.HandlingResult;
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
   *     HandlingResult result = UnitTest.handle(new MyHandler(), fixture ->
   *         fixture.header("input-value", "foo").uri("some/path")
   *     );
   *
   *     assert result.rendered(String.class).equals("received: some/path");
   *     assert result.getHeaders().get("output-value").equals("foo:bar");
   *   }
   * }
   * }</pre>
   *
   * @param handler The handler to invoke
   * @param action The configuration of the context for the handler
   * @return A result object indicating what happened
   * @throws HandlerTimeoutException if the handler takes more than {@link ratpack.test.handling.RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code action}
   * @see #handle(Action, Action)
   */
  @SuppressWarnings("overloads")
  public static HandlingResult handle(Handler handler, Action<? super RequestFixture> action) throws Exception {
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
   * import ratpack.test.UnitTest;
   * import ratpack.test.handling.HandlingResult;
   *
   * public class Example {
   *
   *   public static class MyHandlers implements Action<Chain> {
   *     public void execute(Chain chain) throws Exception {
   *       chain.handler(ctx -> {
   *         String outputHeaderValue = ctx.getRequest().getHeaders().get("input-value") + ":bar";
   *         ctx.getResponse().getHeaders().set("output-value", outputHeaderValue);
   *         ctx.next();
   *       });
   *       chain.handler(ctx -> {
   *         ctx.render("received: " + ctx.getRequest().getPath());
   *       });
   *     }
   *   }
   *
   *   public static void main(String[] args) throws Exception {
   *     HandlingResult result = UnitTest.handle(new MyHandlers(), fixture ->
   *         fixture.header("input-value", "foo").uri("some/path")
   *     );
   *
   *     assert result.rendered(String.class).equals("received: some/path");
   *     assert result.getHeaders().get("output-value").equals("foo:bar");
   *   }
   * }
   * }</pre>
   *
   * @param chainAction the definition of a handler chain to test
   * @param requestFixtureAction the configuration of the request fixture
   * @return a result object indicating what happened
   * @throws HandlerTimeoutException if the handler takes more than {@link ratpack.test.handling.RequestFixture#timeout(int)} seconds to send a response or call {@code next()} on the context
   * @throws Exception any thrown by {@code chainAction} or {@code requestFixtureAction}
   * @see #handle(Handler, Action)
   */
  @SuppressWarnings("overloads")
  public static HandlingResult handle(Action<? super Chain> chainAction, Action<? super RequestFixture> requestFixtureAction) throws Exception {
    RequestFixture requestFixture = requestFixture();
    requestFixtureAction.execute(requestFixture);
    return requestFixture.handleChain(chainAction);
  }

  /**
   * Create a request fixture, for unit testing of {@link Handler handlers}.
   *
   * @see #handle(ratpack.handling.Handler, ratpack.func.Action)
   * @see #handle(ratpack.func.Action, ratpack.func.Action)
   * @return a request fixture
   */
  public static RequestFixture requestFixture() {
    return new DefaultRequestFixture();
  }

}
