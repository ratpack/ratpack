package ratpack.micrometer.metrics.config;

import io.micrometer.kairos.KairosConfig;

import javax.annotation.Nonnull;

public class MappedKairosConfig extends MappedStepRegistryConfig<RatpackKairosConfig>
		implements KairosConfig {

	public MappedKairosConfig(RatpackKairosConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackKairosConfig::getUri, KairosConfig.super::uri);
	}

	@Override
	public String userName() {
		return get(RatpackKairosConfig::getUserName, KairosConfig.super::userName);
	}

	@Override
	public String password() {
		return get(RatpackKairosConfig::getPassword, KairosConfig.super::password);
	}
}
