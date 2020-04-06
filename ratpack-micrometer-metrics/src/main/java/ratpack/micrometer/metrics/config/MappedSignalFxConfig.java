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

