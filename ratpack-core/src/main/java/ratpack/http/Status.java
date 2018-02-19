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
   * The 100 status code.
   * @since 1.6
   */
  Status CONTINUE = Status.of(100);

  /**
   * The 101 status code.
   * @since 1.6
   */
  Status SWITCHING_PROTOCOLS = Status.of(101);

  /**
   * The 102 status code.
   * @since 1.6
   */
  Status PROCESSING = Status.of(102);

  /**
   * The 103 status code.
   * @since 1.6
   */
  Status EARLY_HINTS = Status.of(103);

  /**
   * The 200 status code.
   * @since 1.4
   */
  Status OK = Status.of(200);

  /**
   * The 201 status code.
   * @since 1.6
   */
  Status CREATED = Status.of(201);

  /**
   * The 202 status code.
   * @since 1.6
   */
  Status ACCEPTED = Status.of(202);

  /**
   * The 203 status code.
   * @since 1.6
   */
  Status NON_AUTHORITATIVE_INFO = Status.of(203);

  /**
   * The 204 status code.
   * @since 1.6
   */
  Status NO_CONTENT = Status.of(204);

  /**
   * The 205 status code.
   * @since 1.6
   */
  Status RESET_CONTENT = Status.of(205);

  /**
   * The 206 status code.
   * @since 1.6
   */
  Status PARTIAL_CONTENT = Status.of(206);

  /**
   * The 207 status code.
   * @since 1.6
   */
  Status MULTI_STATUS = Status.of(207);

  /**
   * The 208 status code.
   * @since 1.6
   */
  Status ALREADY_REPORTED = Status.of(208);

  /**
   * The 226 status code.
   * @since 1.6
   */
  Status IM_USED = Status.of(226);

  /**
   * The 300 status code.
   * @since 1.6
   */
  Status MULTIPLE_CHOICES = Status.of(300);

  /**
   * The 301 status code.
   * @since 1.6
   */
  Status MOVED_PERMANENTLY = Status.of(301);

  /**
   * The 302 status code.
   * @since 1.6
   */
  Status FOUND = Status.of(302);

  /**
   * The 303 status code.
   * @since 1.6
   */
  Status SEE_OTHER = Status.of(303);

  /**
   * The 304 status code.
   * @since 1.4
   */
  Status NOT_MODIFIED = Status.of(304);

  /**
   * The 505 status code.
   * @since 1.6
   */
  Status USE_PROXY = Status.of(305);

  /**
   * The 307 status code.
   * @since 1.6
   */
  Status TEMPORARY_REDIRECT = Status.of(307);

  /**
   * The 308 status code.
   * @since 1.6
   */
  Status PERMANENT_REDIRECT = Status.of(308);

  /**
   * The 400 status code.
   * @since 1.6
   */
  Status BAD_REQUEST = Status.of(400);

  /**
   * The 401 status code.
   * @since 1.6
   */
  Status UNAUTHORIZED = Status.of(401);

  /**
   * The 402 status code.
   * @since 1.6
   */
  Status PAYMENT_REQUIRED = Status.of(402);

  /**
   * The 403 status code.
   * @since 1.6
   */
  Status FORBIDDEN = Status.of(403);

  /**
   * The 404 status code.
   * @since 1.6
   */
  Status NOT_FOUND = Status.of(404);

  /**
   * The 405 status code.
   * @since 1.6
   */
  Status METHOD_NOT_ALLOWED = Status.of(405);

  /**
   * The 406 status code.
   * @since 1.6
   */
  Status NOT_ACCEPTABLE = Status.of(406);

  /**
   * The 407 status code.
   * @since 1.6
   */
  Status PROXY_AUTH_REQUIRED = Status.of(407);

  /**
   * The 408 status code.
   * @since 1.6
   */
  Status REQUEST_TIMEOUT = Status.of(408);

  /**
   * The 409 status code.
   * @since 1.6
   */
  Status CONFLICT = Status.of(409);

  /**
   * The 410 status code.
   * @since 1.6
   */
  Status GONE = Status.of(410);

  /**
   * The 411 status code.
   * @since 1.6
   */
  Status LENGTH_REQUIRED = Status.of(411);

  /**
   * The 412 status code.
   * @since 1.6
   */
  Status PRECONDITION_FAILED = Status.of(412);

  /**
   * The 413 status code.
   * @since 1.6
   */
  Status PAYLOAD_TOO_LARGE = Status.of(413);

  /**
   * The 414 status code.
   * @since 1.6
   */
  Status URI_TOO_LONG = Status.of(414);

  /**
   * The 415 status code.
   * @since 1.6
   */
  Status UNSUPPORTED_MEDIA_TYPE = Status.of(415);

  /**
   * The 416 status code.
   * @since 1.6
   */
  Status RANGE_NOT_SATISFIABLE = Status.of(416);

  /**
   * The 417 status code.
   * @since 1.6
   */
  Status EXPECTATION_FAILED = Status.of(417);

  /**
   * The 418 status code.
   * @since 1.6
   */
  Status IM_A_TEAPOT= Status.of(418);

  /**
   * The 421 status code.
   * @since 1.6
   */
  Status MISDIRECTED_REQUEST = Status.of(421);

  /**
   * The 422 status code.
   * @since 1.6
   */
  Status UNPROCESSABLE_ENTITY = Status.of(422);

  /**
   * The 423 status code.
   * @since 1.6
   */
  Status LOCKED = Status.of(423);

  /**
   * The 424 status code.
   * @since 1.6
   */
  Status FAILED_DEPENDENCY = Status.of(424);

  /**
   * The 426 status code.
   * @since 1.6
   */
  Status UPGRADE_REQUIRED = Status.of(426);

  /**
   * The 428 status code.
   * @since 1.6
   */
  Status PRECONDITION_REQUIRED = Status.of(428);

  /**
   * The 429 status code.
   * @since 1.6
   */
  Status TOO_MANY_REQUESTS = Status.of(429);

  /**
   * The 431 status code.
   * @since 1.6
   */
  Status HEADER_FIELDS_TOO_LARGE = Status.of(431);

  /**
   * The 451 status code.
   * @since 1.6
   */
  Status UNAVAILBLE_FOR_LEGAL_REASONS = Status.of(451);

  /**
   * The 500 status code.
   * @since 1.6
   */
  Status INTERNAL_SERVER_ERROR = Status.of(500);

  /**
   * The 501 status code.
   * @since 1.6
   */
  Status NOT_IMPLEMENTED = Status.of(501);

  /**
   * The 502 status code.
   * @since 1.6
   */
  Status BAD_GATEWAY = Status.of(502);

  /**
   * The 503 status code.
   * @since 1.6
   */
  Status SERVICE_UNAVAILABLE = Status.of(503);

  /**
   * The 504 status code.
   * @since 1.6
   */
  Status GATEWAY_TIMEOUT = Status.of(504);

  /**
   * The 505 status code.
   * @since 1.6
   */
  Status HTTP_VER_NOT_SUPPORTED = Status.of(505);

  /**
   * The 506 status code.
   * @since 1.6
   */
  Status VARIANT_ALSO_NEGOTIATES = Status.of(506);

  /**
   * The 507 status code.
   * @since 1.6
   */
  Status INSUFFICIENT_STORAGE = Status.of(507);

  /**
   * The 508 status code.
   * @since 1.6
   */
  Status LOOP_DETECTED = Status.of(508);

  /**
   * The 510 status code.
   * @since 1.6
   */
  Status NOT_EXTENDED = Status.of(510);

  /**
   * The 511 status code.
   * @since 1.6
   */
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
