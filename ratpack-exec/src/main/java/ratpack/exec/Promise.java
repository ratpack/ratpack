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

import ratpack.api.NonBlocking;
import ratpack.exec.internal.CachingUpstream;
import ratpack.exec.internal.DefaultExecution;
import ratpack.exec.internal.DefaultPromise;
import ratpack.exec.util.Promised;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.BiFunction;
import ratpack.func.Block;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.func.Pair;
import ratpack.func.Predicate;
import ratpack.util.Exceptions;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static ratpack.func.Action.ignoreArg;

/**
 * A promise for a single value.
 * <p>
 * A promise is a representation of a value which will become available later.
 * Methods such as {@link #map(Function)}, {@link #flatMap(Function)}, {@link #cache()} etc.) allow a pipeline of “operations” to be specified,
 * that the value will travel through as it becomes available.
 * Such operations are implemented via the {@link #transform(Function)} method.
 * Each operation returns a new promise object, not the original promise object.
 * <p>
 * To create a promise, use the {@link Promise#async(Upstream)} method (or one of the variants such as {@link Promise#sync(Factory)}.
 * To test code that uses promises, use the {@link ratpack.test.exec.ExecHarness}.
 * <p>
 * The promise is not “activated” until the {@link #then(Action)} method is called.
 * This method terminates the pipeline, and receives the final value.
 * <p>
 * Promise objects are multi use.
 * Every promise pipeline has a value producing function at its start.
 * Activating a promise (i.e. calling {@link #then(Action)}) invokes the function.
 * The {@link #cache()} operation can be used to change this behaviour.
 *
 * @param <T> the type of promised value
 */
@SuppressWarnings("JavadocReference")
public interface Promise<T> {

  /**
   * Creates a promise for value that will be produced asynchronously.
   * <p>
   * The {@link Upstream#connect(Downstream)} method of the given upstream will be invoked every time the value is requested.
   * This method should propagate the value (or error) to the given downstream object when it is available.
   *
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     String value = ExecHarness.yieldSingle(e ->
   *       Promise.<String>async(down ->
   *         new Thread(() -> {
   *           down.success("foo");
   *         }).start()
   *       )
   *     ).getValueOrThrow();
   *
   *     assertEquals(value, "foo");
   *   }
   * }
   * }</pre>
   *
   * @param upstream the producer of the value
   * @param <T> the type of promised value
   * @return a promise for the asynchronously created value
   * @see Upstream
   * @see #sync(Factory)
   * @see #value(Object)
   * @see #error(Throwable)
   * @since 1.3
   */
  static <T> Promise<T> async(Upstream<T> upstream) {
    return new DefaultPromise<>(DefaultExecution.upstream(upstream));
  }

  /**
   * Creates a promise for the value synchronously produced by the given factory.
   * <p>
   * The given factory will be invoked every time that the value is requested.
   * If the factory throws an exception, the promise will convey that exception.
   *
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     String value = ExecHarness.yieldSingle(e ->
   *       Promise.sync(() -> "foo")
   *     ).getValueOrThrow();
   *
   *     assertEquals(value, "foo");
   *   }
   * }
   * }</pre>
   *
   * <p>
   * This method is often used to when a method needs to return a promise, but can produce its value synchronously.
   *
   * @param factory the producer of the value
   * @param <T> the type of promised value
   * @return a promise for the result of the factory
   * @see #async(Upstream)
   * @see #value(Object)
   * @see #error(Throwable)
   * @since 1.3
   */
  static <T> Promise<T> sync(Factory<T> factory) {
    return new DefaultPromise<>(down ->
      DefaultExecution.require().delimit(down::error, continuation -> {
        T t;
        try {
          t = factory.create();
        } catch (Exception e) {
          continuation.resume(() -> down.error(e));
          return;
        }
        continuation.resume(() -> down.success(t));
      })
    );
  }

  /**
   * Creates a promise for the promise produced by the given factory.
   * <p>
   * The given factory will be invoked every time that the value is requested.
   * If the factory throws an exception, the promise will convey that exception.
   * <p>
   * This can be used to effectively prepend work to another promise.
   *
   * @param factory the producer of the promise to return
   * @param <T> the type of promised value
   * @return a promise for the result of the factory
   * @since 1.5
   */
  static <T> Promise<T> flatten(Factory<? extends Promise<T>> factory) {
    return new DefaultPromise<>(down -> {
      Promise<T> promise;
      try {
        promise = factory.create();
      } catch (Exception e) {
        down.error(e);
        return;
      }

      promise.connect(down);
    });
  }

  /**
   * Deprecated. Use {@link #flatten(Factory)}.
   *
   * @param factory deprecated.
   * @param <T> the type of promised value
   * @return deprecated.
   * @deprecated since 1.5, replaced by {@link #flatten(Factory)}.
   */
  @Deprecated
  static <T> Promise<T> wrap(Factory<? extends Promise<T>> factory) {
    try {
      return factory.create();
    } catch (Exception e) {
      return Promise.error(e);
    }
  }

  /**
   * Creates a promise for the given item.
   * <p>
   * The given item will be used every time that the value is requested.
   *
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     String value = ExecHarness.yieldSingle(e ->
   *       Promise.value("foo")
   *     ).getValueOrThrow();
   *
   *     assertEquals(value, "foo");
   *   }
   * }
   * }</pre>
   *
   * @param t the promised value
   * @param <T> the type of promised value
   * @return a promise for the given item
   * @see #async(Upstream)
   * @see #sync(Factory)
   * @see #error(Throwable)
   */
  static <T> Promise<T> value(T t) {
    return new DefaultPromise<>(down -> DefaultExecution.require().delimit(down::error, continuation ->
      continuation.resume(() -> down.success(t))
    ));
  }

  /**
   * A promise for {@code null}.
   *
   * @param <T> the type of promised value
   * @return a promise for {@code null}.
   * @since 1.5
   */
  @SuppressWarnings("unchecked")
  static <T> Promise<T> ofNull() {
    return (Promise<T>) DefaultPromise.NULL;
  }

  /**
   * Creates a failed promise with the given error.
   * <p>
   * The given error will be used every time that the value is requested.
   *
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertSame;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     Exception exception = new Exception();
   *     Throwable error = ExecHarness.yieldSingle(e ->
   *       Promise.error(exception)
   *     ).getThrowable();
   *
   *     assertSame(exception, error);
   *   }
   * }
   * }</pre>
   *
   * @param t the error
   * @param <T> the type of promised value
   * @return a failed promise
   * @see #async(Upstream)
   * @see #sync(Factory)
   * @see #value(Object)
   */
  static <T> Promise<T> error(Throwable t) {
    return new DefaultPromise<>(down -> DefaultExecution.require().delimit(down::error, continuation ->
      continuation.resume(() -> down.error(t))
    ));
  }

  /**
   * Deprecated.
   *
   * @param upstream the producer of the value
   * @param <T> the type of promised value
   * @return a promise for the asynchronously created value
   * @deprecated replaced by {@link #async(Upstream)}
   */
  @Deprecated
  static <T> Promise<T> of(Upstream<T> upstream) {
    return async(upstream);
  }

