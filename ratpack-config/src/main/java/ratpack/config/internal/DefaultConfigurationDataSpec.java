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
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.server.ServerConfig;
import ratpack.server.ServerEnvironment;
import ratpack.util.ExceptionUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class DefaultConfigurationDataSpec implements ConfigurationDataSpec {
  private final ImmutableList.Builder<ConfigurationSource> sources = ImmutableList.builder();
  private final ServerEnvironment serverEnvironment;
  private final ObjectMapper objectMapper;

  public DefaultConfigurationDataSpec(ServerEnvironment serverEnvironment) {
    this(serverEnvironment, newDefaultObjectMapper(serverEnvironment));
  }

  public DefaultConfigurationDataSpec(ServerEnvironment serverEnvironment, ObjectMapper objectMapper) {
    this.serverEnvironment = serverEnvironment;
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
    return add(new EnvironmentConfigurationSource(serverEnvironment));
  }

  @Override
  public ConfigurationDataSpec env(String prefix) {
    return add(new EnvironmentConfigurationSource(serverEnvironment, prefix));
  }

  @Override
  public ConfigurationDataSpec env(String prefix, Function<String, String> mapFunc) {
    return add(new EnvironmentConfigurationSource(serverEnvironment, prefix, mapFunc));
  }

  @Override
  public ConfigurationDataSpec env(EnvironmentParser environmentParser) {
    return add(new EnvironmentConfigurationSource(serverEnvironment, environmentParser));
  }

  @Override
  public ConfigurationDataSpec json(ByteSource byteSource) {
    return add(new JsonConfigurationSource(byteSource));
  }

  @Override
  public ConfigurationDataSpec json(Path path) {
    return add(new JsonConfigurationSource(path));
  }

  @Override
  public ConfigurationDataSpec json(URL url) {
    return add(new JsonConfigurationSource(url));
  }

  @Override
  public ConfigurationDataSpec props(ByteSource byteSource) {
    return add(new ByteSourcePropertiesConfigurationSource(Optional.empty(), byteSource));
  }

  @Override
  public ConfigurationDataSpec props(Path path) {
    return add(new ByteSourcePropertiesConfigurationSource(Optional.empty(), Files.asByteSource(path.toFile())));
  }

  @Override
  public ConfigurationDataSpec props(Properties properties) {
    return add(new PropertiesConfigurationSource(Optional.empty(), properties));
  }

  @Override
  public ConfigurationDataSpec props(URL url) {
    return add(new ByteSourcePropertiesConfigurationSource(Optional.empty(), Resources.asByteSource(url)));
  }

  @Override
  public ConfigurationDataSpec props(Map<String, String> map) {
    return add(new MapConfigurationSource(Optional.empty(), map));
  }

  @Override
  public ConfigurationDataSpec sysProps() {
    return sysProps(ServerConfig.Builder.DEFAULT_PROP_PREFIX);
  }

  @Override
  public ConfigurationDataSpec sysProps(String prefix) {
    return add(new PropertiesConfigurationSource(Optional.of(prefix), serverEnvironment.getProperties()));
  }

  @Override
  public ConfigurationDataSpec yaml(ByteSource byteSource) {
    return add(new YamlConfigurationSource(byteSource));
  }

  @Override
  public ConfigurationDataSpec yaml(Path path) {
    return add(new YamlConfigurationSource(path));
  }

  @Override
  public ConfigurationDataSpec yaml(URL url) {
    return add(new YamlConfigurationSource(url));
  }

  public static ObjectMapper newDefaultObjectMapper(ServerEnvironment serverEnvironment) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new ConfigurationModule(serverEnvironment));
    JsonFactory factory = objectMapper.getFactory();
    factory.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
    return objectMapper;
  }
}
