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

import io.micrometer.humio.HumioConfig;

import javax.annotation.Nonnull;
import java.util.Map;

public class MappedHumioConfig extends MappedStepRegistryConfig<RatpackHumioConfig> implements HumioConfig {

	public MappedHumioConfig(RatpackHumioConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackHumioConfig::getUri, HumioConfig.super::uri);
	}

	@Override
	public Map<String, String> tags() {
		return get(RatpackHumioConfig::getTags, HumioConfig.super::tags);
	}

	@Override
	public String apiToken() {
		return get(RatpackHumioConfig::getApiToken, HumioConfig.super::apiToken);
	}
}
