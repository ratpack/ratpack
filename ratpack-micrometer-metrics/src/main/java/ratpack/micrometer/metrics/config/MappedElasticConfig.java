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

import io.micrometer.elastic.ElasticConfig;

class MappedElasticConfig extends MappedStepRegistryConfig<RatpackElasticConfig>
		implements ElasticConfig {

	public MappedElasticConfig(RatpackElasticConfig config) {
		super(config);
	}

	@Override
	public String host() {
		return get(RatpackElasticConfig::getHost, ElasticConfig.super::host);
	}

	@Override
	public String index() {
		return get(RatpackElasticConfig::getIndex, ElasticConfig.super::index);
	}

	@Override
	public String indexDateFormat() {
		return get(RatpackElasticConfig::getIndexDateFormat, ElasticConfig.super::indexDateFormat);
	}

	@Override
	public String timestampFieldName() {
		return get(RatpackElasticConfig::getTimestampFieldName, ElasticConfig.super::timestampFieldName);
	}

	@Override
	public boolean autoCreateIndex() {
		return get(RatpackElasticConfig::isAutoCreateIndex, ElasticConfig.super::autoCreateIndex);
	}

	@Override
	public String userName() {
		return get(RatpackElasticConfig::getUserName, ElasticConfig.super::userName);
	}

	@Override
	public String password() {
		return get(RatpackElasticConfig::getPassword, ElasticConfig.super::password);
	}
}
