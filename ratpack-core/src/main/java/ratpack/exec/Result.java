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

import ratpack.exec.internal.DefaultResult;
import ratpack.util.ExceptionUtils;

/**
 * The result of an asynchronous operation, which may be a failure.
 *
 * @param <T> The type of the successful result object
 */
public interface Result<T> {

  /**
   * Creates a new successful result.
   *
   * @param value the result value
   * @param <T> the type of the result
   * @return the success result
   */
  static <T> Result<T> success(T value) {
    return new DefaultResult<>(value);
  }

  /**
   * Creates a new failure result.
   *
   * @param failure the failure
   * @param <T> the type of the result
   * @return the failure result
   */
  static <T> Result<T> failure(Throwable failure) {
    return new DefaultResult<>(failure);
  }

  /**
   * The failure exception.
   *
   * @return The failure exception, or null if the result was not failure.
   */
  Throwable getThrowable();

  /**
   * The result value.
   *
   * @return The result value, or null if the result was not success.
   */
  T getValue();

  /**
   * True if this was a success result.
   *
   * @return whether the result is success.
   */
  boolean isSuccess();

  /**
   * True if this was a failure result.
   *
   * @return whether the result is failure.
   */
  boolean isFailure();

  /**
   * Returns the value if this is a success result, or throws the exception if it's a failure.
   *
   * @return the value (if this is a success result)
   * @throws Exception the failure (if this is a failure result)
   */
  default T getValueOrThrow() throws Exception {
    if (isFailure()) {
      throw ExceptionUtils.toException(getThrowable());
    } else {
      return getValue();
    }
  }

}
