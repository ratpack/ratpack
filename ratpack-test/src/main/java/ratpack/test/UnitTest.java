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
import ratpack.launch.LaunchConfigBuilder;
import ratpack.test.exec.ExecHarness;
import ratpack.test.exec.internal.DefaultExecHarness;
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
   *   public static void main(String[] args) throws Exception {
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
   * @throws Exception any thrown by {@code action}
   * @see #handle(Action, Action)
   * @see ratpack.test.handling.RequestFixtureAction
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
   *   public static void main(String[] args) throws Exception {
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
   * @throws Exception any thrown by {@code chainAction} or {@code requestFixtureAction}
   * @see #handle(Handler, Action)
   * @see ratpack.test.handling.RequestFixtureAction
   */
  @SuppressWarnings("overloads")
  public static HandlingResult handle(Action<? super Chain> chainAction, Action<? super RequestFixture> requestFixtureAction) throws Exception {
    RequestFixture requestFixture = requestFixture();
    requestFixtureAction.execute(requestFixture);
    return requestFixture.handle(chainAction);
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

  /**
   * Creates a new execution harness, for unit testing code that produces a promise.
   * <pre class="java">
   * import ratpack.func.Action;
   * import ratpack.func.Function;
   * import ratpack.exec.ExecControl;
   * import ratpack.exec.Execution;
   * import ratpack.exec.Promise;
   * import ratpack.exec.Fulfiller;
   * import ratpack.test.UnitTest;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   * import javax.inject.Inject;
   *
   * public class Example {
   *
   *   // An async callback based API
   *   static class AsyncApi {
   *
   *     static interface Callback&lt;T&gt; {
   *       void receive(T value);
   *     }
   *
   *     public &lt;T&gt; void returnAsync(final T value, final Callback&lt;? super T&gt; callback) {
   *       new Thread() {
   *         public void run() {
   *           callback.receive(value);
   *         }
   *       }.run();
   *     }
   *   }
   *
   *   // Our service class that wraps the raw async API
   *   // In the real app this is created by the DI container (e.g. Guice)
   *   static class AsyncService {
   *     private final ExecControl execControl;
   *     private final AsyncApi asyncApi = new AsyncApi();
   *
   *     {@literal @}Inject
   *     public AsyncService(ExecControl execControl) {
   *       this.execControl = execControl;
   *     }
   *
   *     // Our method under test
   *     public &lt;T&gt; Promise&lt;T&gt; promise(final T value) {
   *       return execControl.promise(new Action&lt;Fulfiller&lt;T&gt;&gt;() {
   *         public void execute(final Fulfiller&lt;T&gt; fulfiller) {
   *           asyncApi.returnAsync(value, new AsyncApi.Callback&lt;T&gt;() {
   *             public void receive(T returnedValue) {
   *               fulfiller.success(returnedValue);
   *             }
   *           });
   *         }
   *       });
   *     }
   *   }
   *
   *
   *   // Our test
   *   public static void main(String[] args) throws Throwable {
   *
   *     // the harness must be close()'d when finished with to free resources
   *     try(ExecHarness harness = UnitTest.execHarness()) {
   *
   *       // set up the code under test using the exec control from the harness
   *       final AsyncService service = new AsyncService(harness.getControl());
   *
   *       // exercise the async code using the harness, blocking until the promised value is available
   *       ExecResult&lt;String&gt; result = harness.execute(new Function&lt;Execution, Promise&lt;String&gt;&gt;() {
   *         public Promise&lt;String&gt; apply(Execution execution) {
   *           // execute the code under test
   *           return service.promise("foo");
   *         }
   *       });
   *
   *       assert result.getValue() == "foo";
   *     }
   *   }
   * }
   * </pre>
   *
   * @return An exec harness
   */
  public static ExecHarness execHarness() {
    return new DefaultExecHarness(LaunchConfigBuilder.noBaseDir().build().getExecController());
  }

}
