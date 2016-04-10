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

public class NcsaRequestLogFormatter implements RequestLogger.Formatter {

  public static final RequestLogger.Formatter INSTANCE = new NcsaRequestLogFormatter();

  private NcsaRequestLogFormatter() {
  }

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter
    .ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
    .withZone(ZoneId.systemDefault())
    .withLocale(Locale.ENGLISH);

  @Override
  public String format(RequestOutcome outcome) {
    StringBuilder builder = new StringBuilder();

    Request request = outcome.getRequest();
    SentResponse response = outcome.getResponse();

    builder.append(request.getRemoteAddress());
    builder.append(" - ");

    builder.append(request.maybeGet(UserId.class).map(Object::toString).orElse("-"));
    builder.append(" ");

    builder.append("[");
    builder.append(FORMATTER.format(request.getTimestamp()));
    builder.append("] \"");

    builder.append(request.getMethod().getName());
    builder.append(" /");

    builder.append(request.getRawUri());
    builder.append(" ");

    builder.append(request.getProtocol());
    builder.append("\" ");

    builder.append(response.getStatus().getCode());
    builder.append(" ");

    String contentLength = response.getHeaders().get(HttpHeaderConstants.CONTENT_LENGTH);
    if (contentLength == null) {
      builder.append("-");
    } else {
      builder.append(contentLength);
    }

    request.maybeGet(RequestId.class).ifPresent(id -> {
      builder.append(" id=");
      builder.append(id);
    });

    return builder.toString();
  }

}
