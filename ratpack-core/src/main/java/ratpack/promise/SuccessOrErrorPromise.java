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

package ratpack.promise;

import ratpack.util.Action;

/**
 * A promise of an outcome that may be a successful result or an error exception.
 *
 * @param <T> The type of result object that the operation produces
 */
public interface SuccessOrErrorPromise<T> extends SuccessPromise<T> {

  /**
   * Specifies the action to take if the an error occurs trying to produce the promised value.
   *
   * @param errorHandler the action to take if an error occurs
   * @return A promise for the successful result
   */
  SuccessPromise<T> onError(Action<? super Throwable> errorHandler);

}
