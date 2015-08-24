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

import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.MetricRegistry;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A Provider implementation that sets up a {@link Slf4jReporter} for a {@link MetricRegistry}.
 */
public class Slf4jReporterProvider implements Provider<Slf4jReporter> {
  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;

  @Inject
  public Slf4jReporterProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public Slf4jReporter get() {
    Slf4jReporter.Builder builder = Slf4jReporter.forRegistry(metricRegistry);
    config.getSlf4j().ifPresent(slf4j -> {
      if (slf4j.getLogLevel() != null) {
        builder.withLoggingLevel(slf4j.getLogLevel());
      }
      if (slf4j.getIncludeFilter() != null || slf4j.getExcludeFilter() != null) {
        builder.filter(new RegexMetricFilter(slf4j.getIncludeFilter(), slf4j.getExcludeFilter()));
      }
      if (slf4j.getMarker() != null) {
        builder.markWith(slf4j.getMarker());
      }
      if (slf4j.getPrefix() != null) {
        builder.prefixedWith(slf4j.getPrefix());
      }
      if (slf4j.getLogger() != null) {
        builder.outputTo(slf4j.getLogger());
      }
      if (slf4j.getRateUnit() != null) {
        builder.convertRatesTo(slf4j.getRateUnit());
      }
      if (slf4j.getDurationUnit() != null) {
        builder.convertDurationsTo(slf4j.getDurationUnit());
      }
    });
    return builder.build();
  }
}
