package ratpack.micrometer.metrics.config;

import io.micrometer.core.lang.NonNull;
import io.micrometer.datadog.DatadogConfig;

import javax.annotation.Nonnull;

public class MappedDatadogConfig extends MappedStepRegistryConfig<RatpackDatadogConfig>
		implements DatadogConfig {

	MappedDatadogConfig(RatpackDatadogConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String apiKey() {
		return get(RatpackDatadogConfig::getApiKey, DatadogConfig.super::apiKey);
	}

	@Override
	public String applicationKey() {
		return get(RatpackDatadogConfig::getApplicationKey, DatadogConfig.super::applicationKey);
	}

	@Override
	public String hostTag() {
		return get(RatpackDatadogConfig::getHostTag, DatadogConfig.super::hostTag);
	}

	@NonNull
	@Override
	public String uri() {
		return get(RatpackDatadogConfig::getUri, DatadogConfig.super::uri);
	}

	@Override
	public boolean descriptions() {
		return get(RatpackDatadogConfig::isDescriptions, DatadogConfig.super::descriptions);
	}
}
