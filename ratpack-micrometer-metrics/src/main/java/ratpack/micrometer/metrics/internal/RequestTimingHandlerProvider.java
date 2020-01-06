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

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Provide an instance of a request timing handler.
 */
public class RequestTimingHandlerProvider implements Provider<RequestTimingHandler> {

  private final MeterRegistry meterRegistry;
  private final MicrometerMetricsConfig config;

  @Inject
  public RequestTimingHandlerProvider(MeterRegistry meterRegistry, MicrometerMetricsConfig config) {
    this.meterRegistry = meterRegistry;
    this.config = config;
  }

  @Override
  public RequestTimingHandler get() {
    return config.isRequestTimingMetrics() ? new DefaultRequestTimingHandler(config, meterRegistry) : Context::next;
  }
}
