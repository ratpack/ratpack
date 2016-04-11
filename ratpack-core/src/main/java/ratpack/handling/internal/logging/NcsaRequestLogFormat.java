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

package ratpack.handling.internal.logging;

import ratpack.handling.RequestId;
import ratpack.handling.RequestLogger;
import ratpack.handling.RequestOutcome;
import ratpack.handling.UserId;
import ratpack.http.Request;
import ratpack.http.SentResponse;
import ratpack.http.internal.HttpHeaderConstants;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NcsaRequestLogFormat implements RequestLogger.Format {

  public static final RequestLogger.Format INSTANCE = new NcsaRequestLogFormat();

  private static final String MESSAGE_PATTERN_WITHOUT_ID = "{} {} {} [{}] \"{} /{} {}\" {} {}";
  private static final String MESSAGE_PATTERN_WITH_ID = MESSAGE_PATTERN_WITHOUT_ID + " id={}";
  private static final String NOT_AVAILABLE = "-";

  private NcsaRequestLogFormat() {
  }

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter
    .ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
    .withZone(ZoneId.systemDefault())
    .withLocale(Locale.ENGLISH);


  @Override
  public void log(RequestOutcome requestOutcome, RequestLogger.Router router) {
    Request request = requestOutcome.getRequest();
    SentResponse response = requestOutcome.getResponse();

    final String requestId = request.maybeGet(RequestId.class).map(CharSequence::toString).orElse(null);

    String pattern;
    Object[] args;
    if (requestId == null) {
      pattern = MESSAGE_PATTERN_WITHOUT_ID;
      args = new Object[9];
    } else {
      pattern = MESSAGE_PATTERN_WITH_ID;
      args = new Object[10];
    }

    args[0] = request.getRemoteAddress().getHostText();
    args[1] = NOT_AVAILABLE;
    args[2] = request.maybeGet(UserId.class).map(CharSequence::toString).orElse(NOT_AVAILABLE);
    args[3] = FORMATTER.format(request.getTimestamp());
    args[4] = request.getMethod().getName();
    args[5] = request.getPath(); // The '/' is already contained in the pattern.
    args[6] = request.getProtocol();
    args[7] = response.getStatus().getCode();

    final String responseSize;
    String contentLength = response.getHeaders().get(HttpHeaderConstants.CONTENT_LENGTH);
    if (contentLength != null) {
      responseSize = contentLength;
    } else {
      responseSize = NOT_AVAILABLE;
    }
    args[8] = responseSize;

    if (requestId != null) {
      args[9] = requestId;
    }

    router.log(requestOutcome, pattern, args);
  }

}
