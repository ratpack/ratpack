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
import com.google.inject.Provider;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class MetricsModule extends AbstractModule {

  static MetricRegistry registry = new MetricRegistry();
  private boolean reportToJmx;
  private boolean reportToCsv;
  private File csvReportDirectory;

  @Override
  protected void configure() {
    bind(MetricRegistry.class).toProvider(new Provider<MetricRegistry>() {
      @Override
      public MetricRegistry get() {
        return registry;
      }
    }).asEagerSingleton();

    if (reportToJmx) {
      bind(JmxReporter.class).toProvider(new Provider<JmxReporter>() {
        @Override
        public JmxReporter get() {
          JmxReporter reporter = JmxReporter.forRegistry(registry).build();
          reporter.start();
          return reporter;
        }
      }).asEagerSingleton();
    }

    if (reportToCsv) {
      bind(CsvReporter.class).toProvider(new Provider<CsvReporter>() {
        @Override
        public CsvReporter get() {
          CsvReporter reporter = CsvReporter.
            forRegistry(registry).
            build(csvReportDirectory);
          reporter.start(1, TimeUnit.SECONDS);
          return reporter;
        }
      }).asEagerSingleton();
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

}
