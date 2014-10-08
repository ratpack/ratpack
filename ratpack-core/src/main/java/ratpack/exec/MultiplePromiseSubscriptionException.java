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

/**
 * Thrown when a single use promise is subscribed to more than once.
 * <p>
 * Promises are generally single use.
 * An exception to this general rule are the promises returned by the {@link PromiseOperations#cache()} method.
 * <p>
 * There is no reason for applications to throw this exception or try and catch it.
 */
public class MultiplePromiseSubscriptionException extends IllegalStateException {

  /**
   * Constructor.
   */
  public MultiplePromiseSubscriptionException() {
    super("this promise has already been subscribed - its methods can only be called once");
  }

}
