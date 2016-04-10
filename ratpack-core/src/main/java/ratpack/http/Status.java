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

package ratpack.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import ratpack.http.internal.DefaultStatus;

/**
 * A status line of a HTTP response.
 */
public interface Status {

  /**
   * The 200 status code.
   */
  Status OK = Status.of(200);

  /**
   * Creates a new status object.
   *
   * @param code the status code
   * @param message the status message
   * @return a new status object
   */
  static Status of(int code, String message) {
    return new DefaultStatus(new HttpResponseStatus(code, message));
  }

  /**
   * Creates a new status object.
   *
   * @param code the status code
   * @return a new status object
   */
  static Status of(int code) {
    return new DefaultStatus(HttpResponseStatus.valueOf(code));
  }

  /**
   * The status code.
   *
   * @return The status code
   */
  int getCode();

  /**
   * The message of the status.
   *
   * @return The message of the status
   */
  String getMessage();

  /**
   * If {@link #getCode()} is >= 100 and < 200.
   *
   * @return {@code true} if {@link #getCode()} is >= 100 and < 200, else {@code false}
   * @since 1.3
   */
  default boolean is1xx() {
    return getCode() >= 100 && getCode() < 200;
  }

  /**
   * If {@link #getCode()} is >= 200 and < 300.
   *
   * @return {@code true} if {@link #getCode()} is >= 200 and < 300, else {@code false}
   * @since 1.3
   */
  default boolean is2xx() {
    return getCode() >= 200 && getCode() < 300;
  }

  /**
   * If {@link #getCode()} is >= 300 and < 400.
   *
   * @return {@code true} if {@link #getCode()} is >= 300 and < 400, else {@code false}
   * @since 1.3
   */
  default boolean is3xx() {
    return getCode() >= 300 && getCode() < 400;
  }

  /**
   * If {@link #getCode()} is >= 400 and < 500.
   *
   * @return {@code true} if {@link #getCode()} is >= 400 and < 500, else {@code false}
   * @since 1.3
   */
  default boolean is4xx() {
    return getCode() >= 400 && getCode() < 500;
  }

  /**
   * If {@link #getCode()} is >= 500 and < 600.
   *
   * @return {@code true} if {@link #getCode()} is >= 500 and < 600, else {@code false}
   * @since 1.3
   */
  default boolean is5xx() {
    return getCode() >= 500 && getCode() < 600;
  }

  /**
   * The status as Netty's type.
   * <p>
   * Used internally.
   *
   * @return the status as Netty's type
   */
  HttpResponseStatus getNettyStatus();
}
