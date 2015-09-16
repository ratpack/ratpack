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

package ratpack.handling;

import ratpack.http.Request;
import ratpack.http.SentResponse;

import java.time.Duration;
import java.time.Instant;

/**
 * The outcome of processing a request.
 *
 * @see Context#onClose(ratpack.func.Action)
 */
@SuppressWarnings("UnusedDeclaration")
public interface RequestOutcome {

  /**
   * The request.
   *
   * @return the request
   */
  Request getRequest();

  /**
   * The response.
   *
   * @return the response
   */
  SentResponse getResponse();

  /**
   * The time at when this request was dealt with from the application's point of view.
   * <p>
   * After this instant, it may have taken further time to actually get the response bytes to the client.
   * That is, this timestamp effectively denotes when application processing of the request finished.
   *
   * @return the instant at which the response was sent
   */
  Instant getSentAt();

  /**
   * The amount of elapsed time between {@link Request#getTimestamp()} and {@link #getSentAt()}.
   * <p>
   * This is the wall clock time, not the CPU time.
   * It does not include the time taken to send the response bytes to the client.
   *
   * @return how long it took to process the request
   */
  default Duration getDuration() {
    return Duration.between(getRequest().getTimestamp(), getSentAt());
  }

}
