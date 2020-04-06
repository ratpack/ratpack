/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

