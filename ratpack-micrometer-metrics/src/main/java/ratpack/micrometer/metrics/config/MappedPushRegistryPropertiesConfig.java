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

import io.micrometer.core.instrument.push.PushRegistryConfig;

import javax.annotation.Nonnull;
import java.time.Duration;

public abstract class MappedPushRegistryPropertiesConfig<T extends RatpackPushRegistryConfig>
		extends MappedMeterRegistryConfig<T> implements PushRegistryConfig {

	MappedPushRegistryPropertiesConfig(T config) {
		super(config);
	}

	@Nonnull
	@Override
	public String prefix() {
		return "doesntmatter";
	}

	@Nonnull
	@Override
	public Duration step() {
		return get(T::getStep, PushRegistryConfig.super::step);
	}

	@Override
	public boolean enabled() {
		return get(T::isEnabled, PushRegistryConfig.super::enabled);
	}

	@Override
	public int numThreads() {
		return get(T::getNumThreads, PushRegistryConfig.super::numThreads);
	}

	@Override
	public int batchSize() {
		return get(T::getBatchSize, PushRegistryConfig.super::batchSize);
	}

}
