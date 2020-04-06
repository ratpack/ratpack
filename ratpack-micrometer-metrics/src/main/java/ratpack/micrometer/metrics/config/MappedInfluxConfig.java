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

import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxConsistency;

import javax.annotation.Nonnull;

public class MappedInfluxConfig extends MappedStepRegistryConfig<RatpackInfluxConfig>
		implements InfluxConfig {

	public MappedInfluxConfig(RatpackInfluxConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String db() {
		return get(RatpackInfluxConfig::getDb, InfluxConfig.super::db);
	}

	@Nonnull
	@Override
	public InfluxConsistency consistency() {
		return get(this::influxConsistencyFromString, InfluxConfig.super::consistency);
	}

	@Override
	public String userName() {
		return get(RatpackInfluxConfig::getUserName, InfluxConfig.super::userName);
	}

	@Override
	public String password() {
		return get(RatpackInfluxConfig::getPassword, InfluxConfig.super::password);
	}

	@Override
	public String retentionPolicy() {
		return get(RatpackInfluxConfig::getRetentionPolicy, InfluxConfig.super::retentionPolicy);
	}

	@Override
	public Integer retentionReplicationFactor() {
		return get(RatpackInfluxConfig::getRetentionReplicationFactor, InfluxConfig.super::retentionReplicationFactor);
	}

	@Override
	public String retentionDuration() {
		return get(RatpackInfluxConfig::getRetentionDuration, InfluxConfig.super::retentionDuration);
	}

	@Override
	public String retentionShardDuration() {
		return get(RatpackInfluxConfig::getRetentionShardDuration, InfluxConfig.super::retentionShardDuration);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackInfluxConfig::getUri, InfluxConfig.super::uri);
	}

	@Override
	public boolean compressed() {
		return get(RatpackInfluxConfig::isCompressed, InfluxConfig.super::compressed);
	}

	@Override
	public boolean autoCreateDb() {
		return get(RatpackInfluxConfig::isAutoCreateDb, InfluxConfig.super::autoCreateDb);
	}

  private InfluxConsistency influxConsistencyFromString(RatpackInfluxConfig config) {
    for (InfluxConsistency value : InfluxConsistency.values()) {
      if(value.name().equalsIgnoreCase(config.getConsistency())) {
        return value;
      }
    }
    return null;
  }
}
