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
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import ratpack.codahale.internal.CsvReporterProvider;
import ratpack.codahale.internal.JmxReporterProvider;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;

import java.io.File;

public class MetricsModule extends AbstractModule implements HandlerDecoratingModule {

  private boolean reportToJmx;
  private boolean reportToCsv;
  private File csvReportDirectory;

  @Override
  protected void configure() {
    bind(MetricRegistry.class).in(Singleton.class);

    if (reportToJmx) {
      bind(JmxReporter.class).toProvider(JmxReporterProvider.class).asEagerSingleton();
    }

    if (reportToCsv) {
      bind(File.class).annotatedWith(Names.named(CsvReporterProvider.CSV_REPORT_DIRECTORY)).toInstance(csvReportDirectory);
      bind(CsvReporter.class).toProvider(CsvReporterProvider.class).asEagerSingleton();
    }
  }

  public MetricsModule reportToJmx() {
    this.reportToJmx = true;
    return this;
  }

  public MetricsModule reportToCsv(File reportDirectory) {
    if (reportDirectory == null) {
      throw new IllegalArgumentException("reportDirectory cannot be null");
    }

    this.reportToCsv = true;
    csvReportDirectory = reportDirectory;
    return this;
  }

  @Override
  public Handler decorate(Injector injector, Handler handler) {
    return new MetricHandler(handler);
  }
}
