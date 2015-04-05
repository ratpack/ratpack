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
import ratpack.func.Block;
import ratpack.func.Predicate;

/**
 * A promise for a single value.
 * <p>
 * A promise is a representation of a value, which may not be available yet.
 * This allows asynchronous data processing flows
 * Operations (such as {@link #map(Function)}, {@link #flatMap(Function)}, {@link #cache()} etc.) allow specifying how the value should be processed.
 *
 * <h3>Testing</h3>
 * <p>
 * To test code that uses promises, see the {@code ratpack.test.exec.ExecHarness} class.
 *
 * @param <T> the type of promised value
 */
public interface Promise<T> {

  /**
   * Specifies what should be done with the promised object when it becomes available.
   *
   * @param then the receiver of the promised value
   */
  void then(Action<? super T> then);

  /**
   * Specifies the action to take if the an error occurs trying to produce the promised value.
   *
   * @param errorHandler the action to take if an error occurs
   * @return A promise for the successful result
   */
  Promise<T> onError(Action<? super Throwable> errorHandler);

  /**
   * Consume the promised value as a {@link Result}.
   * <p>
   * This method is an alternative to {@link #then} and {@link #onError}.
   *
   * @param resultHandler the consumer of the result
   */
  default void result(Action<? super Result<T>> resultHandler) {
    onError(t -> resultHandler.execute(Result.<T>failure(t))).then(v -> resultHandler.execute(Result.success(v)));
  }

  /**
   * Transforms the promised value by applying the given function to it.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         c.blocking(() -> "foo")
   *           .map(String::toUpperCase)
   *           .map(s -> s + "-BAR")
   *     );
   *
   *     assertEquals("FOO-BAR", result.getValue());
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
   * Transforms the promise failure (potentially into a value) by applying the given function to it.
   * <p>
   * If the function returns a value, the promise will now be considered successful.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         c.<String>failedPromise(new Exception("!"))
   *           .mapError(e -> "value")
   *     );
   *
   *     assertEquals("value", result.getValue());
   *   }
   * }
   * }</pre>
   * <p>
   * If the function throws an exception, that exception will now represent the promise failure.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         c.<String>failedPromise(new Exception("!"))
   *           .mapError(e -> { throw new RuntimeException("mapped", e); })
   *     );
   *
   *     assertEquals("mapped", result.getThrowable().getMessage());
   *   }
   * }
   * }</pre>
   * <p>
   * The function will not be called if the promise is successful.
   *
   * @param transformer the transformation to apply to the promise failure
   * @return a promise
   */
  Promise<T> mapError(Function<? super Throwable, ? extends T> transformer);

  /**
   * Applies the custom operation function to this promise.
   * <p>
   * This method can be used to apply custom operations without breaking the “code flow”.
   * It works particularly well with method references.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     Integer value = ExecHarness.yieldSingle(e ->
   *         e.blocking(() -> 1)
   *           .apply(Example::dubble)
   *           .apply(Example::triple)
   *     ).getValue();
   *
   *     assertEquals(Integer.valueOf(6), value);
   *   }
   *
   *   public static Promise<Integer> dubble(Promise<Integer> input) {
   *     return input.map(i -> i * 2);
   *   }
   *
   *   public static Promise<Integer> triple(Promise<Integer> input) {
   *     return input.map(i -> i * 3);
   *   }
   * }
   * }</pre>
   * <p>
   * If the apply function throws an exception, the returned promise will fail.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     Throwable error = ExecHarness.yieldSingle(e ->
   *         e.blocking(() -> 1)
   *           .apply(Example::explode)
   *     ).getThrowable();
   *
   *     assertEquals("bang!", error.getMessage());
   *   }
   *
   *   public static Promise<Integer> explode(Promise<Integer> input) throws Exception {
   *     throw new Exception("bang!");
   *   }
   * }
   * }</pre>
   * <p>
   * If the promise having the operation applied to fails, the operation will not be applied.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     Throwable error = ExecHarness.yieldSingle(e ->
   *         e.<Integer>blocking(() -> { throw new Exception("bang!"); })
   *           .apply(Example::dubble)
   *     ).getThrowable();
   *
   *     assertEquals("bang!", error.getMessage());
   *   }
   *
   *   public static Promise<Integer> dubble(Promise<Integer> input) throws Exception {
   *     return input.map(i -> i * 2);
   *   }
   * }
   * }</pre>
   *
   * @param <O> the type of promised object after the operation
   * @param function the operation implementation
   * @return the transformed promise
   */
  <O> Promise<O> apply(Function<? super Promise<T>, ? extends Promise<O>> function);

