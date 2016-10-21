/*
 * Copyright 2016 the original author or authors.
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

package ratpack.http.client;

/**
 * Thrown when the read timeout duration for a request is reached.
 * <p>
 * This will occur if the server fails to send any data back in response within the given duration,
 * since the last time any data was received.
 *
 * @since 1.4.1
 */
public class HttpClientReadTimeoutException extends RuntimeException {

  /**
   * Constructor.
   *
   * @param message the exception message
   */
  public HttpClientReadTimeoutException(String message) {
    super(message);
  }
}
