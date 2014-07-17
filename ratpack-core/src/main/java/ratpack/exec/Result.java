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

package ratpack.exec;

/**
 * The result of an asynchronous operation, which may be a failure.
 *
 * @param <T> The type of the successful result object
 */
public class Result<T> {

  private final Throwable failure;
  private final T value;

  /**
   * Creates a successful result object.
   *
   * @param <T> The type of the result object
   * @param value The object representing the result of the operation
   * @return a success result
   */
  public static <T> Result<T> success(T value) {
    return new Result<>(value);
  }

  /**
   * Creates a failure result object.
   *
   * @param <T> The type of the result object
   * @param failure An exception representing the failure
   * @return a failure result
   */
  public static <T> Result<T> failure(Throwable failure) {
    return new Result<>(failure);
  }

  private Result(Throwable failure) {
    this.failure = failure;
    this.value = null;
  }

  private Result(T value) {
    this.value = value;
    this.failure = null;
  }

  /**
   * The failure exception.
   *
   * @return The failure exception, or null if the result was not failure.
   */
  public Throwable getFailure() {
    return failure;
  }

  /**
   * The result value.
   *
   * @return The result value, or null if the result was not success.
   */
  public T getValue() {
    return value;
  }

  /**
   * True if this was a success result.
   *
   * @return whether the result is success.
   */
  public boolean isSuccess() {
    return failure == null;
  }

  /**
   * True if this was a failure result.
   *
   * @return whether the result is failure.
   */
  public boolean isFailure() {
    return failure != null;
  }

  /**
   * Returns the value if this is a success result, or throws the exception if it's a failure.
   *
   * @return the value (if this is a success result)
   * @throws Throwable the failure (if this is a failure result)
   */
  public T getValueOrThrow() throws Throwable {
    if (isFailure()) {
      throw failure;
    } else {
      return value;
    }
  }

}
