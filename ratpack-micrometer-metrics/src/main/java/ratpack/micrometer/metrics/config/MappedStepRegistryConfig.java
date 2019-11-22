package ratpack.micrometer.metrics.config;

abstract class MappedStepRegistryConfig<T extends RatpackStepRegistryConfig>
		extends MappedPushRegistryPropertiesConfig<T> {

	MappedStepRegistryConfig(T config) {
		super(config);
	}
}
