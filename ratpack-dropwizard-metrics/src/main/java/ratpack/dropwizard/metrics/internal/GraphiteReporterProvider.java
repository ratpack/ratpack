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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.dropwizard.metrics.GraphiteConfig;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;

/**
 * A Provider implementation that sets up a {@link GraphiteReporter} for a {@link MetricRegistry}.
 */
public class GraphiteReporterProvider implements Provider<GraphiteReporter> {

  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;

  @Inject
  public GraphiteReporterProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public GraphiteReporter get() {
    Optional<GraphiteConfig> graphite = config.getGraphite();
    boolean enabled = graphite.map(GraphiteConfig::isEnabled).orElse(false);
    if (!enabled) {
      return null;
    }
    GraphiteReporter.Builder builder = GraphiteReporter.forRegistry(metricRegistry);
    graphite.ifPresent(input -> {
      if (input.getIncludeFilter() != null || input.getExcludeFilter() != null) {
        builder.filter(new RegexMetricFilter(input.getIncludeFilter(), input.getExcludeFilter()));
      }

      if (input.getPrefix() != null) {
        builder.prefixedWith(input.getPrefix());
      }

      if (input.getDurationUnit() != null) {
        builder.convertDurationsTo(input.getDurationUnit());
      }

      if (input.getRateUnit() != null) {
        builder.convertRatesTo(input.getRateUnit());
      }

    });
    return builder.build(graphite.get().getSender());
  }

}
