package ratpack.micrometer.metrics.config;

import io.micrometer.signalfx.SignalFxConfig;

import javax.annotation.Nonnull;

public class MappedSignalFxConfig extends MappedStepRegistryConfig<RatpackSignalFxConfig>
		implements SignalFxConfig {

	public MappedSignalFxConfig(RatpackSignalFxConfig config) {
		super(config);
		accessToken(); // validate that an access token is set
	}

	@Nonnull
	@Override
	public String accessToken() {
		return get(RatpackSignalFxConfig::getAccessToken, SignalFxConfig.super::accessToken);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackSignalFxConfig::getUri, SignalFxConfig.super::uri);
	}

	@Nonnull
	@Override
	public String source() {
		return get(RatpackSignalFxConfig::getSource, SignalFxConfig.super::source);
	}
}

