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

package ratpack.exec;

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.func.NoArgAction;
import ratpack.func.Predicate;

/**
 * Operations that can be performed on promises to define an asynchronous data flow.
 * <p>
 * These methods are available on {@link Promise} and {@link SuccessPromise}, but are defined on this separate interface for clarity.
 *
 * @param <T> the type of promised value
 */
public interface PromiseOperations<T> {

  /**
   * Transforms the promised value by applying the given function to it.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *       c.blocking(() -> "foo")
   *         .map(String::toUpperCase)
   *         .map(s -> s + "-BAR")
   *     );
   *
   *     assert result.getValue().equals("FOO-BAR");
   *   }
   * }
   * }</pre>
   *
   * @param transformer the transformation to apply to the promised value
   * @param <O> the type of the transformed object
   * @return a promise for the transformed value
   */
  <O> Promise<O> map(Function<? super T, ? extends O> transformer);

  /**
   * Like {@link #map(Function)}, but performs the transformation on a blocking thread.
   * <p>
   * This is simply a more convenient form of using {@link ExecControl#blocking(java.util.concurrent.Callable)} and {@link #flatMap(Function)}.
   *
   * @param transformer the transformation to apply to the promised value, on a blocking thread
   * @param <O> the type of the transformed object
   * @return a promise for the transformed value
   */
  <O> Promise<O> blockingMap(Function<? super T, ? extends O> transformer);

  /**
   * Transforms the promised value by applying the given function to it that returns a promise for the transformed value.
   * <p>
   * This is useful when the transformation involves an asynchronous operation.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *       c.blocking(() -> "foo")
   *         .flatMap(s -> c.blocking(() -> s.toUpperCase()))
   *         .map(s -> s + "-BAR")
   *     );
   *
   *     assert result.getValue().equals("FOO-BAR");
   *   }
   * }
   * }</pre>
   * <p>
   * In the above example, {@code flatMap()} is being used because the transformation requires a blocking operation (it doesn't really in this case, but that's what the example is showing).
   * In this case, it would be more convenient to use {@link #blockingMap(Function)}.
   *
   * @param transformer the transformation to apply to the promised value
   * @param <O> the type of the transformed object
   * @see #blockingMap(Function)
   * @return a promise for the transformed value
   */
  <O> Promise<O> flatMap(Function<? super T, ? extends Promise<O>> transformer);

  /**
   * Allows the promised value to be handled specially if it meets the given predicate, instead of being handled by the promise subscriber.
   * <p>
   * This is typically used for validating values, centrally.
   * <pre class="java">{@code
   * import com.google.common.collect.Lists;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * import java.util.List;
   *
   * public class Example {
   *   public static ExecResult<Integer> yield(int i, List<Integer> collector) throws Exception {
   *     return ExecHarness.yieldSingle(c ->
   *         c.<Integer>promise(f -> f.success(i))
   *           .route(v -> v > 5, collector::add)
   *     );
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     List<Integer> routed = Lists.newLinkedList();
   *
   *     ExecResult<Integer> result1 = yield(1, routed);
   *     assert result1.getValue().equals(1);
   *     assert !result1.isComplete(); // false because promise returned a value before the execution completed
   *     assert routed.isEmpty();
   *
   *     ExecResult<Integer> result10 = yield(10, routed);
   *     assert result10.getValue() == null;
   *     assert result10.isComplete(); // true because the execution completed before the promised value was returned (i.e. it was routed)
   *     assert routed.contains(10);
   *   }
   * }
   * }</pre>
   * <p>
   * Be careful about using this where the eventual promise subscriber is unlikely to know that the promise
   * will routed as it can be surprising when neither the promised value nor an error appears.
   * <p>
   * It can be useful at the handler layer to provide common validation.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.handling.Context;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * public class Example {
   *   public static Promise<Integer> getAge(Context ctx) {
   *     return ctx
   *       .blocking(() -> 10) // e.g. fetch value from DB
   *       .route(
   *         i -> i < 21,
   *         i -> ctx.render(i + " is too young to be here!")
   *       );
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.fromHandler(ctx ->
   *         getAge(ctx).then(age -> ctx.render("welcome!"))
   *     ).test(httpClient -> {
   *       assert httpClient.getText().equals("10 is too young to be here!");
   *     });
   *   }
   * }
   * }</pre>
   * <p>
   * If the routed-to action throws an exception, it will be forwarded down the promise chain.
   *
   * @param predicate the condition under which the value should be routed
   * @param action the terminal action for the value
   * @return a routed promise
   */
  Promise<T> route(Predicate<? super T> predicate, Action<? super T> action);

  /**
   * A convenience shorthand for {@link #route(Predicate, Action) routing} {@code null} values.
   * <p>
   * If the promised value is {@code null}, the given action will be called.
   *
   * @param action the action to route to if the promised value is null
   * @return a routed promise
   */
  Promise<T> onNull(NoArgAction action);

