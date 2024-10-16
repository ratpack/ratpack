/*
 * Copyright 2020 the original author or authors.
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
import com.codahale.metrics.Reporter;
import jakarta.inject.Provider;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.dropwizard.metrics.ReporterConfigSupport;

import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractReporterProvider<T extends Reporter, C extends ReporterConfigSupport<C>> implements Provider<T> {

  protected final MetricRegistry metricRegistry;
  protected final DropwizardMetricsConfig config;
  protected final Function<DropwizardMetricsConfig, Optional<C>> configFactory;

  public AbstractReporterProvider(MetricRegistry metricRegistry, DropwizardMetricsConfig config, Function<DropwizardMetricsConfig, Optional<C>> configFactory) {
    this.metricRegistry = metricRegistry;
    this.config = config;
    this.configFactory = configFactory;
  }

  protected abstract T build(C config);

  @Override
  public T get() {
    return configFactory.apply(config).map(c -> {
     if (c.isEnabled()) {
      return build(c);
     } else {
       return null;
     }
    }).orElse(null);
  }
}
