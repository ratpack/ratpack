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
import ratpack.util.Exceptions;

/**
 * The result of an asynchronous operation, which may be an error.
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
   * Creates a new error result.
   *
   * @param error the error
   * @param <T> the type of the result
   * @return the error result
   */
  static <T> Result<T> error(Throwable error) {
    return new DefaultResult<>(error);
  }

  /**
   * The error exception.
   *
   * @return The error exception, or null if the result was not an error.
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
   * True if this was an error result.
   *
   * @return whether the result is an error.
   */
  boolean isError();

  /**
   * Returns the value if this is a success result, or throws the exception if it's an error.
   *
   * @return the value (if this is a success result)
   * @throws Exception the error (if this is an error result)
   */
  default T getValueOrThrow() throws Exception {
    if (isError()) {
      throw Exceptions.toException(getThrowable());
    } else {
      return getValue();
    }
  }

}
