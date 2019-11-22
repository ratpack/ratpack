package ratpack.micrometer.metrics.config;

import io.micrometer.dynatrace.DynatraceConfig;

import javax.annotation.Nonnull;

public class MappedDynatraceConfig extends MappedStepRegistryConfig<RatpackDynatraceConfig>
		implements DynatraceConfig {

  public MappedDynatraceConfig(RatpackDynatraceConfig config) {
		super(config);
	}

  @Nonnull
	@Override
	public String apiToken() {
		return get(RatpackDynatraceConfig::getApiToken, DynatraceConfig.super::apiToken);
	}

  @Nonnull
	@Override
	public String deviceId() {
		return get(RatpackDynatraceConfig::getDeviceId, DynatraceConfig.super::deviceId);
	}

	@Nonnull
	@Override
	public String technologyType() {
		return get(RatpackDynatraceConfig::getTechnologyType, DynatraceConfig.super::technologyType);
	}

  @Nonnull
	@Override
	public String uri() {
		return get(RatpackDynatraceConfig::getUri, DynatraceConfig.super::uri);
	}
}
