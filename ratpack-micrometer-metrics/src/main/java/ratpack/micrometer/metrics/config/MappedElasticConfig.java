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
