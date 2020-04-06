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

import io.micrometer.wavefront.WavefrontConfig;

import javax.annotation.Nonnull;

public class MappedWavefrontConfig extends MappedPushRegistryPropertiesConfig<RatpackWavefrontConfig>
		implements WavefrontConfig {

	public MappedWavefrontConfig(RatpackWavefrontConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackWavefrontConfig::getUri, WavefrontConfig.DEFAULT_DIRECT::uri);
	}

	@Nonnull
	@Override
	public String source() {
		return get(RatpackWavefrontConfig::getSource, WavefrontConfig.super::source);
	}

	@Override
	public String apiToken() {
		return get(RatpackWavefrontConfig::getApiToken, WavefrontConfig.super::apiToken);
	}

	@Override
	public String globalPrefix() {
		return get(RatpackWavefrontConfig::getGlobalPrefix, WavefrontConfig.super::globalPrefix);
	}
}

