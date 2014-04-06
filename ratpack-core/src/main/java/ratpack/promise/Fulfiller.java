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

/**
 * An object that is used to fulfill a promise.
 *
 * @param <T> the type of value that was promised.
 * @see ratpack.handling.Context#promise(ratpack.func.Action)
 */
public interface Fulfiller<T> {

  /**
   * Fulfills the promise with an error result.
   *
   * @param throwable the error result
   */
  void error(Throwable throwable);

  /**
   * Fulfills the promise with the given value.
   *
   * @param value the value to provide to the promise subscriber
   */
  void success(T value);

}