  /**
   * Deprecated.
   *
   * @param factory the producer of the value
   * @param <T> the type of promised value
   * @return a promise for the result of the factory
   * @deprecated replaced by {@link #sync(Factory)}}
   */
  @Deprecated
  static <T> Promise<T> ofLazy(Factory<T> factory) {
    return sync(factory);
  }

  /**
   * Specifies what should be done with the promised object when it becomes available.
   * <p>
   * <b>Important:</b> this method can only be used from a Ratpack managed compute thread.
   * If it is called on a non Ratpack managed compute thread it will immediately throw an {@link ExecutionException}.
   *
   * @param then the receiver of the promised value
   * @throws ExecutionException if not called on a Ratpack managed compute thread
   */
  void then(Action<? super T> then);

  /**
   * A low level hook for consuming the promised value.
   * <p>
   * It is generally preferable to use {@link #then(Action)} over this method.
   *
   * @param downstream the downstream consumer
   */
  void connect(Downstream<? super T> downstream);

  /**
   * Apply a custom transform to this promise.
   * <p>
   * This method is the basis for the standard operations of this interface, such as {@link #map(Function)}.
   * The following is a non generic implementation of a map that converts the value to upper case.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *       Promise.value("foo")
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
   * Specifies the action to take if the an error occurs trying to produce the promised value, that the given predicate applies to.
   * <p>
   * If the given action throws an exception, the original exception will be rethrown with the exception thrown
   * by the action added to the suppressed exceptions list.
   *
   * @param predicate the predicate to test against the error
   * @param errorHandler the action to take if an error occurs
   * @return A promise for the successful result
   * @since 1.1
   */
  default Promise<T> onError(Predicate<? super Throwable> predicate, Action<? super Throwable> errorHandler) {
    return transform(up -> down ->
      up.connect(down.onError(throwable -> {
        if (predicate.apply(throwable)) {
          try {
            errorHandler.execute(throwable);
          } catch (Throwable e) {
            if (e != throwable) {
              e.addSuppressed(throwable);
            }
            down.error(e);
            return;
          }
          down.complete();
        } else {
          down.error(throwable);
        }
      }))
    );
  }

  /**
   * Specifies the action to take if the an error of the given type occurs trying to produce the promised value.
   *
   * <pre class="java">{@code
   * import ratpack.http.TypedData;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.fromHandler(ctx ->
   *         ctx.getRequest().getBody()
   *           .map(TypedData::getText)
   *           .map(t -> {
   *             if (t.equals("1")) {
   *               throw new IllegalArgumentException("validation error!");
   *             } else {
   *               throw new RuntimeException("some other error!");
   *             }
   *           })
   *           .onError(IllegalArgumentException.class, e -> ctx.render("the value is invalid"))
   *           .onError(e -> ctx.render("unknown error: " + e.getMessage()))
   *           .then(t -> ctx.render("ok"))
   *     ).test(httpClient -> {
   *       assertEquals(httpClient.requestSpec(r -> r.getBody().text("0")).postText(), "unknown error: some other error!");
   *       assertEquals(httpClient.requestSpec(r -> r.getBody().text("1")).postText(), "the value is invalid");
   *     });
   *   }
   * }
   * }</pre>
   * <p>
   * If the given action throws an exception, the original exception will be rethrown with the exception thrown
   * by the action added to the suppressed exceptions list.
   *
   * @param errorType the type of exception to handle with the given action
   * @param errorHandler the action to take if an error occurs
   * @param <E> the type of exception to handle with the given action
   * @return A promise for the successful result
   * @since 1.1
   */
  default <E extends Throwable> Promise<T> onError(Class<E> errorType, Action<? super E> errorHandler) {
    return onError(errorType::isInstance, t -> errorHandler.execute(errorType.cast(t)));
  }

  /**
   * Specifies the action to take if the an error occurs trying to produce the promised value.
   * <p>
   * If the given action throws an exception, the original exception will be rethrown with the exception thrown
   * by the action added to the suppressed exceptions list.
   *
   * @param errorHandler the action to take if an error occurs
   * @return A promise for the successful result
   */
  default Promise<T> onError(Action<? super Throwable> errorHandler) {
    return onError(Predicate.TRUE, errorHandler);
  }

