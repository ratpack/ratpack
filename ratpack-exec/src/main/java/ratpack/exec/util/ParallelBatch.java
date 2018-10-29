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

import ratpack.exec.ExecResult;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.exec.util.internal.DefaultParallelBatch;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.stream.TransformablePublisher;

import java.util.Arrays;
import java.util.List;

/**
 * A batch of promises to be processed, in parallel.
 * <p>
 * Parallel batches can be created via {@link #of(Iterable)}.
 * <p>
 * Each promise will be executed in a {@link Execution#fork() forked execution}.
 * The {@link #execInit(Action)} method allows each forked execution to be customised before executing the work.
 *
 * @param <T> the type of value produced by each promise in the batch
 * @since 1.4
 */
public interface ParallelBatch<T> extends Batch<T> {

  /**
   * Creates a new parallel batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a {@link ParallelBatch}
   */
  static <T> ParallelBatch<T> of(Iterable<? extends Promise<? extends T>> promises) {
    return new DefaultParallelBatch<>(promises, Action.noop());
  }

  /**
   * Creates a new parallel batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a {@link ParallelBatch}
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  static <T> ParallelBatch<T> of(Promise<? extends T>... promises) {
    return of(Arrays.asList(promises));
  }

  /**
   * Specifies an initializer for each forked execution.
   * <p>
   * The given action will be called with each execution before processing the promise.
   * This can be used to seed the execution registry.
   * <p>
   * The given function will be invoked from the execution in question, and will be executed concurrently.
   *
   * @param execInit the execution initializer
   * @return a new batch, configured to use the given initializer
   */
  ParallelBatch<T> execInit(Action<? super Execution> execInit);

  /**
   * {@inheritDoc}
   */
  @Override
  Promise<List<? extends ExecResult<T>>> yieldAll();

  /**
   * {@inheritDoc}
   * <p>
   * Multiple errors may occur due to promises being in-flight when the first error occurs.
   * Subsequent errors will be {@link Throwable#addSuppressed(Throwable)} suppressed by the first error.
   */
  @Override
  Promise<List<T>> yield();

  /**
   * {@inheritDoc}
   * <p>
   * Multiple errors may occur due to promises being in-flight when the first error occurs.
   * Subsequent errors will be {@link Throwable#addSuppressed(Throwable)} suppressed by the first error.
   * <p>
   * Note that the given function will be executed concurrently, as values become available.
   *
   * <pre class="java">{@code
   * import org.junit.Assert;
   * import ratpack.exec.Promise;
   * import ratpack.exec.util.ParallelBatch;
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
   *       ParallelBatch.of(promises)
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
   */
  @Override
  Operation forEach(BiAction<? super Integer, ? super T> consumer);

  /**
   * {@inheritDoc}
   * <p>
   * Any errors that occur after the initial will be ignored.
   */
  @Override
  TransformablePublisher<T> publisher();

}
