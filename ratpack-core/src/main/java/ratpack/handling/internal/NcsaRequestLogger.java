/*
 * Copyright 2015 the original author or authors.
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

package ratpack.handling.internal;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import ratpack.handling.RequestId;
import ratpack.handling.RequestLogger;
import ratpack.handling.RequestOutcome;
import ratpack.handling.UserId;
import ratpack.http.Request;
import ratpack.http.SentResponse;
import ratpack.http.internal.HttpHeaderConstants;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class NcsaRequestLogger implements RequestLogger {

  public static final String ACCESS_MARKER_NAME ="access";
  public static final String STATUS_1XX_MARKER_NAME ="status-1xx";
  public static final String STATUS_2XX_MARKER_NAME ="status-2xx";
  public static final String STATUS_3XX_MARKER_NAME ="status-3xx";
  public static final String STATUS_4XX_MARKER_NAME ="status-4xx";
  public static final String STATUS_5XX_MARKER_NAME ="status-5xx";
  public static final String STATUS_UNKNOWN_MARKER_NAME ="status-unknown-code";

  private static final Marker ACCESS_MARKER;
  private static final Marker STATUS_1XX_MARKER;
  private static final Marker STATUS_2XX_MARKER;
  private static final Marker STATUS_3XX_MARKER;
  private static final Marker STATUS_4XX_MARKER;
  private static final Marker STATUS_5XX_MARKER;
  private static final Marker STATUS_UNKNOWN_MARKER;

  static {
    // the markers below should rather be detached markers but log4j2 does not currently support those.
    // WARN Log4j does not support detached Markers. Returned Marker [access] will be unchanged.
    ACCESS_MARKER = MarkerFactory.getMarker(ACCESS_MARKER_NAME);

    STATUS_1XX_MARKER = MarkerFactory.getMarker(STATUS_1XX_MARKER_NAME);
    STATUS_1XX_MARKER.add(ACCESS_MARKER);

    STATUS_2XX_MARKER = MarkerFactory.getMarker(STATUS_2XX_MARKER_NAME);
    STATUS_2XX_MARKER.add(ACCESS_MARKER);

    STATUS_3XX_MARKER = MarkerFactory.getMarker(STATUS_3XX_MARKER_NAME);
    STATUS_3XX_MARKER.add(ACCESS_MARKER);

    STATUS_4XX_MARKER = MarkerFactory.getMarker(STATUS_4XX_MARKER_NAME);
    STATUS_4XX_MARKER.add(ACCESS_MARKER);

    STATUS_5XX_MARKER = MarkerFactory.getMarker(STATUS_5XX_MARKER_NAME);
    STATUS_5XX_MARKER.add(ACCESS_MARKER);

    STATUS_UNKNOWN_MARKER = MarkerFactory.getMarker(STATUS_UNKNOWN_MARKER_NAME);
    STATUS_UNKNOWN_MARKER.add(ACCESS_MARKER);
  }

  public static final String MESSAGE_PATTERN_WITHOUT_ID = "{} {} {} [{}] \"{} /{} {}\" {} {}";
  public static final String MESSAGE_PATTERN_WITH_ID = MESSAGE_PATTERN_WITHOUT_ID + " id={}";
  private static final String NOT_AVAILABLE = "-";

  private static Marker resolveStatusMarker(int statusCode) {
    if(statusCode < 100 || statusCode > 599) {
      // broken status code, just return STATUS_UNKNOWN_MARKER.
      return STATUS_UNKNOWN_MARKER;
    }
    if(statusCode < 200) {
      return STATUS_1XX_MARKER;
    }
    if(statusCode < 300) {
      return STATUS_2XX_MARKER;
    }
    if(statusCode < 400) {
      return STATUS_3XX_MARKER;
    }
    if(statusCode < 500) {
      return STATUS_4XX_MARKER;
    }
    return STATUS_5XX_MARKER;
  }

  private final DateTimeFormatter formatter = DateTimeFormatter
    .ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
    .withZone(ZoneId.systemDefault())
    .withLocale(Locale.ENGLISH);

  private final Logger logger;
  private final boolean loggingErrorStatusCodesAsWarn;

  public NcsaRequestLogger(Logger logger) {
    this(logger, false);
  }

  public NcsaRequestLogger(Logger logger, boolean loggingErrorStatusCodesAsWarn) {
    Objects.requireNonNull(logger, "logger must not be null!");
    this.logger = logger;
    this.loggingErrorStatusCodesAsWarn = loggingErrorStatusCodesAsWarn;
  }

  @Override
  public void log(RequestOutcome outcome) {

    SentResponse response = outcome.getResponse();
    final int statusCode = response.getStatus().getCode();
    final boolean logAtWarnLevel = loggingErrorStatusCodesAsWarn && (statusCode < 100 || statusCode > 399);

    Marker statusMarker = resolveStatusMarker(statusCode);
    if (logAtWarnLevel) {
      if(!logger.isWarnEnabled(statusMarker)) {
        return;
      }
    } else {
      if (!logger.isInfoEnabled(statusMarker)) {
        return;
      }
    }

    Request request = outcome.getRequest();

    final String remoteAddress = request.getRemoteAddress().getHostText();
    final String rfc1413Ident = NOT_AVAILABLE;
    final String userId = request.maybeGet(UserId.class).map(CharSequence::toString).orElse(NOT_AVAILABLE);
    final String dateTime = getTimestampString(request.getTimestamp());
    final String method = request.getMethod().getName();
    final String path = request.getPath(); // The '/' is already contained in the pattern.
    final String protocol = request.getProtocol();

    final String responseSize;
    String contentLength = response.getHeaders().get(HttpHeaderConstants.CONTENT_LENGTH);
    if (contentLength != null) {
      responseSize = contentLength;
    } else {
      responseSize = NOT_AVAILABLE;
    }

    final String requestId = request.maybeGet(RequestId.class).map(CharSequence::toString).orElse(null);

    if(logAtWarnLevel) {
      if (requestId == null) {
        logger.warn(statusMarker, MESSAGE_PATTERN_WITHOUT_ID, remoteAddress, rfc1413Ident, userId, dateTime, method, path, protocol, statusCode, responseSize);
      } else {
        logger.warn(statusMarker, MESSAGE_PATTERN_WITH_ID, remoteAddress, rfc1413Ident, userId, dateTime, method, path, protocol, statusCode, responseSize, requestId);
      }
    } else {
      if (requestId == null) {
        logger.info(statusMarker, MESSAGE_PATTERN_WITHOUT_ID, remoteAddress, rfc1413Ident, userId, dateTime, method, path, protocol, statusCode, responseSize);
      } else {
        logger.info(statusMarker, MESSAGE_PATTERN_WITH_ID, remoteAddress, rfc1413Ident, userId, dateTime, method, path, protocol, statusCode, responseSize, requestId);
      }
    }
  }

  /*
   * Left package-private so formatter can be tested separately.
   * Not static so testing with different locales/timezones is possible.
   */
  String getTimestampString(Instant instant) {
    return formatter.format(instant);
  }

}
