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

import ratpack.exec.ExecResult;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.BiAction;
import ratpack.stream.TransformablePublisher;

import java.util.List;

/**
 * A batch of promises to be processed, in parallel.
 * <p>
 * Serial batches can be created via {@link Batch#serial}.
 * <p>
 * Serial batches are faster than {@link ParallelBatch parallel batches} when the batch size is small and jobs complete quickly.
 *
 * @param <T>
 * @since 1.4
 */
public interface SerialBatch<T> extends Batch<T> {

  /**
   * {@inheritDoc}
   */
  @Override
  Promise<List<? extends ExecResult<T>>> yieldAll();

  /**
   * {@inheritDoc}
   */
  @Override
  Promise<List<? extends T>> yield();

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
