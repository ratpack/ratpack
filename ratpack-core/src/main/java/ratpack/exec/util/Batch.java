/*
 * Copyright 2016 the original author or authors.
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

package ratpack.exec.util;

import com.google.common.collect.Lists;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import ratpack.api.Nullable;
import ratpack.exec.ExecResult;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.BiFunction;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.internal.BufferingPublisher;
import ratpack.util.Types;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A mechanism for processing batches of promises.
 * <p>
 * A batch is created via the {@link #of(Iterable)} method or one of its overloads.
 * This type is effectively a builder for how to yield the promised values.
 * Some methods configure how the processing is to be done, and others initiate the processing.
 * <p>
 * Parallel processing can be enabled by the {@link #parallel()} method, or one of its overloads.
 * <p>
 * Batches are immutable, reusable and are used in a fluent manner.
 *
 * <pre class="java">{@code
 * import ratpack.exec.Promise;
 * import ratpack.exec.util.Batch;
 * import ratpack.test.exec.ExecHarness;
 *
 * import java.util.ArrayList;
 * import java.util.List;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     int sum = ExecHarness.yieldSingle(e -> {
 *
 *       List<Promise<Integer>> promises = new ArrayList<>();
 *       for (int i = 0; i < 100; i++) {
 *         promises.add(Promise.value(i));
 *       }
 *
 *       return Batch.of(promises)
 *         .parallel()
 *         .publisher()
 *         .reduce(0, (r, i) -> r + i);
 *
 *     }).getValueOrThrow();
 *
 *     assertEquals(sum, 4950);
 *   }
 * }
 * }</pre>
 *
 * @param <T> the type of item in the batch
 * @since 1.4
 * @see #yieldAll()
 * @see #yield()
 * @see #parallel()
 * @see #publisher()
 */
public final class Batch<T> {

  private final Iterable<? extends Promise<T>> promises;
  private final Action<? super Execution> execInit;

  private Batch(Iterable<? extends Promise<T>> promises, @Nullable Action<? super Execution> execInit) {
    this.promises = promises;
    this.execInit = execInit;
  }

  /**
   * Creates a new batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a new batch
   */
  public static <T> Batch<T> of(Iterable<? extends Promise<T>> promises) {
    return new Batch<>(promises, null);
  }

  /**
   * Creates a new batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a new batch
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> Batch<T> of(Promise<T>... promises) {
    return new Batch<>(Arrays.asList(promises), null);
  }

  /**
   * Specifies that the batch should be processed in parallel.
   *
   * @return a parallel batch
   * @see #parallel(Action)
   */
  public Batch<T> parallel() {
    return parallel(Action.noop());
  }

  /**
   * Specifies that the batch should be processed in parallel.
   * <p>
   * Each promise will be subscribed to in a {@link Execution#fork() forked execution}.
   * The given action will be called with each execution before processing the promise.
   * This can be used to seed the execution registry.
   * <p>
   * The given action will be invoked from the execution in question, and will be executed in parallel.
   * <p>
   * This method does not initiate processing.
   * It returns a new batch object, that is configured to process in parallel.
   *
   * @param executionInitializer the execution initializer
   * @return a parallel batch
   */
  public Batch<T> parallel(Action<? super Execution> executionInitializer) {
    return new Batch<>(promises, executionInitializer);
  }

  /**
   * Processes all the promises of the batch.
   * <p>
   * This method differs from {@link #yield()} in that every promise will be processed, regardless of any failure.
   * As such, it returns {@link ExecResult} objects representing the outcome as it may be an error.
   * <p>
   * The promise returned from this method will not fail, as failure is conveyed via the result objects of the list.
   * <p>
   * The order of the entries in the promised list corresponds to the order of the promises originally.
   * That is, it is guaranteed that the 2nd item in the list was the 2nd promise specified.
   * It does not reflect the order in which promises completed.
   * <p>
   *
   * @return a promise for the result of each promise
   */
  public Promise<List<? extends ExecResult<T>>> yieldAll() {
    List<Promise<T>> promises = Lists.newArrayList(this.promises);
    if (promises.isEmpty()) {
      return Promise.value(Collections.emptyList());
    }

    return isSerial() ? serialYieldAll(promises) : parallelYieldAll(promises, execInit);
  }

