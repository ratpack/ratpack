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

package ratpack.micrometer.metrics.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.lang.NonNull;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

import java.time.Duration;

public class MappedPrometheusConfig extends MappedMeterRegistryConfig<RatpackPrometheusConfig>
		implements PrometheusConfig {

	public MappedPrometheusConfig(RatpackPrometheusConfig config) {
		super(config);
	}

	@Override
	public boolean descriptions() {
		return get(RatpackPrometheusConfig::isDescriptions, PrometheusConfig.super::descriptions);
	}

	@NonNull
	@Override
	public Duration step() {
		return get(RatpackPrometheusConfig::getStep, PrometheusConfig.super::step);
	}

	@Override
	public PrometheusMeterRegistry buildRegistry(Clock clock) {
	  return new PrometheusMeterRegistry(this, new CollectorRegistry(), clock);
  }
}
