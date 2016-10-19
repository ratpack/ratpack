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

package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.dropwizard.metrics.RequestTimingHandler;
import ratpack.handling.Context;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Provide an instance of a request timing handler.
 *
 * @since 1.2
 */
public class RequestTimingHandlerProvider implements Provider<RequestTimingHandler> {

  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;

  @Inject
  public RequestTimingHandlerProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public RequestTimingHandler get() {
    return config.isRequestTimingMetrics() ? new DefaultRequestTimingHandler(metricRegistry, config) : Context::next;
  }
}
