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

package org.ratpackframework;

/**
 * The result of an asynchronous operation, which may be a failure.
 *
 * @param <T> The type of the successful result object.
 */
public class Result<T> {

  private final Exception failure;
  private final T value;

  public Result(Exception failure) {
    this.failure = failure;
    this.value = null;
  }

  public <Y extends T> Result(Y value) {
    this.value = value;
    this.failure = null;
  }

  public Exception getFailure() {
    return failure;
  }

  public T getValue() {
    return value;
  }

  public boolean isSuccess() {
    return failure == null;
  }

  public boolean isFailure() {
    return failure != null;
  }

}
