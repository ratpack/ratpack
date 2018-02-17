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
   * The 1xx status codes.
   */

  Status CONTINUE = Status.of(100);
  Status SWITCHING_PROTOCOLS = Status.of(101);
  Status PROCESSING = Status.of(102);
  Status EARLY_HINTS = Status.of(103);

  /**
   * The 2xx status codes.
   */
  Status OK = Status.of(200);
  Status CREATED = Status.of(201);
  Status ACCEPTED = Status.of(202);
  Status NON_AUTHORITATIVE_INFO = Status.of(203);
  Status NO_CONTENT = Status.of(204);
  Status RESET_CONTENT = Status.of(205);
  Status PARTIAL_CONTENT = Status.of(206);
  Status MULTI_STATUS = Status.of(207);
  Status ALREADY_REPORTED = Status.of(208);
  Status IM_USED = Status.of(226);

  /**
   * The 3xx status codes.
   */
  Status MULTIPLE_CHOICES = Status.of(300);
  Status MOVED_PERMANENTLY = Status.of(301);
  Status FOUND = Status.of(302);
  Status SEE_OTHER = Status.of(303);
  Status NOT_MODIFIED = Status.of(304);
  Status USE_PROXY = Status.of(305);
  Status TEMPORARY_REDIRECT = Status.of(307);
  Status PERMANENT_REDIRECT = Status.of(308);

  /**
   * The 4xx status codes.
   */
  Status BAD_REQUEST = Status.of(400);
  Status UNAUTHORIZED = Status.of(401);
  Status PAYMENT_REQUIRED = Status.of(402);
  Status FORBIDDEN = Status.of(403);
  Status NOT_FOUND = Status.of(404);
  Status METHOD_NOT_ALLOWED = Status.of(405);
  Status NOT_ACCEPTABLE = Status.of(406);
  Status PROXY_AUTH_REQUIRED = Status.of(407);
  Status REQUEST_TIMEOUT = Status.of(408);
  Status CONFLICT = Status.of(409);
  Status GONE = Status.of(410);
  Status LENGTH_REQUIRED = Status.of(411);
  Status PRECONDITION_FAILED = Status.of(412);
  Status PAYLOAD_TOO_LARGE = Status.of(413);
  Status URI_TOO_LONG = Status.of(414);
  Status UNSUPPORTED_MEDIA_TYPE = Status.of(415);
  Status RANGE_NOT_SATISFIABLE = Status.of(416);
  Status EXPECTATION_FAILED = Status.of(417);
  Status IM_A_TEAPOT= Status.of(418);
  Status MISDIRECTED_REQUEST = Status.of(421);
  Status UNPROCESSABLE_ENTITY = Status.of(422);
  Status LOCKED = Status.of(423);
  Status FAILED_DEPENDENCY = Status.of(424);
  Status UPGRADE_REQUIRED = Status.of(426);
  Status PRECONDITION_REQUIRED = Status.of(428);
  Status TOO_MANY_REQUESTS = Status.of(429);
  Status HEADER_FIELDS_TOO_LARGE = Status.of(431);
  Status UNAVAILBLE_FOR_LEGAL_REASONS = Status.of(451);

  /**
   * The 5xx status codes.
   */
  Status INTERNAL_SERVER_ERROR = Status.of(500);
  Status NOT_IMPLEMENTED = Status.of(501);
  Status BAD_GATEWAY = Status.of(502);
  Status SERVICE_UNAVAILABLE = Status.of(503);
  Status GATEWAY_TIMEOUT = Status.of(504);
  Status HTTP_VER_NOT_SUPPORTED = Status.of(505);
  Status VARIANT_ALSO_NEGOTIATES = Status.of(506);
  Status INSUFFICIENT_STORAGE = Status.of(507);
  Status LOOP_DETECTED = Status.of(508);
  Status NOT_EXTENDED = Status.of(510);
  Status NETWORK_AUTH_REQUIRED = Status.of(511);

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
