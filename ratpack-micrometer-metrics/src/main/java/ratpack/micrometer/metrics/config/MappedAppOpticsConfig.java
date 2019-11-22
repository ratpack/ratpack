package ratpack.micrometer.metrics.config;

import io.micrometer.appoptics.AppOpticsConfig;

import javax.annotation.Nonnull;

public class MappedAppOpticsConfig extends MappedStepRegistryConfig<RatpackAppOpticsConfig>
		implements AppOpticsConfig {

	public MappedAppOpticsConfig(RatpackAppOpticsConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackAppOpticsConfig::getUri, AppOpticsConfig.super::uri);
	}

  @Nonnull
	@Override
	public String apiToken() {
		return get(RatpackAppOpticsConfig::getApiToken, AppOpticsConfig.super::apiToken);
	}

	@Override
	public String hostTag() {
		return get(RatpackAppOpticsConfig::getHostTag, AppOpticsConfig.super::hostTag);
	}
}
