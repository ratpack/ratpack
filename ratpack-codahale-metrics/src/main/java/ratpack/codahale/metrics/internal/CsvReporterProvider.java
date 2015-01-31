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

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Provider;
import ratpack.codahale.metrics.CodaHaleMetricsModule;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * A Provider implementation that sets up a {@link CsvReporter} for a {@link MetricRegistry}.
 */
public class CsvReporterProvider implements Provider<CsvReporter> {

  private final MetricRegistry metricRegistry;
  private final CodaHaleMetricsModule.Config config;

  @Inject
  public CsvReporterProvider(MetricRegistry metricRegistry, CodaHaleMetricsModule.Config config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public CsvReporter get() {
    CsvReporter reporter = CsvReporter.forRegistry(metricRegistry).build(config.getCsv().getReportDirectory());
    if (config.getCsv().isEnabled()) {
      reporter.start(config.getCsv().getReporterInterval(), TimeUnit.SECONDS);
    }
    return reporter;
  }
}

