/*
 * Copyright 2018 the original author or authors.
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
package ratpack.micrometer.internal;

import io.micrometer.core.instrument.MeterRegistry;
import ratpack.handling.Context;
import ratpack.micrometer.MicrometerConfig;
import ratpack.micrometer.RequestTimingHandler;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultRequestTimingHandler implements RequestTimingHandler {
  private final MeterRegistry meterRegistry;
  private final MicrometerConfig metricsConfig;

  @Inject
  public DefaultRequestTimingHandler(MeterRegistry meterRegistry,
                                     MicrometerConfig metricsConfig) {
    this.meterRegistry = meterRegistry;
    this.metricsConfig = metricsConfig;
  }

  @Override
  public void handle(Context ctx) {
    ctx.onClose((outcome) -> {
        String statusCode = String.valueOf(outcome.getResponse().getStatus().getCode());
        meterRegistry.timer("http.requests",
          "status", statusCode,
          "path", findPathGroup(outcome.getRequest().getPath()),
          "method", outcome.getRequest().getMethod().getName().toLowerCase())
          .record(outcome.getDuration().toNanos(), TimeUnit.NANOSECONDS);

        meterRegistry.timer("http.server.requests", "status", statusCode).record(outcome.getDuration().toNanos(), TimeUnit.NANOSECONDS);
      }
    );
    ctx.next();
  }

  private String findPathGroup(String requestPath) {
    String tagName = "".equals(requestPath) ? "root" : requestPath;

    for (Map.Entry<String, String> metricGrouping : metricsConfig.groups.entrySet()) {
      Pattern pattern = Pattern.compile(metricGrouping.getValue());
      Matcher match = pattern.matcher(requestPath);

      if (requestPath.matches(metricGrouping.getValue())) {
        tagName = metricGrouping.getKey();
      }

      if (match.groupCount() > 1) {
        while (match.find()) {
          for (int index = 1; index <= match.groupCount(); index++) {
            tagName = tagName.replace("$" + index, match.group(index));
          }
        }
      }
    }
    return tagName;
  }
}

