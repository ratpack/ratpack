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

import com.netflix.spectator.atlas.AtlasConfig;

import java.time.Duration;

public class MappedAtlasConfig extends MappedStepRegistryConfig<RatpackAtlasConfig> implements AtlasConfig {

	public MappedAtlasConfig(RatpackAtlasConfig config) {
		super(config);
	}

	@SuppressWarnings("deprecation")
  @Override
	public Duration connectTimeout() {
		return get(RatpackAtlasConfig::getConnectTimeout, AtlasConfig.super::connectTimeout);
	}

	@SuppressWarnings("deprecation")
  @Override
	public Duration readTimeout() {
		return get(RatpackAtlasConfig::getReadTimeout, AtlasConfig.super::readTimeout);
	}

	@Override
	public int numThreads() {
		return get(RatpackAtlasConfig::getNumThreads, AtlasConfig.super::numThreads);
	}

	@Override
	public int batchSize() {
		return get(RatpackAtlasConfig::getBatchSize, AtlasConfig.super::batchSize);
	}

	@Override
	public String uri() {
		return get(RatpackAtlasConfig::getUri, AtlasConfig.super::uri);
	}

	@Override
	public Duration meterTTL() {
		return get(RatpackAtlasConfig::getMeterTimeToLive, AtlasConfig.super::meterTTL);
	}

	@Override
	public boolean lwcEnabled() {
		return get(RatpackAtlasConfig::isLwcEnabled, AtlasConfig.super::lwcEnabled);
	}

	@Override
	public Duration configRefreshFrequency() {
		return get(RatpackAtlasConfig::getConfigRefreshFrequency, AtlasConfig.super::configRefreshFrequency);
	}

	@Override
	public Duration configTTL() {
		return get(RatpackAtlasConfig::getConfigTimeToLive, AtlasConfig.super::configTTL);
	}

	@Override
	public String configUri() {
		return get(RatpackAtlasConfig::getConfigUri, AtlasConfig.super::configUri);
	}

	@Override
	public String evalUri() {
		return get(RatpackAtlasConfig::getEvalUri, AtlasConfig.super::evalUri);
	}
}
