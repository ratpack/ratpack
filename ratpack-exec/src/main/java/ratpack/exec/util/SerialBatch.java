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
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.exec.util.internal.DefaultSerialBatch;
import ratpack.func.BiAction;
import ratpack.stream.TransformablePublisher;

import java.util.Arrays;
import java.util.List;

/**
 * A batch of promises to be processed, serially.
 * <p>
 * Serial batches can be created via {@link #of}.
 * <p>
 * Serial batches are faster than {@link ParallelBatch parallel batches} when the batch size is small and jobs complete quickly.
 *
 * @param <T> the type of value produced by each promise in the batch
 * @since 1.4
 */
public interface SerialBatch<T> extends Batch<T> {

  /**
   * Creates a new serial batch of the given promises.
   *
   * @param promises the promises
   * @param <T> the type of item produced by each promise
   * @return a {@link SerialBatch}
   */
  static <T> SerialBatch<T> of(Iterable<? extends Promise<? extends T>> promises) {
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
  static <T> SerialBatch<T> of(Promise<? extends T>... promises) {
    return of(Arrays.asList(promises));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  Promise<List<? extends ExecResult<T>>> yieldAll();

  /**
   * {@inheritDoc}
   */
  @Override
  Promise<List<T>> yield();

  /**
   * {@inheritDoc}
   */
  @Override
  Operation forEach(BiAction<? super Integer, ? super T> consumer);

  /**
   * {@inheritDoc}
   */
  @Override
  TransformablePublisher<T> publisher();

}
