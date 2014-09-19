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

package ratpack.exec;

import ratpack.func.Action;

/**
 * A promise for a value that will be available later.
 * <p>
 * A promise allows what is to be done with the value to be specified without having the value available.
 * They are a common alternative to callbacks in asynchronous programming.
 *
 * <h3>One-shot</h3>
 * <p>
 * Promise instances cannot be reused.
 * Furthermore, only one method can be called on any promise instance.
 * <p>
 * Methods returning a promise (or success promise) return a new promise instance, which can also only be used once.
 * Once any method has been called on a given promise object, calling any other method will throw an exception.
 *
 * <h3>Testing</h3>
 * <p>
 * To test code that uses promises, see the {@code ratpack.test.exec.ExecHarness} class.
 *
 * @param <T> the type of promised value
 */
public interface Promise<T> extends SuccessPromise<T> {

  /**
   * Specifies the action to take if the an error occurs trying to produce the promised value.
   *
   * @param errorHandler the action to take if an error occurs
   * @return A promise for the successful result
   */
  SuccessPromise<T> onError(Action<? super Throwable> errorHandler);

  /**
   * {@inheritDoc}
   */
  @Override
  void then(Action<? super T> then);

}
