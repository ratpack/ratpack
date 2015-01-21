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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import ratpack.config.ConfigurationData;
import ratpack.config.ConfigurationDataSpec;
import ratpack.config.ConfigurationSource;
import ratpack.config.EnvironmentParser;
import ratpack.config.internal.module.ConfigurationModule;
import ratpack.config.internal.source.*;
import ratpack.config.internal.source.env.Environment;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.util.ExceptionUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class DefaultConfigurationDataSpec implements ConfigurationDataSpec {
  private final ImmutableList.Builder<ConfigurationSource> sources = ImmutableList.builder();
  private final Environment environment;
  private final ObjectMapper objectMapper;

  public DefaultConfigurationDataSpec(Environment environment) {
    this(environment, newDefaultObjectMapper(environment));
  }

  public DefaultConfigurationDataSpec(Environment environment, ObjectMapper objectMapper) {
    this.environment = environment;
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
    add(new EnvironmentConfigurationSource(environment));
    return this;
  }

  @Override
  public ConfigurationDataSpec env(String prefix) {
    add(new EnvironmentConfigurationSource(environment, prefix));
    return this;
  }

  @Override
  public ConfigurationDataSpec env(String prefix, Function<String, String> mapFunc) {
    add(new EnvironmentConfigurationSource(environment, prefix, mapFunc));
    return this;
  }

  @Override
  public ConfigurationDataSpec env(EnvironmentParser environmentParser) {
    add(new EnvironmentConfigurationSource(environment, environmentParser));
    return this;
  }

  @Override
  public ConfigurationDataSpec json(ByteSource byteSource) {
    add(new JsonConfigurationSource(byteSource));
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

  @Override
  public ConfigurationDataSpec props(ByteSource byteSource) {
    add(new ByteSourcePropertiesConfigurationSource(Optional.empty(), byteSource));
    return this;
  }

  @Override
  public ConfigurationDataSpec props(Path path) {
    add(new ByteSourcePropertiesConfigurationSource(Optional.empty(), Files.asByteSource(path.toFile())));
    return this;
  }

  @Override
  public ConfigurationDataSpec props(Properties properties) {
    add(new PropertiesConfigurationSource(Optional.empty(), properties));
    return this;
  }

  @Override
  public ConfigurationDataSpec props(URL url) {
    add(new ByteSourcePropertiesConfigurationSource(Optional.empty(), Resources.asByteSource(url)));
    return this;
  }

  @Override
  public ConfigurationDataSpec props(Map<String, String> map) {
    add(new MapConfigurationSource(Optional.empty(), map));
    return this;
  }

  @Override
  public ConfigurationDataSpec sysProps() {
    add(new SystemPropertiesConfigurationSource());
    return this;
  }

  @Override
  public ConfigurationDataSpec sysProps(String prefix) {
    add(new SystemPropertiesConfigurationSource(Optional.of(prefix)));
    return this;
  }

  @Override
  public ConfigurationDataSpec yaml(ByteSource byteSource) {
    add(new YamlConfigurationSource(byteSource));
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

  public static ObjectMapper newDefaultObjectMapper(Environment environment) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new ConfigurationModule(environment));
    JsonFactory factory = objectMapper.getFactory();
    factory.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
    return objectMapper;
  }
}
