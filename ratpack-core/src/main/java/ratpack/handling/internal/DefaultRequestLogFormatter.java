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

import ratpack.handling.RequestId;
import ratpack.handling.RequestLogFormatter;
import ratpack.handling.RequestOutcome;
import ratpack.http.Request;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DefaultRequestLogFormatter implements RequestLogFormatter {

  private static final String DEFAULT_FORMAT = "dd/MMM/yyyy:HH:mm:ss Z";
  private ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<>();

  @Override
  public String format(Request request, RequestOutcome outcome) {
    StringBuilder logLine = new StringBuilder()
      .append(request.getRemoteAddress().getHostText()).append(" ") //client IP
      .append("-").append(" ") //RFC 1413 user-identifier
      .append("-").append(" ") //userid //TODO
      .append("[").append(timestamp()).append("]").append(" ") //timestamp
      .append("\"")
      .append(request.getMethod().getName()).append(" ") //Request Method
      .append("/").append(request.getPath()).append(" ") //Request Path
      .append(request.getProtocol()) //Request HTTP Protocol
      .append("\" ")
      .append(outcome.getResponse().getStatus().getCode()).append(" ") //Response code
      .append("-"); //Response size //TODO

    request.maybeGet(RequestId.class).ifPresent(id1 -> {
      logLine.append(" id=");
      logLine.append(id1.toString());
    });
    return logLine.toString();
  }

  private String timestamp() {
    if (formatter.get() == null) {
      SimpleDateFormat format = new SimpleDateFormat(DEFAULT_FORMAT);
      //TODO is this sufficient? It logs with whatever timezone the JVM is set with. Would people want these to be different?
      format.setTimeZone(TimeZone.getDefault());
      formatter.set(format);
    }
    return formatter.get().format(new Date()); //TODO Should we get this date from somewhere else?
  }
}
