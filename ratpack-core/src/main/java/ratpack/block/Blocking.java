/*
 * Copyright 2013 the original author or authors.
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

package ratpack.block;

import ratpack.api.NonBlocking;
import ratpack.util.Action;

import java.util.concurrent.Callable;

/**
 * Allows blocking operations to be executed off of a main request handling thread.
 * <p>
 * See {@link ratpack.handling.Context#getBlocking()}.
 */
public interface Blocking {

  /**
   * Execute the given operation in a background thread pool, avoiding blocking on a request handling thread.
   *
   * @param operation The operation to perform
   * @param <T> The type of result object that the operation produces
   * @return A fluent style builder for specifying how to process the result and optionally how to deal with errors
   */
  <T> SuccessOrError<T> exec(Callable<T> operation);

  /**
   * A builder for specifying the result handling strategy for a blocking operation.
   *
   * @param <T> The type of result object that the operation produces
   */
  interface SuccessOrError<T> extends Success<T> {

    /**
     * Specifies the action to take should an error occur during the blocking operation.
     *
     * @param errorHandler The action to take
     * @return A builder for specifying the action to take when the blocking operation succeeds
     */
    Success<T> onError(Action<? super Throwable> errorHandler);
  }

  /**
   * A builder for specifying the result handling strategy for a blocking operation that succeeds.
   *
   * @param <T> The type of result object that the operation produces
   */
  interface Success<T> {

    /**
     * Specifies the success handler, and actually starts the process of having the blocking operation execute.
     *
     * @param then The action to process the result of the blocking operation.
     */
    @NonBlocking
    void then(Action<? super T> then);
  }

}
