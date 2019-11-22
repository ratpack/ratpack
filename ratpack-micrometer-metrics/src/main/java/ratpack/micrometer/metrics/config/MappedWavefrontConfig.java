package ratpack.micrometer.metrics.config;

import io.micrometer.wavefront.WavefrontConfig;

import javax.annotation.Nonnull;

public class MappedWavefrontConfig extends MappedPushRegistryPropertiesConfig<RatpackWavefrontConfig>
		implements WavefrontConfig {

	public MappedWavefrontConfig(RatpackWavefrontConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackWavefrontConfig::getUri, WavefrontConfig.DEFAULT_DIRECT::uri);
	}

	@Nonnull
	@Override
	public String source() {
		return get(RatpackWavefrontConfig::getSource, WavefrontConfig.super::source);
	}

	@Override
	public String apiToken() {
		return get(RatpackWavefrontConfig::getApiToken, WavefrontConfig.super::apiToken);
	}

	@Override
	public String globalPrefix() {
		return get(RatpackWavefrontConfig::getGlobalPrefix, WavefrontConfig.super::globalPrefix);
	}
}

