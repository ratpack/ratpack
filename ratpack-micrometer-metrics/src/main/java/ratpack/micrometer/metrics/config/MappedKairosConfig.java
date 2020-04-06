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

import io.micrometer.kairos.KairosConfig;

import javax.annotation.Nonnull;

public class MappedKairosConfig extends MappedStepRegistryConfig<RatpackKairosConfig>
		implements KairosConfig {

	public MappedKairosConfig(RatpackKairosConfig config) {
		super(config);
	}

	@Nonnull
	@Override
	public String uri() {
		return get(RatpackKairosConfig::getUri, KairosConfig.super::uri);
	}

	@Override
	public String userName() {
		return get(RatpackKairosConfig::getUserName, KairosConfig.super::userName);
	}

	@Override
	public String password() {
		return get(RatpackKairosConfig::getPassword, KairosConfig.super::password);
	}
}
