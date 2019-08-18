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
import com.codahale.metrics.jmx.JmxReporter;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A Provider implementation that sets up a {@link JmxReporter} for a {@link MetricRegistry}.
 */
public class JmxReporterProvider implements Provider<JmxReporter> {
  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;

  @Inject
  public JmxReporterProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public JmxReporter get() {
    JmxReporter.Builder builder = JmxReporter.forRegistry(metricRegistry);
    config.getJmx().ifPresent(jmx -> {
      if (jmx.getIncludeFilter() != null || jmx.getExcludeFilter() != null) {
        builder.filter(new RegexMetricFilter(jmx.getIncludeFilter(), jmx.getExcludeFilter()));
      }
    });

    return builder.build();
  }

}
