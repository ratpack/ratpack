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

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import ratpack.dropwizard.metrics.CsvConfig;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A Provider implementation that sets up a {@link CsvReporter} for a {@link MetricRegistry}.
 */
public class CsvReporterProvider implements Provider<CsvReporter> {
  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;

  @Inject
  public CsvReporterProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public CsvReporter get() {
    if (config.getCsv().isPresent()) {
      CsvConfig csv = config.getCsv().get();
      CsvReporter.Builder builder = CsvReporter.forRegistry(metricRegistry);
      if (csv.getIncludeFilter() != null || csv.getExcludeFilter() != null) {
        builder.filter(new RegexMetricFilter(csv.getIncludeFilter(), csv.getExcludeFilter()));
      }
      return builder.build(csv.getReportDirectory());
    } else {
      return null;
    }
  }

}
