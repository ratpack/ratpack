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
import org.reactivestreams.Subscription;
import ratpack.api.Nullable;
import ratpack.exec.ExecResult;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.exec.Result;
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

/**
 * A mechanism for processing batches of jobs as {@link Promise promises}.
 * <p>
 * A batch is created via the {@link #of(Iterable)} method or one of its overloads.
 * This type is effectively a builder for how to process the jobs.
 * Some methods configure how the processing is to be done, and others initiate the processing.
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
 *       List<Promise<Integer>> jobs = new ArrayList<>();
 *       for (int i = 0; i < 100; i++) {
 *         jobs.add(Promise.value(i));
 *       }
 *
 *       return Batch.of(jobs)
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
 */
public final class Batch<T> {

  private final Iterable<? extends Promise<T>> jobs;
  private final Action<? super Execution> execInit;

  private Batch(Iterable<? extends Promise<T>> jobs, @Nullable Action<? super Execution> execInit) {
    this.jobs = jobs;
    this.execInit = execInit;
  }

  /**
   * Creates a new batch of the given promises.
   * <p>
   * The given iterable will not be consumed until processing begins.
   *
   * @param jobs the batch jobs
   * @param <T> the type of item produced by each job
   * @return a new batch
   */
  public static <T> Batch<T> of(Iterable<? extends Promise<T>> jobs) {
    return new Batch<>(jobs, null);
  }

  /**
   * Creates a new batch of the given promises.
   *
   * @param jobs the batch jobs
   * @param <T> the type of item produced by each job
   * @return a new batch
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> Batch<T> of(Promise<T>... jobs) {
    return new Batch<>(Arrays.asList(jobs), null);
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
   * Specifies that the batch should be processed in parallel, and the given action should initialize each execution.
   * <p>
   * Each promise will be subscribed to in a {@link Execution#fork() forked execution}.
   * The given action will be called with each execution before processing the promise.
   * This can be used to seed the execution registry.
   *
   * @param executionInitializer the execution initializer
   * @return a parallel batch
   */
  public Batch<T> parallel(Action<? super Execution> executionInitializer) {
    return new Batch<>(jobs, executionInitializer);
  }

  /**
   * Processes the batch jobs, return {@link Result} objects for each.
   * <p>
   * Every job will be processed, regardless of any failure.
   * <p>
   * The order of the returned list corresponds to the original job order.
   * That is, it is guaranteed that the 2nd item in the list was the 2nd job specified.
   * It does not reflect the order in which jobs completed.
   *
   * @return a promise for the result of each job
   */
  public Promise<List<? extends ExecResult<T>>> yieldAll() {
    List<Promise<T>> jobs = Lists.newArrayList(this.jobs);
    if (jobs.isEmpty()) {
      return Promise.value(Collections.emptyList());
    }

    return execInit == null ? serialYieldAll(jobs) : parallelYieldAll(jobs, execInit);
  }

  /**
   * Creates a publisher that emits the job results.
   * <p>
   * Items are emitted in completion order, not source order.
   * <p>
   * Processing is effectively halted when the first error occurs.
   * If the batch is being processed in parallel, any errors that occur after the initial will be ignored.
   *
   * @return the batch as a publisher
   */
  public TransformablePublisher<T> publisher() {
    if (execInit == null) {
      return serialPublisher(jobs);
    } else {
      return parallelPublisher(jobs, execInit);
    }
  }

  private TransformablePublisher<T> parallelPublisher(Iterable<? extends Promise<T>> jobs, Action<? super Execution> execInit) {
    Iterator<? extends Promise<T>> iterator = jobs.iterator();
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
              Promise<T> job = iterator.next();
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
                .start(e -> job.onError(write::error).then(write::item));
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

  private TransformablePublisher<T> serialPublisher(Iterable<? extends Promise<T>> jobs) {
    Iterator<? extends Promise<T>> iterator = jobs.iterator();
    return Streams.flatYield(r -> {
      if (iterator.hasNext()) {
        return iterator.next();
      } else {
        return Promise.value(null);
      }
    });
  }

  private static <T> Promise<List<? extends ExecResult<T>>> serialYieldAll(List<Promise<T>> jobs) {
    List<ExecResult<T>> results = Types.cast(jobs);
    int lastIndex = results.size() - 1;
    return Promise.async(d ->
      yieldJob(jobs, 0, new BiAction<Integer, ExecResult<T>>() {
        @Override
        public void execute(Integer i, ExecResult<T> t) throws Exception {
          results.set(i, t);
          if (i == lastIndex) {
            d.success(results);
          } else {
            yieldJob(jobs, i + 1, this);
          }
        }
      })
    );
  }

  private static <T> void yieldJob(List<Promise<T>> jobs, int i, BiAction<Integer, ? super ExecResult<T>> withItem) {
    jobs.get(i).result(r -> withItem.execute(i, r));
  }

  private static <T> Promise<List<? extends ExecResult<T>>> parallelYieldAll(List<? extends Promise<T>> jobs, Action<? super Execution> init) {
    List<ExecResult<T>> results = Types.cast(jobs);
    AtomicInteger counter = new AtomicInteger(jobs.size());

    return Promise.async(d -> {
      for (int i = 0; i < jobs.size(); ++i) {
        final int finalI = i;
        Execution.fork()
          .onStart(init)
          .onComplete(e -> {
            if (counter.decrementAndGet() == 0) {
              d.success(results);
            }
          })
          .start(e ->
            jobs.get(finalI).result(t -> {
              results.set(finalI, t);
            })
          );
      }
    });
  }


}
