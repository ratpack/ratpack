/*
 * Copyright 2018 the original author or authors.
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
package ratpack.micrometer;

import com.google.inject.Provides;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import ratpack.guice.ConfigurableModule;
import ratpack.micrometer.internal.DefaultRequestTimingHandler;

import javax.inject.Singleton;
import java.time.Duration;

public class PrometheusMicrometerModule extends ConfigurableModule<MicrometerConfig> {

  @Override
  public void configure() {
    bind(RequestTimingHandler.class).to(DefaultRequestTimingHandler.class);
  }

  @Provides
  @Singleton
  PrometheusMeterRegistry providesPrometheus() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  @Provides
  @Singleton
  CompositeMeterRegistry providesCompositeMeterRegistry() {
    return new CompositeMeterRegistry();
  }

  @Provides
  @Singleton
  MeterRegistry providesMeterRegistry(PrometheusMeterRegistry prometheus,
                                      CompositeMeterRegistry composite,
                                      MicrometerConfig config) {
    prometheus.config().meterFilter(new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        return DistributionStatisticConfig.builder()
          .percentilesHistogram(true)
          .minimumExpectedValue(Duration.ofMillis(10).toNanos())
          .maximumExpectedValue(Duration.ofSeconds(5).toNanos())
          .build()
          .merge(config);
      }
    });

    prometheus.config().commonTags("application", config.application);

    composite.add(prometheus);

    new ClassLoaderMetrics().bindTo(composite);
    new JvmMemoryMetrics().bindTo(composite);
    new JvmGcMetrics().bindTo(composite);
    new ProcessorMetrics().bindTo(composite);
    new JvmThreadMetrics().bindTo(composite);
    new LogbackMetrics().bindTo(composite);
    new UptimeMetrics().bindTo(composite);

    return composite;
  }
}



