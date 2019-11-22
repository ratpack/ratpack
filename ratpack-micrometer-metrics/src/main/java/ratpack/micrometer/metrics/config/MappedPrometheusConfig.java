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
