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

/**
 * A Provider implementation that sets up a {@link GraphiteReporter} for a {@link MetricRegistry}.
 */
public class GraphiteReporterProvider extends AbstractReporterProvider<GraphiteReporter, GraphiteConfig> {

  @Inject
  public GraphiteReporterProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    super(metricRegistry, config, DropwizardMetricsConfig::getGraphite);
  }

  @Override
  protected GraphiteReporter build(GraphiteConfig graphite) {
    GraphiteReporter.Builder builder = GraphiteReporter.forRegistry(metricRegistry);
    if (graphite.getIncludeFilter() != null || graphite.getExcludeFilter() != null) {
      builder.filter(new RegexMetricFilter(graphite.getIncludeFilter(), graphite.getExcludeFilter()));
    }

    if (graphite.getPrefix() != null) {
      builder.prefixedWith(graphite.getPrefix());
    }

    if (graphite.getDurationUnit() != null) {
      builder.convertDurationsTo(graphite.getDurationUnit());
    }

    if (graphite.getRateUnit() != null) {
      builder.convertRatesTo(graphite.getRateUnit());
    }

    return builder.build(graphite.getSender());
  }

}
