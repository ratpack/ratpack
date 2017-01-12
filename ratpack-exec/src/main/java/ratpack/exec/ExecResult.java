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

import ratpack.exec.internal.CompleteExecResult;
import ratpack.exec.internal.ResultBackedExecResult;
import ratpack.registry.Registry;

/**
 * The result of an execution.
 *
 * @param <T> the type of promised value
 */
public interface ExecResult<T> extends Result<T> {

  /**
   * Is the result that the execution completed without a value being returned.
   * <p>
   * This can happen if the promise was {@link ratpack.exec.Promise#route(ratpack.func.Predicate, ratpack.func.Action) routed} instead of being returned to the caller,
   * or if the promise failed and the error was handled “upstream”.
   * In such a case, the code under test handled the promised value.
   * As the promise producing code dealt with the value, the tests will have to examine the side affects of this occurring.
   *
   * @return true if the execution the promise was bound to completed before the promised value was provided.
   */
  boolean isComplete();

  /**
   * Deprecated. Do not use.
   *
   * @return the execution registry.
   * @throws UnsupportedOperationException always
   */
  @Deprecated
  default Registry getRegistry() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("ExecResult.getRegistry() has been removed.");
  }

  /**
   * Wraps the given result as an exec result.
   *
   * @param result the result
   * @param <T> the type of value
   * @return the given result as an exec result
   * @since 1.2
   */
  static <T> ExecResult<T> of(Result<T> result) {
    return new ResultBackedExecResult<>(result);
  }

  /**
   * Returns a complete exec result.
   *
   * @param <T> the type of value
   * @return a complete exec result
   * @since 1.2
   */
  static <T> ExecResult<T> complete() {
    return CompleteExecResult.get();
  }

}
