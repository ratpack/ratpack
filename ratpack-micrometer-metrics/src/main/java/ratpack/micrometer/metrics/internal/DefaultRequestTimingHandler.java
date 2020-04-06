/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics.internal;

import io.micrometer.core.instrument.MeterRegistry;
import ratpack.handling.Context;
import ratpack.micrometer.metrics.MicrometerMetricsConfig;
import ratpack.micrometer.metrics.RequestTimingHandler;

import java.util.concurrent.TimeUnit;

public class DefaultRequestTimingHandler implements RequestTimingHandler {
  private final MeterRegistry meterRegistry;
  private final MicrometerMetricsConfig config;

  DefaultRequestTimingHandler(MeterRegistry meterRegistry, MicrometerMetricsConfig config) {
    this.meterRegistry = meterRegistry;
    this.config = config;
  }

  @Override
  public void handle(final Context context) {
    context.onClose(outcome -> meterRegistry
      .timer("http.server.requests", config.getHandlerTags().apply(context, null))
      .record(outcome.getDuration().toNanos(), TimeUnit.NANOSECONDS));
    context.next();
  }
}
