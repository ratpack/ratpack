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

package ratpack.codahale;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import ratpack.codahale.internal.CsvReporterProvider;
import ratpack.codahale.internal.JmxReporterProvider;
import ratpack.codahale.internal.RequestTimingHandler;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.guice.internal.GuiceUtil;
import ratpack.handling.Handler;
import ratpack.util.Action;

import java.io.File;

public class CodaHaleModule extends AbstractModule implements HandlerDecoratingModule {

  private boolean metricsEnabled;
  private boolean reportMetricsToJmx;
  private boolean reportMetricsToCsv;
  private File csvReportDirectory;
  private boolean healthChecksEnabled;

  @Override
  protected void configure() {
    if (metricsEnabled) {
      bind(MetricRegistry.class).in(Singleton.class);

      if (reportMetricsToJmx) {
        bind(JmxReporter.class).toProvider(JmxReporterProvider.class).asEagerSingleton();
      }

      if (reportMetricsToCsv) {
        bind(File.class).annotatedWith(Names.named(CsvReporterProvider.CSV_REPORT_DIRECTORY)).toInstance(csvReportDirectory);
        bind(CsvReporter.class).toProvider(CsvReporterProvider.class).asEagerSingleton();
      }
    }

    if (healthChecksEnabled) {
      bind(HealthCheckRegistry.class).in(Singleton.class);
    }
  }

  public CodaHaleModule enableMetrics() {
    this.metricsEnabled = true;
    return this;
  }

  public CodaHaleModule enableHealthChecks() {
    this.healthChecksEnabled = true;
    return this;
  }

  public CodaHaleModule reportMetricsToJmx() {
    this.reportMetricsToJmx = true;
    return this;
  }

  public CodaHaleModule reportMetricsToCsv(File reportDirectory) {
    if (reportDirectory == null) {
      throw new IllegalArgumentException("reportDirectory cannot be null");
    }

    this.reportMetricsToCsv = true;
    csvReportDirectory = reportDirectory;
    return this;
  }

  @Override
  public Handler decorate(Injector injector, Handler handler) {
    if (healthChecksEnabled) {
      final HealthCheckRegistry registry = injector.getInstance(HealthCheckRegistry.class);
      GuiceUtil.eachOfType(injector, TypeLiteral.get(NamedHealthCheck.class), new Action<NamedHealthCheck>() {
        public void execute(NamedHealthCheck healthCheck) throws Exception {
          registry.register(healthCheck.getName(), healthCheck);
        }
      });
    }

    if (metricsEnabled) {
      return new RequestTimingHandler(handler);
    } else {
      return handler;
    }
  }
}
