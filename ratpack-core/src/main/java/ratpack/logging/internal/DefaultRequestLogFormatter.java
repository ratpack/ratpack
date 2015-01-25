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

package ratpack.logging.internal;

import ratpack.handling.RequestOutcome;
import ratpack.http.Request;
import ratpack.logging.RequestLogFormatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DefaultRequestLogFormatter implements RequestLogFormatter {

  @Override
  public String format(RequestOutcome outcome) {
    RequestData data = new RequestData(outcome);
    return String.format("%s %s %s %s %s %s %s", data.getRemoteIp(), data.getUserIdentifier(), data.getUserId(), data.getDate(), data.getRequest(), data.getStatus(), data.getSize());
  }

  private class RequestData {

    private final RequestOutcome outcome;

    public RequestData(RequestOutcome outcome) {
      this.outcome = outcome;
    }

    String getRemoteIp() {
      return outcome.getRequest().getRemoteAddress().getHostText();
    }

    String getUserIdentifier() {
      return normalize(null);
    }

    String getUserId() {
      return null;
    }

    String getDate() {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("%d/%b/%Y:%H:%M:%S %z");
      return normalize("[", "]", formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(outcome.getClosedAt()), ZoneId.systemDefault())));
    }

    String getRequest() {
      Request request = outcome.getRequest();
      return String.format("\"%s %s %s\"", request.getMethod().getName(), request.getPath(), request.getHeaders().get("version"));
    }

    String getStatus() {
      return normalize(String.valueOf(outcome.getResponse().getStatus().getCode()));
    }

    String getSize() {
      return normalize(null);
    }

    private String normalize(String value) {
      return normalize("", "", value);
    }

    private String normalize(String prefix, String suffix, String value) {
      if (value == null) {
        return "-";
      } else {
        return String.format("%s%s%s", prefix, value, suffix);
      }
    }
  }
}