  /**
   * Consume the promised value as a {@link Result}.
   * <p>
   * This method is an alternative to {@link #then(Action)} and {@link #onError(Action)}.
   *
   * @param resultHandler the consumer of the result
   */
  default void result(Action<? super ExecResult<T>> resultHandler) {
    connect(new Downstream<T>() {
      @Override
      public void success(T value) {
        try {
          resultHandler.execute(ExecResult.of(Result.success(value)));
        } catch (Throwable e) {
          DefaultPromise.throwError(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        try {
          resultHandler.execute(ExecResult.of(Result.<T>error(throwable)));
        } catch (Throwable e) {
          DefaultPromise.throwError(e);
        }
      }

      @Override
      public void complete() {
        try {
          resultHandler.execute(ExecResult.<T>complete());
        } catch (Throwable e) {
          DefaultPromise.throwError(e);
        }
      }
    });
  }

  /**
   * Transforms the promised value by applying the given function to it.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.value("foo")
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
   * Transforms the promised value by applying the given function to it, if it satisfies the predicate.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.value("foo")
   *           .mapIf(s -> s.contains("f"), String::toUpperCase)
   *           .mapIf(s -> s.contains("f"), s -> s + "-BAR")
   *     );
   *
   *     assertEquals("FOO", result.getValue());
   *   }
   * }
   * }</pre>
   *
   * @param predicate the condition to satisfy in order to be transformed
   * @param transformer the transformation to apply to the promised value
   * @return a promise
   * @since 1.4
   */
  default Promise<T> mapIf(Predicate<? super T> predicate, Function<? super T, ? extends T> transformer) {
    return map(Function.when(predicate, transformer));
  }

  /**
   * Transforms the promised value by applying one of the given functions to it, depending if it satisfies the predicate.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.value("foo")
   *           .mapIf(s -> s.contains("f"), String::toUpperCase, s -> s)
   *           .mapIf(s -> s.contains("f"), s -> s, s -> s + "-BAR")
   *     );
   *
   *     assertEquals("FOO-BAR", result.getValue());
   *   }
   * }
   * }</pre>
   *
   * @param predicate the condition to decide which transformation to apply
   * @param onTrue the transformation to apply when the predicate is true
   * @param onFalse the transformation to apply when the predicate is false
   * @param <O> the type of the transformed object
   * @return a promise
   * @since 1.5
   */
  default <O> Promise<O> mapIf(Predicate<? super T> predicate, Function<? super T, ? extends O> onTrue, Function<? super T, ? extends O> onFalse) {
    return map(Function.when(predicate, onTrue, onFalse));
  }

  /**
   * Like {@link #map(Function)}, but performs the transformation on a blocking thread.
   * <p>
   * This is simply a more convenient form of using {@link Blocking#get(Factory)} and {@link #flatMap(Function)}.
   *
   * @param transformer the transformation to apply to the promised value, on a blocking thread
   * @param <O> the type of the transformed object
   * @return a promise for the transformed value
   */
  default <O> Promise<O> blockingMap(Function<? super T, ? extends O> transformer) {
    return flatMap(t -> Blocking.get(() -> transformer.apply(t)));
  }

  /**
   * Executes the given action with the promise value, on a blocking thread.
   * <p>
   * Similar to {@link #blockingMap(Function)}, but does not provide a new value.
   * This can be used to do something with the value, without terminating the promise.
   *
   * @param action the action to to perform with the value, on a blocking thread
   * @return a promise for the same value given to the action
   */
  default Promise<T> blockingOp(Action<? super T> action) {
    return flatMap(t -> Blocking.op(action.curry(t)).map(() -> t));
  }

  /**
   * Deprecated.
   * <p>
   * Use {@link #replace(Promise)}.
   *
   * @param next the promise to replace {@code this} with
   * @param <O> the type of value provided by the replacement promise
   * @return a promise
   * @deprecated replaced by {@link #replace(Promise)} as of 1.1.0
   */
  @Deprecated
  default <O> Promise<O> next(Promise<O> next) {
    return flatMap(in -> next);
  }

  /**
   * Executes the provided, potentially asynchronous, {@link Action} with the promised value as input.
   * <p>
   * This method can be used when needing to perform an action with the promised value, without substituting the promised value.
   * That is, the exact same object provided to the given action will be propagated downstream.
   * <p>
   * The given action is executed within an {@link Operation}, allowing it to perform asynchronous work.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import com.google.common.collect.Lists;
   *
   * import java.util.concurrent.TimeUnit;
   * import java.util.Arrays;
   * import java.util.List;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> events = Lists.newLinkedList();
   *     ExecHarness.runSingle(c ->
   *       Promise.value("foo")
   *        .next(v ->
   *          Promise.value(v) // may be async
   *            .map(String::toUpperCase)
   *            .then(events::add)
   *        )
   *        .then(events::add)
   *     );
   *     assertEquals(Arrays.asList("FOO", "foo"), events);
   *   }
   * }
   * }</pre>
   *
   * @param action the action to execute with the promised value
   * @return a promise for the original value
   * @see #nextOp(Function)
   * @since 1.1
   */
  default Promise<T> next(@NonBlocking Action<? super T> action) {
    return nextOp(v ->
      Operation.of(() ->
        action.execute(v)
      )
    );
  }


  /**
   * Executes the operation returned by the given function.
   * <p>
   * This method can be used when needing to perform an operation returned by another object, based on the promised value.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import ratpack.exec.Operation;
   *
   * import com.google.common.collect.Lists;
   *
   * import java.util.concurrent.TimeUnit;
   * import java.util.Arrays;
   * import java.util.List;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static class CaseService {
   *     public Operation toUpper(String value, List<String> values) {
   *       return Operation.of(() -> values.add(value.toUpperCase()));
   *     }
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     CaseService service = new CaseService();
   *     List<String> events = Lists.newLinkedList();
   *
   *     ExecHarness.runSingle(c ->
   *       Promise.value("foo")
   *        .nextOp(v -> service.toUpper(v, events))
   *        .then(events::add)
   *     );
   *
   *     assertEquals(Arrays.asList("FOO", "foo"), events);
   *   }
   * }
   * }</pre>
   *
   * @param function a function that returns an operation that acts on the promised value
   * @return a promise for the original value
   * @see #next(Action)
   * @since 1.1
   */
  default Promise<T> nextOp(Function<? super T, ? extends Operation> function) {
    return transform(up -> down ->
      up.connect(
        down.<T>onSuccess(value ->
          function.apply(value)
            .onError(down::error)
            .then(() ->
              down.success(value)
            )
        )
      )
    );
  }

  /**
   * Executes the operation returned by the given function, if it satisfies the predicate.
   * <p>
   * This method can be used when needing to perform an operation returned by another object, based on the promised value.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import ratpack.exec.Operation;
   *
   * import com.google.common.collect.Lists;
   *
   * import java.util.concurrent.TimeUnit;
   * import java.util.Arrays;
   * import java.util.List;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static class CaseService {
   *     public Operation toUpper(String value, List<String> values) {
   *       return Operation.of(() -> values.add(value.toUpperCase()));
   *     }
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     CaseService service = new CaseService();
   *     List<String> events = Lists.newLinkedList();
   *
   *     ExecHarness.runSingle(c ->
   *       Promise.value("foo")
   *        .nextOpIf(v -> v.startsWith("f"), v -> service.toUpper(v, events))
   *        .nextOpIf(v -> v.isEmpty(), v -> Operation.of(() -> events.add("empty")))
   *        .then(events::add)
   *     );
   *
   *     assertEquals(Arrays.asList("FOO", "foo"), events);
   *   }
   * }
   * }</pre>
   *
   * @param predicate the condition to satisfy in order to execute the operation.
   * @param function a function that returns an operation that acts on the promised value
   * @return a promise for the original value
   * @since 1.5
   */
  default Promise<T> nextOpIf(Predicate<? super T> predicate, Function<? super T, ? extends Operation> function) {
    return transform(up -> down -> up.connect(
      down.<T>onSuccess(value -> {
        if (predicate.apply(value)) {
          function.apply(value)
            .onError(down::error)
            .then(() -> down.success(value));
        } else {
          down.success(value);
        }
      })
      )
    );
  }

  /**
   * Replaces {@code this} promise with the provided promise for downstream subscribers.
   * <p>
   * This is simply a more convenient form of {@link #flatMap(Function)}, where the given promise is returned.
   * This method can be used when a subsequent operation on a promise isn't dependent on the actual promised value.
   * <p>
   * If the upstream promise fails, its error will propagate downstream and the given promise will never be subscribed to.
   *
   *  <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   private static String value;
   *
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.value("foo")
   *           .next(v -> value = v)
   *           .replace(Promise.value("bar"))
   *     );
   *
   *     assertEquals("bar", result.getValue());
   *     assertEquals("foo", value);
   *   }
   * }
   * }</pre>
   *
   * @param next the promise to replace {@code this} with
   * @param <O> the type of the value of the replacement promise
   * @return a promise
   * @since 1.1
   */
  default <O> Promise<O> replace(Promise<O> next) {
    return flatMap(in -> next);
  }

  /**
   * Transforms the promised value to a {@link Pair}, with the value of the given promise as the {@code left}.
   * <p>
   * The existing promised value will become the {@code right}.
   *
   * @param left a promise for the left value of the result pair
   * @param <O> the type of the left value
   * @return a promise
   */
  default <O> Promise<Pair<O, T>> left(Promise<O> left) {
    return flatLeft(t -> left);
  }

  /**
   * Transforms the promised value to a {@link Pair}, with the result of the given function as the {@code left}.
   * <p>
   * The function is called with the promised value.
   * The existing promised value will become the {@code right}.
   *
   * @param leftFunction a function that produces the left value from the promised value
   * @param <O> the type of the left value
   * @return a promise
   * @since 1.4
   */
  default <O> Promise<Pair<O, T>> left(Function<? super T, ? extends O> leftFunction) {
    return map(right -> Pair.of(
      leftFunction.apply(right), right
    ));
  }

  /**
   * Transforms the promised value to a {@link Pair}, with the value of the result of the given function as the {@code left}.
   * <p>
   * The function is called with the promised value.
   * The existing promised value will become the {@code right}.
   *
   * @param leftFunction a function that produces a promise for the left value from the promised value
   * @param <O> the type of the left value
   * @return a promise
   * @since 1.4
   */
  default <O> Promise<Pair<O, T>> flatLeft(Function<? super T, ? extends Promise<O>> leftFunction) {
    return flatMap(right ->
      leftFunction.apply(right)
        .map(left ->
          Pair.of(left, right)
        )
    );
  }

  /**
   * Transforms the promised value to a {@link Pair}, with the value of the given promise as the {@code right}.
   * <p>
   * The existing promised value will become the {@code left}.
   *
   * @param right a promise for the right value of the result pair
   * @param <O> the type of the right value
   * @return a promise
   */
  default <O> Promise<Pair<T, O>> right(Promise<O> right) {
    return flatRight(t -> right);
  }

  /**
   * Transforms the promised value to a {@link Pair}, with the result of the given function as the {@code right}.
   * <p>
   * The function is called with the promised value.
   * The existing promised value will become the {@code left}.
   *
   * @param rightFunction a function that produces the right value from the promised value
   * @param <O> the type of the left value
   * @return a promise
   * @since 1.4
   */
  default <O> Promise<Pair<T, O>> right(Function<? super T, ? extends O> rightFunction) {
    return map(left -> Pair.of(
      left, rightFunction.apply(left)
    ));
  }

  /**
   * Transforms the promised value to a {@link Pair}, with the value of the result of the given function as the {@code right}.
   * <p>
   * The function is called with the promised value.
   * The existing promised value will become the {@code left}.
   *
   * @param rightFunction a function that produces a promise for the right value from the promised value
   * @param <O> the type of the left value
   * @return a promise
   * @since 1.4
   */
  default <O> Promise<Pair<T, O>> flatRight(Function<? super T, ? extends Promise<O>> rightFunction) {
    return flatMap(left ->
      rightFunction.apply(left)
        .map(right ->
          Pair.of(left, right)
        )
    );
  }

  /**
   * Converts this promise to an operation, by effectively discarding the result.
   *
   * @return an operation
   */
  default Operation operation() {
    return operation(Action.noop());
  }

  /**
   * Converts this promise to an operation which is effectively {@code action}.
   *
   * @param action an operation on the promised value
   * @return an operation representing {@code action}
   */
  default Operation operation(@NonBlocking Action<? super T> action) {
    return Operation.of(() -> then(action));
  }

  /**
   * Converts this promise to an operation, which is the return of {@code function}.
   *
   * @param function a function that returns an operation for the promised value
   * @return effectively the return of {@code function}
   * @since 1.6
   */
  default Operation flatOp(Function<? super T, ? extends Operation> function) {
    return operation(t -> function.apply(t).then());
  }

  /**
   * Transforms the promise failure (potentially into a value) by applying the given function to it.
   * <p>
   * If the function returns a value, the promise will now be considered successful.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.<String>error(new Exception("!"))
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
   * import ratpack.exec.Promise;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.<String>error(new Exception("!"))
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
          T transformed = transformer.apply(throwable);
          down.success(transformed);
        } catch (Throwable t) {
          down.error(t);
        }
      }))
    );
  }

  /**
   * Transforms a failure of the given type (potentially into a value) by applying the given function to it.
   * <p>
   * This method is similar to {@link #mapError(Function)}, except that it will only apply if the error is of the given type.
   * If the error is not of the given type, it will not be transformed and will propagate as normal.
   *
   * @param function the transformation to apply to the promise failure
   * @return a promise
   * @since 1.3
   */
  default <E extends Throwable> Promise<T> mapError(Class<E> type, Function<? super E, ? extends T> function) {
    return transform(up -> down ->
      up.connect(down.onError(throwable -> {
        if (type.isInstance(throwable)) {
          T transformed;
          try {
            transformed = function.apply(type.cast(throwable));
          } catch (Throwable t) {
            down.error(t);
            return;
          }
          down.success(transformed);
        } else {
          down.error(throwable);
        }
      }))
    );
  }

  /**
   * Transforms a failure of the given type (potentially into a value) by applying the given function to it.
   * <p>
   * This method is similar to {@link #mapError(Function)}, except that it will only apply depending if it satisfies the predicate.
   * If the error is not of the given type, it will not be transformed and will propagate as normal.
   *
   * @param predicate the predicate to test against the error
   * @param function the transformation to apply to the promise failure
   * @return a promise
   * @since 1.6.0
   */
  default Promise<T> mapError(Predicate<? super Throwable> predicate, Function<? super Throwable, ? extends T> function) {
    return transform(up -> down ->
      up.connect(down.onError(throwable -> {
        if (predicate.apply(throwable)) {
          T transformed;
          try {
            transformed = function.apply(throwable);
          } catch (Throwable t) {
            down.error(t);
            return;
          }
          down.success(transformed);
        } else {
          down.error(throwable);
        }
      }))
    );
  }

  /**
   * Transforms a failure of the given type (potentially into a value) by applying the given function to it.
   * <p>
   * This method is similar to {@link #mapError(Function)}, except that it allows async transformation.
   *
   * @param function the transformation to apply to the promise failure
   * @return a promise
   * @since 1.3
   */
  default Promise<T> flatMapError(Function<? super Throwable, ? extends Promise<T>> function) {
    return transform(up -> down ->
      up.connect(down.onError(throwable -> {
        Promise<T> transformed;
        try {
          transformed = function.apply(throwable);
        } catch (Throwable t) {
          down.error(t);
          return;
        }
        transformed.connect(down);
      }))
    );
  }

  /**
   * Transforms a failure of the given type (potentially into a value) by applying the given function to it.
   * <p>
   * This method is similar to {@link #mapError(Class, Function)}, except that it allows async transformation.
   *
   * @param function the transformation to apply to the promise failure
   * @return a promise
   * @since 1.3
   */
  default <E extends Throwable> Promise<T> flatMapError(Class<E> type, Function<? super E, ? extends Promise<T>> function) {
    return transform(up -> down ->
      up.connect(down.onError(throwable -> {
        if (type.isInstance(throwable)) {
          Promise<T> transformed;
          try {
            transformed = function.apply(type.cast(throwable));
          } catch (Throwable t) {
            down.error(t);
            return;
          }
          transformed.connect(down);
        } else {
          down.error(throwable);
        }
      }))
    );
  }

  /**
   * Transforms a failure of the given type (potentially into a value) by applying the given function to it.
   * <p>
   * This method is similar to {@link #mapError(Predicate, Function)}, except that it allows async transformation.
   *
   * @param predicate the predicate to test against the error
   * @param function the transformation to apply to the promise failure
   * @return a promise
   * @since 1.6.0
   */
  default Promise<T> flatMapError(Predicate<? super Throwable> predicate, Function<? super Throwable, ? extends Promise<T>> function) {
    return transform(up -> down ->
      up.connect(down.onError(throwable -> {
        if (predicate.apply(throwable)) {
          Promise<T> transformed;
          try {
            transformed = function.apply(throwable);
          } catch (Throwable t) {
            down.error(t);
            return;
          }
          transformed.connect(down);
        } else {
          down.error(throwable);
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
   *         Promise.value(1)
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
   *         Promise.value(1)
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
   *         Promise.<Integer>error(new Exception("bang!"))
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
      return Promise.error(e);
    }
  }

  /**
   * Applies the given function to {@code this} and returns the result.
   * <p>
   * This method can be useful when needing to convert a promise to another type as it facilitates doing so without breaking the “code flow”.
   * For example, this can be used when integrating with RxJava.
   * <pre class="java">{@code
   * import ratpack.rx.RxRatpack;
   * import ratpack.exec.Promise;
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
   *         Promise.value("foo")
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
   * Transforms the promised value by applying the given function to it that returns a promise for the transformed value.
   * <p>
   * This is useful when the transformation involves an asynchronous operation.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import ratpack.exec.Blocking;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.value("foo")
   *           .flatMap(s -> Blocking.get(s::toUpperCase))
   *           .map(s -> s + "-BAR")
   *     );
   *
   *     assertEquals("FOO-BAR", result.getValue());
   *   }
   * }
   * }</pre>
   * <p>
   *
   * @param transformer the transformation to apply to the promised value
   * @param <O> the type of the transformed object
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
   * Transforms the promised value by applying the given function to it that returns a promise for the transformed value, if it satisfies the predicate.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.value("foo")
   *           .flatMapIf(s -> s.contains("f"), s -> Promise.value(s.toUpperCase()))
   *           .flatMapIf(s -> s.contains("f"), s -> Promise.value(s + "-BAR"))
   *     );
   *
   *     assertEquals("FOO", result.getValue());
   *   }
   * }
   * }</pre>
   *
   * @param predicate the condition to satisfy in order to be transformed
   * @param transformer the transformation to apply to the promised value
   * @return a promise
   * @since 1.4
   */
  default Promise<T> flatMapIf(Predicate<? super T> predicate, Function<? super T, ? extends Promise<T>> transformer) {
    return flatMapIf(predicate, transformer, Promise::value);
  }

  /**
   * Transforms the promised value by applying one of the given functions to it that returns a promise for the transformed value, depending if it satisfies the predicate.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(c ->
   *         Promise.value("foo")
   *           .flatMapIf(s -> s.contains("f"), s -> Promise.value(s.toUpperCase()), s -> Promise.value(s))
   *           .flatMapIf(s -> s.contains("f"), s -> Promise.value(s), s -> Promise.value(s + "-BAR"))
   *     );
   *
   *     assertEquals("FOO-BAR", result.getValue());
   *   }
   * }
   * }</pre>
   *
   * @param predicate the condition to decide which transformation to apply
   * @param onTrue the transformation to apply to the promised value when the predicate is true
   * @param onFalse the transformation to apply to the promised value when the predicate is false
   * @return a promise
   * @since 1.5
   */
  default <O> Promise<O> flatMapIf(Predicate<? super T> predicate, Function<? super T, ? extends Promise<O>> onTrue, Function<? super T, ? extends Promise<O>> onFalse) {
    return flatMap(Function.when(predicate, onTrue, onFalse));
  }

  /**
   * Allows the promised value to be handled specially if it meets the given predicate, instead of being handled by the promise subscriber.
   * <p>
   * This is typically used for validating values, centrally.
   * <pre class="java">{@code
   * import com.google.common.collect.Lists;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   *
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static ExecResult<Integer> yield(int i, List<Integer> collector) throws Exception {
   *     return ExecHarness.yieldSingle(c ->
   *         Promise.value(i)
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
   *     return Promise.value(10)
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
   * <p>
   * This method is equivalent to using {@link #cacheResultIf(Predicate)} with a predicate that always returns {@code true}.
   *
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
   *       Promise<Long> uncached = Promise.async(f -> f.success(counter.getAndIncrement()));
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
   *
   * <p>
   * If the cached promise fails, the same exception will be returned every time.
   *
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
   *       Promise<Object> cached = Promise.error(error).cache();
   *       cached.onError(t -> assertTrue(t == error)).then(i -> assertTrue("not called", false));
   *       cached.onError(t -> assertTrue(t == error)).then(i -> assertTrue("not called", false));
   *       cached.onError(t -> assertTrue(t == error)).then(i -> assertTrue("not called", false));
   *     });
   *   }
   * }
   * }</pre>
   *
   * @return a caching promise
   * @see #cacheIf(Predicate)
   * @see #cacheResultIf(Predicate)
   * @see #cacheResultFor(Function)
   */
  default Promise<T> cache() {
    return cacheResultIf(Predicate.TRUE);
  }

  /**
   * Caches the promise value and provides it to all future subscribers, if it satisfies the predicate.
   * <p>
   * This method is equivalent to using {@link #cacheResultIf(Predicate)} with a predicate that requires
   * a successful result and for the value to satisfy the predicate given to this method.
   * <p>
   * Non success results will not be cached.
   *
   * @param shouldCache the test for whether a successful result is cacheable
   * @return a caching promise
   * @see #cacheResultIf(Predicate)
   * @see #cacheResultFor(Function)
   * @since 1.4
   */
  default Promise<T> cacheIf(Predicate<? super T> shouldCache) {
    return cacheResultIf(r -> r.isSuccess() && shouldCache.apply(r.getValue()));
  }

  /**
   * Caches the promise result eternally and provide it to all future subscribers, if it satisfies the predicate.
   *
   * <pre class="java">{@code
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.ArrayList;
   * import java.util.List;
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     List<ExecResult<Integer>> results = new ArrayList<>();
   *     AtomicInteger counter = new AtomicInteger();
   *     Promise<Integer> promise = Promise.sync(() -> {
   *       int i = counter.getAndIncrement();
   *       if (i < 2) {
   *         return i;
   *       } else if (i == 2) {
   *         throw new Exception(Integer.toString(i));
   *       } else if (i == 3) {
   *         throw new RuntimeException(Integer.toString(i));
   *       } else {
   *         throw new IllegalStateException(Integer.toString(i));
   *       }
   *     });
   *
   *     Promise<Integer> cachedPromise = promise.cacheResultIf(r ->
   *       (r.isError() && r.getThrowable().getClass() == RuntimeException.class)
   *         || (r.isSuccess() && r.getValue() > 10)
   *     );
   *
   *     ExecHarness.runSingle(e -> {
   *       for (int i = 0; i < 6; i++) {
   *         cachedPromise.result(results::add);
   *       }
   *     });
   *
   *     assertEquals(results.get(0).getValueOrThrow(), Integer.valueOf(0));
   *     assertEquals(results.get(1).getValueOrThrow(), Integer.valueOf(1));
   *     assertEquals(results.get(2).getThrowable().getClass(), Exception.class);
   *     assertEquals(results.get(3).getThrowable().getClass(), RuntimeException.class);
   *
   *     // value is now cached
   *     assertEquals(results.get(4).getThrowable().getClass(), RuntimeException.class);
   *     assertEquals(results.get(5).getThrowable().getClass(), RuntimeException.class);
   *   }
   * }
   * }</pre>
   *
   * <p>
   * Note, the cached value never expires.
   * If you wish to cache for a certain amount of time, use {@link #cacheResultFor(Function)}.
   *
   * @param shouldCache the test for whether a result is cacheable
   * @return a caching promise
   * @see #cache()
   * @see #cacheIf(Predicate)
   * @see #cacheResultFor(Function)
   * @since 1.4
   */
  default Promise<T> cacheResultIf(Predicate<? super ExecResult<T>> shouldCache) {
    return transform(up ->
      new CachingUpstream<>(up, shouldCache.function(Duration.ofSeconds(-1), Duration.ZERO))
    );
  }

  /**
   * Caches the promise result for a calculated amount of time.
   * <p>
   * A cached promise is fully threadsafe and and can be subscribed to concurrently.
   * While there is no valid cached value, yielding the upstream value is serialised.
   * That is, one value is requested at a time regardless of concurrent subscription.
   * <p>
   * As the result is received, it is given to the {@code ttlFunc} which determines how long to cache it for.
   * A {@link Duration#ZERO} duration indicates that the value should not be cached.
   * Any {@link Duration#isNegative()} duration indicates that the value should be cached eternally.
   * Any other duration indicates how long to cache the result for.
   * <p>
   * If the promise is subscribed to again after the cached value has expired, the process repeats.
   * <p>
   * As such promises tend to be held and reused, it is sometimes necessary to consider garbage collection implications.
   * A caching promise (like all multi-use promises) must retain all of its upstream functions/objects.
   * Care should be taken to ensure that this does not cause long lived references to objects that should be collected.
   * <p>
   * It is common to use cached promises in conjunction with a cache implementation such as Google Guava or Caffeine.
   *
   * <pre class="java">{@code
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.ArrayList;
   * import java.util.List;
   * import java.time.Duration;
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     List<Integer> results = new ArrayList<>();
   *     AtomicInteger counter = new AtomicInteger();
   *     Promise<Integer> promise = Promise.sync(counter::getAndIncrement)
   *       .cacheResultFor(
   *         i -> i.isSuccess() && i.getValue() > 1
   *         ? Duration.ofSeconds(1)
   *         : Duration.ZERO
   *       );
   *
   *     for (int i = 0; i < 4; ++i) {
   *       ExecHarness.runSingle(e -> promise.then(results::add));
   *     }
   *
   *     // let the cache entry expire
   *     Thread.sleep(1500);
   *
   *     for (int i = 0; i < 2; ++i) {
   *       ExecHarness.runSingle(e -> promise.then(results::add));
   *     }
   *
   *     assertEquals(results.get(0), Integer.valueOf(0));
   *     assertEquals(results.get(1), Integer.valueOf(1));
   *     assertEquals(results.get(2), Integer.valueOf(2));
   *     assertEquals(results.get(3), Integer.valueOf(2));
   *
   *     // cache entry has expired
   *
   *     assertEquals(results.get(4), Integer.valueOf(3));
   *     assertEquals(results.get(5), Integer.valueOf(3));
   *   }
   * }
   * }</pre>
   *
   * @param cacheFor a function that determines how long to cache the given result for
   * @return a caching promise
   * @since 1.5
   * @see #cache()
   * @see #cacheIf(Predicate)
   * @see #cacheResultIf(Predicate)
   */
  default Promise<T> cacheResultFor(Function<? super ExecResult<T>, Duration> cacheFor) {
    return transform(up -> new CachingUpstream<>(up, cacheFor));
  }

  /**
   * Defers the subscription of {@code this} promise until later.
   * <p>
   * When the returned promise is subscribed to, the given {@code releaser} action will be invoked.
   * The execution of {@code this} promise is deferred until the runnable given to the {@code releaser} is run.
   * <p>
   * It is important to note that this defers the <i>subscription</i> of the promise, not the delivery of the value.
   * <p>
   * It is generally more convenient to use {@link #throttled(Throttle)} or {@link #onYield(Runnable)} than this operation.
   *
   * @param releaser the action that will initiate the execution some time later
   * @return a deferred promise
   */
  default Promise<T> defer(Action<? super Runnable> releaser) {
    return transform(up -> down ->
      Promise.async(innerDown ->
        releaser.execute((Runnable) () -> innerDown.success(true))
      ).then(v ->
        up.connect(down)
      )
    );
  }

  /**
   * Defers the subscription of {@code this} promise for the given duration.
   * <p>
   * This operation is roughly the promise based analog of {@link Execution#sleep(Duration, Block)}.
   * <p>
   * The given duration must be non-negative.
   *
   * @param duration the amount of time to defer for
   * @return a deferred promise
   * @see #defer(Action)
   * @since 1.5
   */
  default Promise<T> defer(Duration duration) {
    return defer(r -> Execution.sleep(duration, r::run));
  }

  /**
   * Registers a listener that is invoked when {@code this} promise is initiated.
   * <pre class="java">{@code
   * import com.google.common.collect.Lists;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.Promise;
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
   *         Promise.<String>sync(() -> {
   *           events.add("promise");
   *           return "foo";
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
   * import ratpack.exec.Promise;
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
   *         Promise.<String>sync(() -> {
   *           events.add("promise");
   *           return "foo";
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
      up.connect(new Downstream<T>() {
        @Override
        public void success(T value) {
          try {
            listener.execute(Result.success(value));
          } catch (Exception e) {
            down.error(e);
            return;
          }
          down.success(value);
        }

        @Override
        public void error(Throwable throwable) {
          try {
            listener.execute(Result.error(throwable));
          } catch (Exception e) {
            throwable.addSuppressed(e);
          }
          down.error(throwable);
        }

        @Override
        public void complete() {
          down.complete();
        }
      })
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
   * import ratpack.exec.Promise;
   * import ratpack.exec.Execution;
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
   *     ExecResult<Integer> result = ExecHarness.yieldSingle(exec -> {
   *       AtomicInteger maxConcurrent = new AtomicInteger();
   *       AtomicInteger active = new AtomicInteger();
   *       AtomicInteger done = new AtomicInteger();
   *
   *       Throttle throttle = Throttle.ofSize(maxAtOnce);
   *
   *       // Launch numJobs forked executions, and return the maximum number that were executing at any given time
   *       return Promise.async(downstream -> {
   *         for (int i = 0; i < numJobs; i++) {
   *           Execution.fork().start(forkedExec ->
   *             Promise.sync(() -> {
   *               int activeNow = active.incrementAndGet();
   *               int maxConcurrentVal = maxConcurrent.updateAndGet(m -> Math.max(m, activeNow));
   *               active.decrementAndGet();
   *               return maxConcurrentVal;
   *             })
   *             .throttled(throttle) // limit concurrency
   *             .then(max -> {
   *               if (done.incrementAndGet() == numJobs) {
   *                 downstream.success(max);
   *               }
   *             })
   *           );
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

  /**
   * Closes the given closeable when the value or error propagates to this point.
   * <p>
   * This can be used to simulate a try/finally synchronous construct.
   * It is typically used to close some resource after an asynchronous operation.
   *
   * <pre class="java">{@code
   * import org.junit.Assert;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * public class Example {
   *   static class MyResource implements AutoCloseable {
   *     final boolean inError;
   *     boolean closed;
   *
   *     public MyResource(boolean inError) {
   *       this.inError = inError;
   *     }
   *
   *     {@literal @}Override
   *     public void close() {
   *       closed = true;
   *     }
   *   }
   *
   *   static Promise<String> resourceUsingMethod(MyResource resource) {
   *     return Promise.sync(() -> {
   *       if (resource.inError) {
   *         throw new Exception("error!");
   *       } else {
   *         return "ok!";
   *       }
   *     });
   *   }
   *
   *   public static void main(String[] args) throws Exception {
   *     ExecHarness.runSingle(e -> {
   *       MyResource myResource = new MyResource(false);
   *       resourceUsingMethod(myResource)
   *         .close(myResource)
   *         .then(value -> Assert.assertTrue(myResource.closed));
   *     });
   *
   *     ExecHarness.runSingle(e -> {
   *       MyResource myResource = new MyResource(true);
   *       resourceUsingMethod(myResource)
   *         .close(myResource)
   *         .onError(error -> Assert.assertTrue(myResource.closed))
   *         .then(value -> {
   *           throw new UnsupportedOperationException("should not reach here!");
   *         });
   *     });
   *
   *   }
   * }
   * }</pre>
   * <p>
   * The general pattern is to open the resource, and then pass it to some method/closure that works with it and returns a promise.
   * This method is then called on the returned promise to cleanup the resource.
   *
   * @param closeable the closeable to close
   * @see #close(Operation)
   * @return a promise
   * @since 1.3
   */
  default Promise<T> close(AutoCloseable closeable) {
    return transform(up -> down ->
      up.connect(new Downstream<T>() {
        @Override
        public void success(T value) {
          try {
            closeable.close();
          } catch (Exception e) {
            down.error(e);
            return;
          }
          down.success(value);
        }

        @Override
        public void error(Throwable throwable) {
          try {
            closeable.close();
          } catch (Exception e) {
            throwable.addSuppressed(e);
          }
          down.error(throwable);
        }

        @Override
        public void complete() {
          try {
            closeable.close();
          } catch (Exception e) {
            down.error(e);
            return;
          }
          down.complete();
        }
      })
    );
  }

  /**
   * Like {@link #close(AutoCloseable)}, but allows async close operations.
   *
   * @param closer the close operation.
   * @return a promise
   * @since 1.5
   */
  default Promise<T> close(Operation closer) {
    return transform(up -> down ->
      up.connect(new Downstream<T>() {
        @Override
        public void success(T value) {
          closer.promise().connect(new Downstream<Void>() {
            @Override
            public void success(Void v) {
              down.success(value);
            }

            @Override
            public void error(Throwable throwable) {
              down.error(throwable);
            }

            @Override
            public void complete() {
              down.success(value);
            }
          });
        }

        @Override
        public void error(Throwable throwable) {
          closer.promise().connect(new Downstream<Void>() {
            @Override
            public void success(Void v) {
              down.error(throwable);
            }

            @Override
            public void error(Throwable innerThrowable) {
              innerThrowable.addSuppressed(throwable);
              down.error(innerThrowable);
            }

            @Override
            public void complete() {
              down.error(throwable);
            }
          });
        }

        @Override
        public void complete() {
          closer.promise().connect(new Downstream<Void>() {
            @Override
            public void success(Void v) {
              down.complete();
            }

            @Override
            public void error(Throwable innerThrowable) {
              down.error(innerThrowable);
            }

            @Override
            public void complete() {
              down.complete();
            }
          });
        }
      })
    );
  }

  /**
   * Emits the time taken from when the promise is subscribed to to when the result is available.
   * <p>
   * The given {@code action} is called regardless of whether the promise is successful or not.
   * <p>
   * If the promise fails and this method throws an exception, the original exception will propagate with the thrown exception suppressed.
   * If the promise succeeds and this method throws an exception, the thrown exception will propagate.
   *
   * @param action a callback for the time
   * @since 1.3
   * @see #timeResult(BiAction)
   * @return effectively {@code this}
   */
  default Promise<T> time(Action<? super Duration> action) {
    return timeResult((r, d) -> action.execute(d));
  }

  /**
   * Emits the time taken from when the promise is subscribed to to when the result is available.
   * <p>
   * The given {@code action} is called regardless of whether the promise is successful or not.
   * <p>
   * If the promise fails and this method throws an exception, the original exception will propagate with the thrown exception suppressed.
   * If the promise succeeds and this method throws an exception, the thrown exception will propagate.
   *
   * @param action a callback for the time
   * @since 1.5
   * @see #time(Action)
   * @return effectively {@code this}
   */
  default Promise<T> timeResult(BiAction<? super ExecResult<T>, ? super Duration> action) {
    return around(System::nanoTime, (start, result) -> {
      // protect against clock skew causing negative durations
      Duration duration = Duration.ofNanos(Math.max(0, System.nanoTime() - start));
      action.execute(result, duration);
      return result;
    });

  }

  default Promise<Pair<ExecResult<T>, Duration>> timeResult() {
    return around(System::nanoTime, (start, result) -> {
      // protect against clock skew causing negative durations
      Duration duration = Duration.ofNanos(Math.max(0, System.nanoTime() - start));
      return ExecResult.of(Result.success(Pair.of(result, duration)));
    });

  }

  /**
   * Facilitates capturing a value before the the promise is subscribed and using it to later augment the result.
   * <p>
   * The {@code before} factory is invoked as the promise is subscribed.
   * As the promise result becomes available, it and the result are given to the {@code after} function.
   * The return value of the {@code after} function forms the basis of the promise returned from this method.
   *
   * @param before the before value supplier
   * @param after the after function
   * @param <B> the before value type
   * @param <A> the after value type
   * @return a promise
   * @since 1.5
   */
  default <B, A> Promise<A> around(Factory<? extends B> before, BiFunction<? super B, ? super ExecResult<T>, ? extends ExecResult<A>> after) {
    return transform(up -> down -> {
      B start;
      try {
        start = before.create();
      } catch (Throwable e) {
        down.error(e);
        return;
      }

      up.connect(new Downstream<T>() {
        private void onResult(ExecResult<T> originalResult) {
          ExecResult<A> newResult;
          try {
            newResult = after.apply(start, originalResult);
          } catch (Throwable t) {
            if (originalResult.isError() && originalResult.getThrowable() != t) {
              t.addSuppressed(originalResult.getThrowable());
            }
            down.error(t);
            return;
          }

          down.accept(newResult);
        }

        @Override
        public void success(T value) {
          onResult(ExecResult.of(Result.success(value)));
        }

        @Override
        public void error(Throwable throwable) {
          onResult(ExecResult.of(Result.error(throwable)));
        }

        @Override
        public void complete() {
          onResult(ExecResult.complete());
        }
      });
    });
  }

  /**
   * Forks a new execution and subscribes to this promise, returning a promise for its value.
   * <p>
   * The new execution is created and started immediately by this method, effectively subscribing to the promise immediately.
   * The returned promise provides the value when the execution completes.
   * <p>
   * This method can be used for simple of processing.
   * It is often combined with the {@link #left(Promise)} or {@link #right(Promise)}.
   *
   * <pre class="java">{@code
   * import ratpack.exec.Blocking;
   * import ratpack.exec.Promise;
   * import ratpack.func.Pair;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.concurrent.CyclicBarrier;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     CyclicBarrier barrier = new CyclicBarrier(2);
   *
   *     Pair<Integer, String> result = ExecHarness.yieldSingle(r -> {
   *       Promise<Integer> p1 = Blocking.get(() -> {
   *         barrier.await();
   *         return 1;
   *       });
   *       Promise<String> p2 = Blocking.get(() -> {
   *         barrier.await();
   *         return "2";
   *       });
   *
   *       return p1.right(p2.fork());
   *     }).getValueOrThrow();
   *
   *     assertEquals(result, Pair.of(1, "2"));
   *   }
   *
   * }
   * }</pre>
   * <p>
   * <b>Warning:</b> be mindful of error handling for forked promises.
   * If the forked promise is never subscribed to, its failure may go unnoticed.
   * In scenarios, where it cannot be guaranteed that the forked promise will be subscribed to
   * or that subscribers would satisfactorily deal with error conditions,
   * consider <i>listening</i> for errors by using {@link #wiretap(Action)} before {@link #fork()}
   * and logging the error or similar.
   *
   * @param execSpec configuration for the forked execution
   * @return a promise
   * @throws Exception any thrown by {@code execSpec}
   * @since 1.4
   */
  default Promise<T> fork(Action<? super ExecSpec> execSpec) throws Exception {
    Promised<T> promised = new Promised<>();
    execSpec.with(Execution.fork()).start(e -> connect(promised));
    return promised.promise();
  }

  /**
   * Forks a new execution and subscribes to this promise, returning a promise for its value.
   * <p>
   * This method delegates to {@link #fork(Action)} with {@link Action#noop()}.
   *
   * @return a promise
   * @since 1.4
   * @see #fork(Action)
   */
  default Promise<T> fork() {
    return Exceptions.uncheck(() -> fork(Action.noop()));
  }

  /**
   * Causes {@code this} yielding the promised value to be retried on error, after a fixed delay.
   * <p>
   * The given function is invoked for each failure,
   * with the sequence number of the failure as the first argument and the failure exception as the second.
   * This may be used to log or collect exceptions.
   * If all errors are to be ignored, use {@link BiAction#noop()}.
   * <p>
   * Any exception thrown by the function – possibly the exception it receives as an argument – will
   * be propagated to the subscriber, yielding a failure.
   * This can be used to selectively retry on certain failures, but immediately fail on others.
   * <p>
   * If the promise fails {@code maxAttempts} times,
   * the given function will not be invoked and the most and the most recent exception will propagate.
   * <p>
   * To retry immediately, pass a zero duration.
   * Use {@link #retry(int, BiFunction)} if desiring a dynamic (e.g. increasing) delay between attempts.
   *
   * <pre class="java">{@code
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.time.Duration;
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.List;
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   private static final List<String> LOG = new LinkedList<>();
   *
   *   public static void main(String... args) throws Exception {
   *     AtomicInteger source = new AtomicInteger();
   *
   *     ExecResult<Integer> result = ExecHarness.yieldSingle(exec ->
   *       Promise.sync(source::incrementAndGet)
   *         .mapIf(i -> i < 3, i -> { throw new IllegalStateException(); })
   *         .retry(3, Duration.ofMillis(500), (i, t) -> LOG.add("retry attempt: " + i))
   *     );
   *
   *     assertEquals(Integer.valueOf(3), result.getValue());
   *     assertEquals(Arrays.asList("retry attempt: 1", "retry attempt: 2"), LOG);
   *   }
   * }
   * }</pre>
   *
   * @param maxAttempts the maximum number of times to retry
   * @param delay the duration to wait between retry attempts
   * @param onError the error handler
   * @return a promise with a retry error handler
   * @see #retry(int, BiFunction)
   * @since 1.5
   */
  default Promise<T> retry(int maxAttempts, Duration delay, @NonBlocking BiAction<? super Integer, ? super Throwable> onError) {
    Promise<Duration> delayPromise = Promise.value(delay);
    return retry(maxAttempts, (i, error) ->
      Operation.of(() ->
        onError.execute(i, error)
      )
        .flatMap(delayPromise)
    );
  }

  /**
   * Causes {@code this} yielding the promised value to be retried on error, after a calculated delay.
   * <p>
   * The given function is invoked for each failure,
   * with the sequence number of the failure as the first argument and the failure exception as the second.
   * It should return the duration of time to wait before retrying.
   * To retry immediately, return a zero duration.
   * Use {@link #retry(int, Duration, BiAction)} if desiring a fixed delay between attempts.
   * <p>
   * Any exception thrown by the function – possibly the exception it receives as an argument – will
   * be propagated to the subscriber, yielding a failure.
   * This can be used to selectively retry on certain failures, but immediately fail on others.
   * <p>
   * If the promise fails {@code maxAttempts} times,
   * the given function will not be invoked and the most and the most recent exception will propagate.
   *
   * <pre class="java">{@code
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.time.Duration;
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.List;
   * import java.util.concurrent.atomic.AtomicInteger;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   private static final List<String> LOG = new LinkedList<>();
   *
   *   public static void main(String... args) throws Exception {
   *     AtomicInteger source = new AtomicInteger();
   *
   *     ExecResult<Integer> result = ExecHarness.yieldSingle(exec ->
   *       Promise.sync(source::incrementAndGet)
   *         .mapIf(i -> i < 3, i -> { throw new IllegalStateException(); })
   *         .retry(3, (i, t) -> {
   *           LOG.add("retry attempt: " + i);
   *           return Promise.value(Duration.ofMillis(500 * i));
   *         })
   *     );
   *
   *     assertEquals(Integer.valueOf(3), result.getValue());
   *     assertEquals(Arrays.asList("retry attempt: 1", "retry attempt: 2"), LOG);
   *   }
   * }
   * }</pre>
   *
   * @param maxAttempts the maximum number of times to retry
   * @param onError the error handler
   * @return a promise with a retry error handler
   * @see #retry(int, Duration, BiAction)
   * @since 1.5
   */
  default Promise<T> retry(int maxAttempts, BiFunction<? super Integer, ? super Throwable, Promise<Duration>> onError) {
    return transform(up -> down -> DefaultPromise.retryAttempt(1, maxAttempts, up, down, onError));
  }

  /**
   * Convert this promise into a {@link CompletableFuture}.
   * <p>
   * @return a {@link CompletableFuture} that will complete successfully or exceptionally on the current execution thread.
   * @since 1.6
   */
  default CompletableFuture<T> toCompletableFuture() {
    CompletableFuture<T> future = new CompletableFuture<>();
    onError(future::completeExceptionally).then(future::complete);
    return future;
  }

  /**
   * Convert a {@link CompletableFuture} into a promise.
   * <p>
   * @param future the {@link CompletableFuture} to convert into a {@link Promise}
   * @param <T> The type of the promised value
   * @return a {@link Promise} that will be consumed on the current execution thread.
   * @since 1.6
   */
  static <T> Promise<T> toPromise(CompletableFuture<T> future) {
    return async(downstream -> downstream.accept(future));
  }

}
