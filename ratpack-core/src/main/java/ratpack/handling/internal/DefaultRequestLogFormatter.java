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

public class DefaultRequestLogFormatter implements RequestLogFormatter {

  @Override
  public String format(Request request, RequestOutcome outcome) {
    StringBuilder logLine = new StringBuilder()
      .append(request.getMethod().toString())
      .append(" ")
      .append(request.getUri())
      .append(" ")
      .append(outcome.getResponse().getStatus().getCode());

    request.maybeGet(RequestId.class).ifPresent(id1 -> {
      logLine.append(" id=");
      logLine.append(id1.toString());
    });
    return logLine.toString();
  }
}