  /**
   * Initiates processing of the promises, stopping at the first error, returning the results when done.
   * <p>
   * This method differs from {@link #yieldAll()} in that processing will be halted as soon as the first error occurs.
   * The error will be propagated through the returned promise.
   * <p>
   * If processing in parallel, subsequent errors may also occur due to the promises being in-flight when the first error occurred.
   * Such errors will be {@link Throwable#addSuppressed(Throwable)} suppressed by the first error.
   * <p>
   * The order of the entries in the promised list corresponds to the order of the promises originally.
   * That is, it is guaranteed that the 2nd item in the list was the 2nd promise specified.
   * It does not reflect the order in which promises completed.
   *
   * @return a promise for the result of each promise
   */
  public Promise<List<? extends T>> yield() {
    List<Promise<T>> promises = Lists.newArrayList(this.promises);
    if (promises.isEmpty()) {
      return Promise.value(Collections.emptyList());
    }

    return isSerial() ? serialYield(promises) : parallelYield(promises, execInit);
  }

  /**
   * Initiates processing of the promises, stopping at the first error, emitting results to the given callback when available.
   * <p>
   * This method is useful for aggregating or reducing the batch.
   * If processing in parallel, the consumer function may be executed concurrently.
   * Therefore, the consumer must be thread safe.
   * To serialise consumption of a parallel batch, use the {@link #publisher()} method.
   * <p>
   * The returned operation will complete after all items have been consumed or if there is an error.
   * If processing in parallel, subsequent errors may also occur due to the promises being in-flight when the first error occurred.
   * Such errors will be {@link Throwable#addSuppressed(Throwable)} suppressed by the first error.
   * <p>
   * The integer value given the to consumer indicates the source position of the corresponding promise.
   * When processing serially, this value will increment from 0 to «size - 1».
   * When processing in parallel, the value may not be monotonic as it reflects the source order and not completion order.
   *
   * <pre class="java">{@code
   * import org.junit.Assert;
   * import ratpack.exec.Promise;
   * import ratpack.exec.util.Batch;
   * import ratpack.func.Pair;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   * import java.util.Map;
   * import java.util.concurrent.ConcurrentHashMap;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     Map<String, Integer> map = new ConcurrentHashMap<>();
   *
   *     ExecHarness.runSingle(e -> {
   *       List<Promise<Pair<String, Integer>>> promises = Arrays.asList(
   *         Promise.value(Pair.of("a", 1)),
   *         Promise.value(Pair.of("b", 2)),
   *         Promise.value(Pair.of("c", 3)),
   *         Promise.value(Pair.of("d", 4))
   *       );
   *
   *       Batch.of(promises)
   *         .parallel()
   *         .forEach((i, v) -> map.put(v.left, v.right))
   *         .then();
   *     });
   *
   *     Assert.assertEquals(Integer.valueOf(1), map.get("a"));
   *     Assert.assertEquals(Integer.valueOf(2), map.get("b"));
   *     Assert.assertEquals(Integer.valueOf(3), map.get("c"));
   *     Assert.assertEquals(Integer.valueOf(4), map.get("d"));
   *   }
   * }
   * }</pre>
   *
   * @param consumer the consumer of promise values
   * @return an operation for the consumption of the values
   */
  public Operation forEach(BiAction<? super Integer, ? super T> consumer) {
    return isSerial() ? serialForEach(promises, consumer) : parallelForEach(promises, execInit, consumer);
  }

  private boolean isSerial() {
    return execInit == null;
  }

  private static <T> Operation serialForEach(Iterable<? extends Promise<T>> promises, BiAction<? super Integer, ? super T> consumer) {
    return Promise.<Void>async(d ->
      yieldPromise(promises.iterator(), 0, (i, r) -> consumer.execute(i, r.getValue()), (i, r) -> {
        d.error(r.getThrowable());
        return false;
      }, () -> d.success(null))
    ).operation();
  }

