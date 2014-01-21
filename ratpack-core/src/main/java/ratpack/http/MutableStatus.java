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

package ratpack.http;

/**
 * A status line of a HTTP response.
 */
public interface MutableStatus extends Status {

  /**
   * Update the status to be the given code, and the default message for that code.
   *
   * @param code The status code
   */
  void set(int code);

  /**
   * Update the status to be the given code and message.
   *
   * @param code The status code
   * @param message The status message
   */
  void set(int code, String message);

}
