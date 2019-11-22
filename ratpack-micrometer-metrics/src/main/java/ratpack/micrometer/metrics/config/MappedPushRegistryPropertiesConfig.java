package ratpack.micrometer.metrics.config;

import io.micrometer.core.instrument.push.PushRegistryConfig;

import javax.annotation.Nonnull;
import java.time.Duration;

public abstract class MappedPushRegistryPropertiesConfig<T extends RatpackPushRegistryConfig>
		extends MappedMeterRegistryConfig<T> implements PushRegistryConfig {

	MappedPushRegistryPropertiesConfig(T config) {
		super(config);
	}

	@Nonnull
	@Override
	public String prefix() {
		return "doesntmatter";
	}

	@Nonnull
	@Override
	public Duration step() {
		return get(T::getStep, PushRegistryConfig.super::step);
	}

	@Override
	public boolean enabled() {
		return get(T::isEnabled, PushRegistryConfig.super::enabled);
	}

	@Override
	public int numThreads() {
		return get(T::getNumThreads, PushRegistryConfig.super::numThreads);
	}

	@Override
	public int batchSize() {
		return get(T::getBatchSize, PushRegistryConfig.super::batchSize);
	}

}
