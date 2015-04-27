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

import ratpack.exec.internal.CachingUpstream;
import ratpack.exec.internal.ExecutionBacking;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.func.Function;
import ratpack.func.Predicate;

import java.util.Objects;

import static ratpack.exec.ExecControl.execControl;
import static ratpack.func.Action.ignoreArg;

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
   * Apply a custom transform to this promise.
   * <p>
   * This method is the basis for the standard operations of this interface, such as {@link #map}.
   * The following is a non generic implementation of a map that converts the value to upper case.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *       c.promiseOf("foo")
   *         .transform(up -> down ->
   *           up.connect(down.<String>onSuccess(value -> {
   *             try {
   *               down.success(value.toUpperCase());
   *             } catch (Throwable e) {
   *               down.error(e);
   *             }
   *           }))
   *         )
   *     );
   *
   *     assertEquals("FOO", result.getValue());
   *   }
   * }
   * }</pre>
   * <p>
   * The “upstreamTransformer” function takes an upstream data source, and returns another upstream that wraps it.
   * It is typical for the returned upstream to invoke the {@link Upstream#connect(Downstream)} method of the given upstream during its connect method.
   * <p>
   * For more examples of transform implementations, please see the implementations of the methods of this interface.
   *
   * @param upstreamTransformer a function that returns a new upstream, typically wrapping the given upstream argument
   * @param <O> the type of item emitted by the transformed upstream
   * @return a new promise
   */
  <O> Promise<O> transform(Function<? super Upstream<? extends T>, ? extends Upstream<O>> upstreamTransformer);

  /**
   * Specifies the action to take if the an error occurs trying to produce the promised value.
   *
   * @param errorHandler the action to take if an error occurs
   * @return A promise for the successful result
   */
  default Promise<T> onError(Action<? super Throwable> errorHandler) {
    return transform(up -> down ->
        up.connect(down.onError(throwable -> {
          try {
            errorHandler.execute(throwable);
          } catch (Throwable e) {
            e.addSuppressed(throwable);
            down.error(e);
            return;
          }
          down.complete();
        }))
    );
  }

  /**
   * Consume the promised value as a {@link Result}.
   * <p>
   * This method is an alternative to {@link #then} and {@link #onError}.
   *
   * @param resultHandler the consumer of the result
   */
  default void result(Action<? super Result<T>> resultHandler) {
    onError(t -> resultHandler.execute(Result.<T>error(t))).then(v -> resultHandler.execute(Result.success(v)));
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
  default <O> Promise<O> map(Function<? super T, ? extends O> transformer) {
    return transform(up -> down -> up.connect(
        down.<T>onSuccess(value -> {
          try {
            O apply = transformer.apply(value);
            down.success(apply);
          } catch (Throwable e) {
            down.error(e);
          }
        })
      )
    );
  }

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
  default Promise<T> mapError(Function<? super Throwable, ? extends T> transformer) {
    return transform(up -> down ->
        up.connect(down.onError(throwable -> {
          try {
            down.success(transformer.apply(throwable));
          } catch (Throwable t) {
            t.addSuppressed(throwable);
            down.error(t);
          }
        }))
    );
  }

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
   *   public static Promise<Integer> dubble(Promise<Integer> input) {
   *     return input.map(i -> i * 2);
   *   }
   * }
   * }</pre>
   *
   * @param <O> the type of promised object after the operation
   * @param function the operation implementation
   * @return the transformed promise
   */
  default <O> Promise<O> apply(Function<? super Promise<T>, ? extends Promise<O>> function) {
    try {
      return function.apply(this);
    } catch (Throwable e) {
      return ExecControl.current().failedPromise(e);
    }
  }

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
  default <O> O to(Function<? super Promise<T>, ? extends O> function) throws Exception {
    return function.apply(this);
  }

  /**
   * Like {@link #map(Function)}, but performs the transformation on a blocking thread.
   * <p>
   * This is simply a more convenient form of using {@link ExecControl#blocking(java.util.concurrent.Callable)} and {@link #flatMap(Function)}.
   *
   * @param transformer the transformation to apply to the promised value, on a blocking thread
   * @param <O> the type of the transformed object
   * @return a promise for the transformed value
   */
  default <O> Promise<O> blockingMap(Function<? super T, ? extends O> transformer) {
    return flatMap(t -> execControl().blocking(() -> transformer.apply(t)));
  }

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
  default <O> Promise<O> flatMap(Function<? super T, ? extends Promise<O>> transformer) {
    return transform(up -> down ->
        up.connect(down.<T>onSuccess(value -> {
          try {
            transformer.apply(value).onError(down::error).then(down::success);
          } catch (Throwable e) {
            down.error(e);
          }
        }))
    );
  }

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
  default Promise<T> route(Predicate<? super T> predicate, Action<? super T> action) {
    return transform(up -> down ->
        up.connect(down.<T>onSuccess(value -> {
          boolean apply;
          try {
            apply = predicate.apply(value);
          } catch (Throwable e) {
            down.error(e);
            return;
          }

          if (apply) {
            try {
              action.execute(value);
              down.complete();
            } catch (Throwable e) {
              down.error(e);
            }
          } else {
            down.success(value);
          }
        }))
    );
  }

  /**
   * A convenience shorthand for {@link #route(Predicate, Action) routing} {@code null} values.
   * <p>
   * If the promised value is {@code null}, the given action will be called.
   *
   * @param action the action to route to if the promised value is null
   * @return a routed promise
   */
  default Promise<T> onNull(Block action) {
    return route(Objects::isNull, ignoreArg(action));
  }

  /**
   * Caches the promised value (or error) and returns it to all subscribers.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.concurrent.atomic.AtomicLong;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(c -> {
   *       AtomicLong counter = new AtomicLong();
   *       Promise<Long> uncached = c.promise(f -> f.success(counter.getAndIncrement()));
   *
   *       uncached.then(i -> assertEquals(0l, i.longValue()));
   *       uncached.then(i -> assertEquals(1l, i.longValue()));
   *       uncached.then(i -> assertEquals(2l, i.longValue()));
   *
   *       Promise<Long> cached = uncached.cache();
   *
   *       cached.then(i -> assertEquals(3l, i.longValue()));
   *       cached.then(i -> assertEquals(3l, i.longValue()));
   *
   *       uncached.then(i -> assertEquals(4l, i.longValue()));
   *       cached.then(i -> assertEquals(3l, i.longValue()));
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
  default Promise<T> cache() {
    return transform(CachingUpstream::new);
  }

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
  default Promise<T> defer(Action<? super Runnable> releaser) {
    return transform(up -> down ->
        ExecutionBacking.require().streamSubscribe((streamHandle) -> {
          try {
            releaser.execute((Runnable) () ->
                streamHandle.complete(() -> up.connect(down))
            );
          } catch (Throwable t) {
            down.error(t);
          }
        })
    );
  }

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
  default Promise<T> onYield(Runnable onYield) {
    return transform(up -> down -> {
      try {
        onYield.run();
      } catch (Throwable e) {
        down.error(e);
        return;
      }
      up.connect(down);
    });
  }

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
  default Promise<T> wiretap(Action<? super Result<T>> listener) {
    return transform(up -> down ->
        up.connect(down.<T>onSuccess(value -> {
          try {
            listener.execute(Result.success(value));
          } catch (Throwable t) {
            down.error(t);
            return;
          }

          down.success(value);
        }))
    );
  }

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
  default Promise<T> throttled(Throttle throttle) {
    return throttle.throttle(this);
  }

}
