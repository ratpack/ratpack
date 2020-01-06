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
