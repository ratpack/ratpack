/*
 * Copyright 2015 the original author or authors.
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

package ratpack.config.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import ratpack.config.ConfigurationData;
import ratpack.config.ConfigurationDataSpec;
import ratpack.config.ConfigurationSource;
import ratpack.config.internal.module.ConfigurationModule;
import ratpack.config.internal.source.*;
import ratpack.func.Action;
import ratpack.util.ExceptionUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;

public class DefaultConfigurationDataSpec implements ConfigurationDataSpec {
  private final ObjectMapper objectMapper;
  private final ImmutableList.Builder<ConfigurationSource> sources = ImmutableList.builder();

  public DefaultConfigurationDataSpec() {
    this(newDefaultObjectMapper());
  }

  public DefaultConfigurationDataSpec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public ConfigurationDataSpec add(ConfigurationSource configurationSource) {
    sources.add(configurationSource);
    return this;
  }

  @Override
  public ConfigurationData build() {
    return new DefaultConfigurationData(objectMapper, sources.build());
  }

  @Override
  public ConfigurationDataSpec configureObjectMapper(Action<ObjectMapper> action) {
    try {
      action.execute(objectMapper);
    } catch (Exception ex) {
      throw ExceptionUtils.uncheck(ex);
    }
    return this;
  }

  @Override
  public ConfigurationDataSpec env() {
    add(new EnvironmentVariablesConfigurationSource());
    return this;
  }

  @Override
  public ConfigurationDataSpec json(Path path) {
    add(new JsonConfigurationSource(path));
    return this;
  }

  @Override
  public ConfigurationDataSpec json(URL url) {
    add(new JsonConfigurationSource(url));
    return this;
  }

  // TODO: support specifying a prefix?
  @Override
  public ConfigurationDataSpec props(Path path) {
    add(new PropertiesConfigurationSource(path));
    return this;
  }

  @Override
  public ConfigurationDataSpec props(Properties properties) {
    add(new PropertiesConfigurationSource(null, properties));
    return this;
  }

  @Override
  public ConfigurationDataSpec props(URL url) {
    add(new PropertiesConfigurationSource(url));
    return this;
  }

  // TODO: support specifying a prefix?
  @Override
  public ConfigurationDataSpec sysProps() {
    add(new SystemPropertiesConfigurationSource());
    return this;
  }

  @Override
  public ConfigurationDataSpec yaml(Path path) {
    add(new YamlConfigurationSource(path));
    return this;
  }

  @Override
  public ConfigurationDataSpec yaml(URL url) {
    add(new YamlConfigurationSource(url));
    return this;
  }

  private static ObjectMapper newDefaultObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new ConfigurationModule());
    JsonFactory factory = objectMapper.getFactory();
    factory.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
    return objectMapper;
  }
}
