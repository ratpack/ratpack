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

package org.ratpackframework.util.internal;

/**
 * The result of an asynchronous operation, which may be a failure.
 *
 * @param <T> The type of the successful result object.
 */
public class Result<T> {

  private final Exception failure;
  private final T value;

  /**
   * Creates a failure result object.
   *
   * @param failure An exception representing the failure
   */
  public Result(Exception failure) {
    this.failure = failure;
    this.value = null;
  }

  /**
   * Creates a successful result object.
   *
   * @param value The object representing the result of the operation.
   * @param <Y> The type of the result object.
   */
  public <Y extends T> Result(Y value) {
    this.value = value;
    this.failure = null;
  }

  /**
   * The failure exception.
   *
   * @return The failure exception, or null if the result was not failure.
   */
  public Exception getFailure() {
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

}