  /**
   * Caches the promised value (or error) and returns it to all subscribers.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(c -> {
   *       AtomicInteger counter = new AtomicInteger();
   *       Promise<Integer> uncached = c.promise(f -> f.success(counter.getAndIncrement()));
   *
   *       uncached.then(i -> { assert i == 0; });
   *       uncached.then(i -> { assert i == 1; });
   *       uncached.then(i -> { assert i == 2; });
   *
   *       Promise<Integer> cached = uncached.cache();
   *
   *       cached.then(i -> { assert i == 3; });
   *       cached.then(i -> { assert i == 3; });
   *
   *       uncached.then(i -> { assert i == 4; });
   *       cached.then(i -> { assert i == 3; });
   *     });
   *   }
   * }
   * }</pre>
   * <p>
   * If the cached promise fails, the same exception will be returned every time.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(c -> {
   *       Throwable error = new Exception("bang!");
   *       Promise<Object> cached = c.promise(f -> f.error(error)).cache();
   *       cached.onError(t -> { assert t == error; }).then(i -> { assert false : "not called"; });
   *       cached.onError(t -> { assert t == error; }).then(i -> { assert false : "not called"; });
   *       cached.onError(t -> { assert t == error; }).then(i -> { assert false : "not called"; });
   *     });
   *   }
   * }
   * }</pre>
   *
   * @return a caching promise.
   */
  Promise<T> cache();

  /**
   * Allows the execution of the promise to be deferred to a later time.
   * <p>
   * When the returned promise is subscribed to, the given {@code releaser} action will be invoked.
   * The execution of {@code this} promise is deferred until the runnable given to the {@code releaser} is run.
   * <p>
   * It is generally more convenient to use {@link #throttled(Throttle)} or {@link #onYield(Runnable)} than this operation.
   *
   * @param releaser the action that will initiate the execution some time later
   * @return a deferred promise
   */
  Promise<T> defer(Action<? super Runnable> releaser);

  /**
   * Registers a listener that is invoked when {@code this} promise is initiated.
   * <pre class="java">{@code
   * import com.google.common.collect.Lists;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> events = Lists.newLinkedList();
   *     ExecHarness.runSingle(c ->
   *       c.<String>promise(f -> {
   *         events.add("promise");
   *         f.success("foo");
   *       })
   *       .onYield(() -> events.add("onYield"))
   *       .then(v -> events.add("then"))
   *     );
   *
   *     assert events.equals(Arrays.asList("onYield", "promise", "then"));
   *   }
   * }
   * }</pre>
   *
   * @param onYield the action to take when the promise is initiated
   * @return effectively, {@code this} promise
   */
  Promise<T> onYield(Runnable onYield);

  /**
   * Registers a listener for the promise outcome.
   * <pre class="java">{@code
   * import com.google.common.collect.Lists;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> events = Lists.newLinkedList();
   *     ExecHarness.runSingle(c ->
   *       c.<String>promise(f -> {
   *         events.add("promise");
   *         f.success("foo");
   *       })
   *       .wiretap(r -> events.add("wiretap: " + r.getValue()))
   *       .then(v -> events.add("then"))
   *     );
   *
   *     assert events.equals(Arrays.asList("promise", "wiretap: foo", "then"));
   *   }
   * }
   * }</pre>
   *
   * @param listener the result listener
   * @return effectively, {@code this} promise
   */
  Promise<T> wiretap(Action<? super Result<T>> listener);

  /**
   * Throttles {@code this} promise, using the given {@link Throttle throttle}.
   * <p>
   * Throttling can be used to limit concurrency.
   * Typically to limit concurrent use of an external resource, such as a HTTP API.
   * <p>
   * Note that the {@link Throttle} instance given defines the actual throttling semantics.
   * <pre class="java">{@code
   * import ratpack.exec.ExecControl;
   * import ratpack.exec.Throttle;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     int numJobs = 1000;
   *     int maxAtOnce = 10;
   *
   *     ExecResult<Integer> result = ExecHarness.yieldSingle(c -> {
   *       AtomicInteger maxConcurrent = new AtomicInteger();
   *       AtomicInteger active = new AtomicInteger();
   *       AtomicInteger done = new AtomicInteger();
   *
   *       Throttle throttle = Throttle.ofSize(maxAtOnce);
   *
   *       // Launch numJobs forked executions, and return the maximum number that were executing at any given time
   *       return c.promise(f -> {
   *         for (int i = 0; i < numJobs; i++) {
   *           c.exec().start(e2 ->
   *             c
   *               .<Integer>promise(f2 -> {
   *                 int activeNow = active.incrementAndGet();
   *                 int maxConcurrentVal = maxConcurrent.updateAndGet(m -> Math.max(m, activeNow));
   *                 active.decrementAndGet();
   *                 f2.success(maxConcurrentVal);
   *               })
   *               .throttled(throttle) // limit concurrency
   *               .then(max -> {
   *                 if (done.incrementAndGet() == numJobs) {
   *                   f.success(max);
   *                 }
   *               }));
   *         }
   *       });
   *     });
   *
   *     assert result.getValue() <= maxAtOnce;
   *   }
   * }
   * }</pre>
   *
   * @param throttle the particular throttle to use to throttle the operation
   * @return the throttled promise
   */
  Promise<T> throttled(Throttle throttle);

}
