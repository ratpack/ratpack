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
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.internal.BufferingPublisher;
import ratpack.util.Types;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

    return execInit == null ? serialYieldAll(promises) : parallelYieldAll(promises, execInit);
  }

  /**
   * Initiates processing of the promises, stopping at the first error.
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

    return execInit == null ? serialYield(promises) : parallelYield(promises, execInit);
  }

  private static <T> Promise<List<? extends T>> parallelYield(List<Promise<T>> promises, Action<? super Execution> execInit) {
    List<T> results = Types.cast(promises);
    AtomicInteger counter = new AtomicInteger(promises.size());
    AtomicReference<Throwable> error = new AtomicReference<>();
    return Promise.async(d -> {
      for (int i = 0; i < promises.size(); ++i) {
        final int finalI = i;
        Execution.fork()
          .onStart(execInit)
          .onComplete(e -> {
            if (counter.decrementAndGet() == 0) {
              Throwable t = error.get();
              if (t == null) {
                d.success(results);
              } else {
                d.error(t);
              }
            }
          })
          .start(e -> {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (error.get() == null) {
              promises.get(finalI).result(t -> {
                if (t.isError()) {
                  if (!error.compareAndSet(null, t.getThrowable())) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    error.get().addSuppressed(t.getThrowable());
                  }
                } else {
                  results.set(finalI, t.getValue());
                }
              });
            }
          });
      }
    });
  }

  private static <T> Promise<List<? extends T>> serialYield(List<Promise<T>> promises) {
    List<T> results = Types.cast(promises);
    int lastIndex = results.size() - 1;
    return Promise.async(d ->
      yieldPromise(promises, 0, new BiAction<Integer, ExecResult<T>>() {
        @Override
        public void execute(Integer i, ExecResult<T> t) throws Exception {
          if (t.isError()) {
            d.error(t.getThrowable());
          } else {
            results.set(i, t.getValue());
            if (i < lastIndex) {
              yieldPromise(promises, i + 1, this);
            } else {
              d.success(results);
            }
          }
        }
      })
    );
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
    if (execInit == null) {
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
    int lastIndex = results.size() - 1;
    return Promise.async(d ->
      yieldPromise(promises, 0, new BiAction<Integer, ExecResult<T>>() {
        @Override
        public void execute(Integer i, ExecResult<T> t) throws Exception {
          results.set(i, t);
          if (i == lastIndex) {
            d.success(results);
          } else {
            yieldPromise(promises, i + 1, this);
          }
        }
      })
    );
  }

  private static <T> void yieldPromise(List<Promise<T>> promises, int i, BiAction<Integer, ? super ExecResult<T>> withItem) {
    promises.get(i).result(r -> withItem.execute(i, r));
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