  private static <T> Operation parallelForEach(Iterable<? extends Promise<T>> promises, Action<? super Execution> execInit, BiAction<? super Integer, ? super T> consumer) {
    AtomicReference<Throwable> error = new AtomicReference<>();
    AtomicBoolean done = new AtomicBoolean();
    AtomicInteger wip = new AtomicInteger();

    return Promise.async(d -> {
      int i = 0;
      Iterator<? extends Promise<T>> iterator = promises.iterator();
      while (iterator.hasNext()) {
        Promise<T> promise = iterator.next();
        final int finalI = i++;
        wip.incrementAndGet();
        if (!iterator.hasNext()) {
          done.set(true);
        }

        Execution.fork()
          .onStart(execInit)
          .onComplete(e -> {
            if (wip.decrementAndGet() == 0 && done.get()) {
              Throwable t = error.get();
              if (t == null) {
                d.success(null);
              } else {
                d.error(t);
              }
            }
          })
          .start(e -> {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (error.get() == null) {
              promise.result(t -> {
                if (t.isError()) {
                  if (!error.compareAndSet(null, t.getThrowable())) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    error.get().addSuppressed(t.getThrowable());
                  }
                } else {
                  consumer.execute(finalI, t.getValue());
                }
              });
            }
          });
      }
      if (i == 0) {
        d.success(null);
      }
    }).operation();
  }

  private static <T> Promise<List<? extends T>> parallelYield(List<Promise<T>> promises, Action<? super Execution> execInit) {
    List<T> results = Types.cast(promises);
    return Promise.async(d -> parallelForEach(promises, execInit, results::set).onError(d::error).then(() -> d.success(results)));
  }

  private static <T> Promise<List<? extends T>> serialYield(List<? extends Promise<T>> promises) {
    List<T> results = Types.cast(promises);
    return Promise.async(d -> serialForEach(promises, results::set).onError(d::error).then(() -> d.success(results)));
  }

  /**
   * Creates a publisher that emits the promised values.
   * <p>
   * This method differs to {@link #yield()} and {@link #yieldAll()} in that items are emitted as soon as they have completed.
   * As such, it is more appropriate when wanting to stream the results in some fashion.
   * <p>
   * Items are emitted in completion order, not source order.
   * <p>
   * Processing is effectively halted when the first error occurs.
   * If the batch is being processed in parallel, any errors that occur after the initial will be ignored.
   * <p>
   * The returned publisher is NOT {@link Streams#bindExec(Publisher) execution bound}.
   *
   * @return the batch as a publisher
   */
  public TransformablePublisher<T> publisher() {
    if (isSerial()) {
      return serialPublisher(promises);
    } else {
      return parallelPublisher(promises, execInit);
    }
  }

  private TransformablePublisher<T> parallelPublisher(Iterable<? extends Promise<T>> promises, Action<? super Execution> execInit) {
    Iterator<? extends Promise<T>> iterator = promises.iterator();
    return new BufferingPublisher<>(Action.noop(), write -> {
      return new Subscription() {
        volatile boolean cancelled;
        volatile boolean complete;
        final AtomicLong finished = new AtomicLong();
        volatile long started;

        @Override
        public void request(long n) {
          while (n-- > 0 && !cancelled) {
            if (iterator.hasNext()) {
              ++started;
              Promise<T> promise = iterator.next();
              if (!iterator.hasNext()) {
                complete = true;
              }
              Execution.fork()
                .onStart(execInit)
                .onComplete(e -> {
                  long finished = this.finished.incrementAndGet();
                  if (finished == started && complete && !cancelled) {
                    write.complete();
                  }
                })
                .start(e -> promise.onError(write::error).then(write::item));
            } else {
              return;
            }
          }
        }

        @Override
        public void cancel() {
          cancelled = true;
        }
      };
    });
  }

  private TransformablePublisher<T> serialPublisher(Iterable<? extends Promise<T>> promises) {
    Iterator<? extends Promise<T>> iterator = promises.iterator();
    return Streams.flatYield(r -> {
      if (iterator.hasNext()) {
        return iterator.next();
      } else {
        return Promise.value(null);
      }
    });
  }

  private static <T> Promise<List<? extends ExecResult<T>>> serialYieldAll(List<Promise<T>> promises) {
    List<ExecResult<T>> results = Types.cast(promises);
    return Promise.async(d ->
      yieldPromise(promises.iterator(), 0,
        results::set,
        (i, r) -> {
          results.set(i, r);
          return true;
        },
        () -> d.success(results)
      )
    );
  }

  private static <T> void yieldPromise(Iterator<? extends Promise<T>> promises, int i, BiAction<Integer, ExecResult<T>> withItem, BiFunction<Integer, ExecResult<T>, Boolean> onError, Runnable onComplete) {
    if (promises.hasNext()) {
      promises.next().result(r -> {
        if (r.isError()) {
          if (!onError.apply(i, r)) {
            return;
          }
        } else {
          withItem.execute(i, r);
        }
        yieldPromise(promises, i + 1, withItem, onError, onComplete);
      });
    } else {
      onComplete.run();
    }
  }

  private static <T> Promise<List<? extends ExecResult<T>>> parallelYieldAll(List<? extends Promise<T>> promises, Action<? super Execution> init) {
    List<ExecResult<T>> results = Types.cast(promises);
    AtomicInteger counter = new AtomicInteger(promises.size());

    return Promise.async(d -> {
      for (int i = 0; i < promises.size(); ++i) {
        final int finalI = i;
        //noinspection CodeBlock2Expr
        Execution.fork()
          .onStart(init)
          .onComplete(e -> {
            if (counter.decrementAndGet() == 0) {
              d.success(results);
            }
          })
          .start(e ->
            promises.get(finalI).result(t -> {
              results.set(finalI, t);
            })
          );
      }
    });
  }

}
