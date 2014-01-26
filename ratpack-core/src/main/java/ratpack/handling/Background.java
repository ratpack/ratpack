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

package ratpack.handling;

import ratpack.promise.SuccessOrErrorPromise;

import java.util.concurrent.Callable;

/**
 * Allows blocking operations to be executed off of a main request handling thread.
 * <p>
 * See {@link ratpack.handling.Context#getBackground()}.
 */
public interface Background {

  /**
   * Execute the given operation in a background thread pool, avoiding blocking on a request handling thread.
   *
   * @param operation The operation to perform
   * @param <T> The type of result object that the operation produces
   * @return A fluent style builder for specifying how to process the result and optionally how to deal with errors
   */
  <T> SuccessOrErrorPromise<T> exec(Callable<T> operation);

}
