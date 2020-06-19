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

package ratpack.test.handling;

import ratpack.core.handling.Handler;

/**
 * Thrown when a handler under test takes too long to produce a result.
 *
 * @see RequestFixture#handle(Handler)
 */
public class HandlerTimeoutException extends RuntimeException {

  private static final long serialVersionUID = 0;

  /**
   * Constructor.
   *
   * @param handlingResult the handling result at the time of the timeout
   * @param timeoutSecs the allowed time in seconds
   */
  public HandlerTimeoutException(HandlingResult handlingResult, int timeoutSecs) {
    // need to ensure handlingResult has a good toString()
    super(String.format("handler took more than %s seconds to delegate to the next handler or send a response (result: %s)", timeoutSecs, handlingResult));
  }

}
