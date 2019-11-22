package ratpack.micrometer.metrics.config;

import io.micrometer.humio.HumioConfig;

import javax.annotation.Nonnull;
import java.util.Map;

public class MappedHumioConfig extends MappedStepRegistryConfig<RatpackHumioConfig> implements HumioConfig {

	public MappedHumioConfig(RatpackHumioConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackHumioConfig::getUri, HumioConfig.super::uri);
	}

	@Override
	public Map<String, String> tags() {
		return get(RatpackHumioConfig::getTags, HumioConfig.super::tags);
	}

	@Override
	public String apiToken() {
		return get(RatpackHumioConfig::getApiToken, HumioConfig.super::apiToken);
	}
}
