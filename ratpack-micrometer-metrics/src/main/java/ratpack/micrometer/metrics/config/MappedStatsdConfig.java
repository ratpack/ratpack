package ratpack.micrometer.metrics.config;

import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;

import javax.annotation.Nonnull;
import java.time.Duration;

public class MappedStatsdConfig extends MappedMeterRegistryConfig<RatpackStatsdConfig> implements StatsdConfig {

	public MappedStatsdConfig(RatpackStatsdConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public StatsdFlavor flavor() {
		return get(this::statsdFlavorFromString, StatsdConfig.super::flavor);
	}

	@Override
	public boolean enabled() {
		return get(RatpackStatsdConfig::isEnabled, StatsdConfig.super::enabled);
	}

  @Nonnull
	@Override
	public String host() {
		return get(RatpackStatsdConfig::getHost, StatsdConfig.super::host);
	}

	@Override
	public int port() {
		return get(RatpackStatsdConfig::getPort, StatsdConfig.super::port);
	}

	@Override
	public int maxPacketLength() {
		return get(RatpackStatsdConfig::getMaxPacketLength, StatsdConfig.super::maxPacketLength);
	}

	@Nonnull
	@Override
	public Duration pollingFrequency() {
		return get(RatpackStatsdConfig::getPollingFrequency, StatsdConfig.super::pollingFrequency);
	}

	@Override
	public boolean publishUnchangedMeters() {
		return get(RatpackStatsdConfig::isPublishUnchangedMeters, StatsdConfig.super::publishUnchangedMeters);
	}

  private StatsdFlavor statsdFlavorFromString(RatpackStatsdConfig config) {
    for (StatsdFlavor value : StatsdFlavor.values()) {
      if(value.name().equalsIgnoreCase(config.getFlavor())) {
        return value;
      }
    }
    return null;
  }
}

