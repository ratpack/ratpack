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

package ratpack.exec.batch;

import org.reactivestreams.Publisher;
import ratpack.exec.ExecResult;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.exec.batch.internal.DefaultParallelBatch;
import ratpack.exec.batch.internal.DefaultSerialBatch;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

import java.util.Arrays;
import java.util.List;

/**
 * A batch of promises to be processed.
 * <p>
 * A batch is created via the {@link #serial} or {@link #parallel} methods.
 * For precise semantics of batch processing methods, consult {@link SerialBatch} and {@link ParallelBatch}.
 *
 * <pre class="java">{@code
 * import ratpack.exec.Promise;
 * import ratpack.exec.batch.Batch;
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
 *       return Batch.parallel(promises)
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
 * @param <T>
 * @since 1.4
 */
public interface Batch<T> {

  /**
   * Creates a new serial batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a {@link SerialBatch}
   */
  static <T> SerialBatch<T> serial(Iterable<? extends Promise<T>> promises) {
    return new DefaultSerialBatch<>(promises);
  }

  /**
   * Creates a new serial batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a {@link SerialBatch}
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  static <T> Batch<T> serial(Promise<T>... promises) {
    return serial(Arrays.asList(promises));
  }

  /**
   * Creates a new parallel batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a {@link ParallelBatch}
   */
  static <T> ParallelBatch<T> parallel(Iterable<? extends Promise<T>> promises) {
    return new DefaultParallelBatch<>(promises, Action.noop());
  }

  /**
   * Creates a new serial batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a {@link ParallelBatch}
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  static <T> Batch<T> parallel(Promise<T>... promises) {
    return parallel(Arrays.asList(promises));
  }

  /**
   * Processes all the promises of the batch, collecting any errors.
   * <p>
   * This method differs from {@link #yield()} in that every promise will be processed, regardless of any failure.
   * As such, it returns {@link ExecResult} objects representing the outcome as it may be an error.
   * <p>
   * The promise returned from this method will not fail, as failure is conveyed via the result objects of the list.
   * <p>
   * The order of the entries in the promised list corresponds to the order of the promises originally.
   * That is, it is guaranteed that the 2nd item in the list was the 2nd promise specified.
   *
   * @return a promise for the result of each promise
   */
  Promise<List<? extends ExecResult<T>>> yieldAll();

  /**
   * Processes all the promises of the batch, stopping at the first error.
   * <p>
   * This method differs from {@link #yieldAll()} in that processing will be halted as soon as the first error occurs.
   * The error will be propagated through the returned promise.
   * <p>
   * The order of the entries in the promised list corresponds to the order of the promises originally.
   * That is, it is guaranteed that the 2nd item in the list was the 2nd promise specified.
   * It does not reflect the order in which promises completed.
   *
   * @return a promise for each promised value
   */
  Promise<List<? extends T>> yield();

  /**
   * Processes the promises of the batch, stopping at the first error, emitting results to the given callback.
   * <p>
   * This method is useful for aggregating or reducing the batch.
   * <p>
   * The returned operation will complete after all items have been consumed or if there is an error.
   * <p>
   * The integer value given the to consumer indicates the source position of the corresponding promise.
   *
   * @param consumer the consumer of promise values
   * @return an operation for the consumption of the values
   */
  Operation forEach(BiAction<? super Integer, ? super T> consumer);

  /**
   * Creates a publisher that emits the promised values.
   * <p>
   * This method differs to {@link #yield()} and {@link #yieldAll()} in that items are emitted as soon as they have completed.
   * As such, it is more appropriate when wanting to stream the results in some fashion.
   * <p>
   * Items are emitted in completion order, not source order.
   * <p>
   * Processing is effectively halted when the first error occurs.
   * <p>
   * The returned publisher is NOT {@link Streams#bindExec(Publisher) execution bound}.
   *
   * @return a publisher for the batch
   */
  TransformablePublisher<T> publisher();

}
