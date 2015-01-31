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

package ratpack.codahale.metrics.internal;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Provider;
import ratpack.codahale.metrics.CodaHaleMetricsModule;

import javax.inject.Inject;

/**
 * A Provider implementation that sets up a {@link JmxReporter} for a {@link MetricRegistry}.
 */
public class JmxReporterProvider implements Provider<JmxReporter> {
  private final MetricRegistry metricRegistry;
  private final CodaHaleMetricsModule.Config config;

  @Inject
  public JmxReporterProvider(CodaHaleMetricsModule.Config config, MetricRegistry metricRegistry) {
    this.config = config;
    this.metricRegistry = metricRegistry;
  }

  @Override
  public JmxReporter get() {
    JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
    if (config.getJmx().isEnabled()) {
      reporter.start();
    }
    return reporter;
  }
}