  /**
   * Applies the given function to {@code this} and returns the result.
   * <p>
   * This method can be useful when needing to convert a promise to another type as it facilitates doing so without breaking the “code flow”.
   * For example, this can be used when integrating with RxJava.
   * <pre class="java">{@code
   * import ratpack.rx.RxRatpack;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.List;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   private static final List<String> LOG = new LinkedList<>();
   *
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(e ->
   *         e.blocking(() -> "foo")
   *           .to(RxRatpack::observe)
   *           .doOnNext(i -> LOG.add("doOnNext"))
   *           .subscribe(LOG::add)
   *     );
   *
   *     assertEquals(Arrays.asList("doOnNext", "foo"), LOG);
   *   }
   * }
   * }</pre>
   * <p>
   * The given function is executed immediately.
   * <p>
   * This method should only be used when converting a promise to another type.
   * See {@link #apply(Function)} for applying custom promise operators.
   *
   * @param function the promise conversion function
   * @param <O> the type the promise will be converted to
   * @return the output of the given function
   * @throws Exception any thrown by the given function
   */
  <O> O to(Function<? super Promise<T>, ? extends O> function) throws Exception;

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
   * import ratpack.exec.ExecResult;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         c.blocking(() -> "foo")
   *           .flatMap(s -> c.blocking(s::toUpperCase))
   *           .map(s -> s + "-BAR")
   *     );
   *
   *     assertEquals("FOO-BAR", result.getValue());
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
   * import ratpack.exec.ExecResult;
   *
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
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
   *     assertEquals(new Integer(1), result1.getValue());
   *     assertFalse(result1.isComplete()); // false because promise returned a value before the execution completed
   *     assertTrue(routed.isEmpty());
   *
   *     ExecResult<Integer> result10 = yield(10, routed);
   *     assertNull(result10.getValue());
   *     assertTrue(result10.isComplete()); // true because the execution completed before the promised value was returned (i.e. it was routed)
   *     assertTrue(routed.contains(10));
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
   * import static org.junit.Assert.assertEquals;
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
   *       assertEquals("10 is too young to be here!", httpClient.getText());
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
  Promise<T> onNull(Block action);

  /**
   * Caches the promised value (or error) and returns it to all subscribers.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * import static org.junit.Assert.assertTrue;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(c -> {
   *       AtomicInteger counter = new AtomicInteger();
   *       Promise<Integer> uncached = c.promise(f -> f.success(counter.getAndIncrement()));
   *
   *       uncached.then(i -> assertTrue(i == 0));
   *       uncached.then(i -> assertTrue(i == 1));
   *       uncached.then(i -> assertTrue(i == 2));
   *
   *       Promise<Integer> cached = uncached.cache();
   *
   *       cached.then(i -> assertTrue(i == 3));
   *       cached.then(i -> assertTrue(i == 3));
   *
   *       uncached.then(i -> assertTrue(i == 4));
   *       cached.then(i -> assertTrue(i == 3));
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
   * import static org.junit.Assert.assertTrue;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(c -> {
   *       Throwable error = new Exception("bang!");
   *       Promise<Object> cached = c.promise(f -> f.error(error)).cache();
   *       cached.onError(t -> assertTrue(t == error)).then(i -> assertTrue("not called", false));
   *       cached.onError(t -> assertTrue(t == error)).then(i -> assertTrue("not called", false));
   *       cached.onError(t -> assertTrue(t == error)).then(i -> assertTrue("not called", false));
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
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> events = Lists.newLinkedList();
   *     ExecHarness.runSingle(c ->
   *         c.<String>promise(f -> {
   *           events.add("promise");
   *           f.success("foo");
   *         })
   *           .onYield(() -> events.add("onYield"))
   *           .then(v -> events.add("then"))
   *     );
   *     assertEquals(Arrays.asList("onYield", "promise", "then"), events);
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
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> events = Lists.newLinkedList();
   *     ExecHarness.runSingle(c ->
   *         c.<String>promise(f -> {
   *           events.add("promise");
   *           f.success("foo");
   *         })
   *           .wiretap(r -> events.add("wiretap: " + r.getValue()))
   *           .then(v -> events.add("then"))
   *     );
   *
   *     assertEquals(Arrays.asList("promise", "wiretap: foo", "then"), events);
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
   * import ratpack.exec.Throttle;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   *
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * import static org.junit.Assert.assertTrue;
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
   *     assertTrue(result.getValue() <= maxAtOnce);
   *   }
   * }
   * }</pre>
   *
   * @param throttle the particular throttle to use to throttle the operation
   * @return the throttled promise
   */
  Promise<T> throttled(Throttle throttle);

}
