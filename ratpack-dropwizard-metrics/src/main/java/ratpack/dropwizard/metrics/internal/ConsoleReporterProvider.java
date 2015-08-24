/*
 * Copyright 2013 the original author or authors.
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

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A Provider implementation that sets up a {@link ConsoleReporter} for a {@link MetricRegistry}.
 */
public class ConsoleReporterProvider implements Provider<ConsoleReporter> {
  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;

  @Inject
  public ConsoleReporterProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public ConsoleReporter get() {
    ConsoleReporter.Builder builder = ConsoleReporter.forRegistry(metricRegistry);
    config.getConsole().ifPresent(console -> {
      if (console.getIncludeFilter() != null || console.getExcludeFilter() != null) {
        builder.filter(new RegexMetricFilter(console.getIncludeFilter(), console.getExcludeFilter()));
      }
    });
    return builder.build();
  }

}
