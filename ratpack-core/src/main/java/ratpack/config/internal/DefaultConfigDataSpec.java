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
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk7.Jdk7Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import ratpack.config.ConfigData;
import ratpack.config.ConfigDataSpec;
import ratpack.config.ConfigSource;
import ratpack.config.EnvironmentParser;
import ratpack.config.internal.module.ConfigModule;
import ratpack.config.internal.source.*;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.server.internal.ServerEnvironment;
import ratpack.util.Exceptions;
import ratpack.util.internal.Paths2;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class DefaultConfigDataSpec implements ConfigDataSpec {
  private final ImmutableList.Builder<ConfigSource> sources = ImmutableList.builder();
  private final ServerEnvironment serverEnvironment;
  private final ObjectMapper objectMapper;
  private Action<? super Throwable> errorHandler = Action.throwException();

  public DefaultConfigDataSpec(ServerEnvironment serverEnvironment) {
    this(serverEnvironment, newDefaultObjectMapper(serverEnvironment));
  }

  public DefaultConfigDataSpec(ServerEnvironment serverEnvironment, Module... modules) {
    this(serverEnvironment, newDefaultObjectMapper(serverEnvironment).registerModules(modules));
  }

  public DefaultConfigDataSpec(ServerEnvironment serverEnvironment, Optional<ObjectMapper> objectMapper) {
    this.serverEnvironment = serverEnvironment;
    this.objectMapper = objectMapper.orElse(newDefaultObjectMapper(serverEnvironment));
  }

  public DefaultConfigDataSpec(ServerEnvironment serverEnvironment, ObjectMapper objectMapper) {
    this.serverEnvironment = serverEnvironment;
    this.objectMapper = objectMapper;
  }

  @Override
  public ConfigDataSpec add(ConfigSource configSource) {
    sources.add(new ErrorHandlingConfigSource(configSource, errorHandler));
    return this;
  }

  @Override
  public ConfigData build() {
    return new DefaultConfigData(this);
  }

  @Override
  public ConfigDataSpec configureObjectMapper(Action<ObjectMapper> action) {
    try {
      action.execute(objectMapper);
    } catch (Exception ex) {
      throw Exceptions.uncheck(ex);
    }
    return this;
  }

  @Override
  public ConfigDataSpec env() {
    return add(new EnvironmentConfigSource(serverEnvironment));
  }

  @Override
  public ConfigDataSpec env(String prefix) {
    return add(new EnvironmentConfigSource(serverEnvironment, prefix));
  }

  @Override
  public ConfigDataSpec env(String prefix, Function<String, String> mapFunc) {
    return add(new EnvironmentConfigSource(serverEnvironment, prefix, mapFunc));
  }

  @Override
  public ConfigDataSpec env(EnvironmentParser environmentParser) {
    return add(new EnvironmentConfigSource(serverEnvironment, environmentParser));
  }

  @Override
  public ConfigDataSpec json(ByteSource byteSource) {
    return add(new JsonConfigSource(byteSource));
  }

  @Override
  public ConfigDataSpec json(Path path) {
    return add(new JsonConfigSource(path));
  }

  @Override
  public ConfigDataSpec json(URL url) {
    return add(new JsonConfigSource(url));
  }

  @Override
  public ConfigDataSpec props(ByteSource byteSource) {
    return add(new ByteSourcePropertiesConfigSource(Optional.empty(), byteSource));
  }

  @Override
  public ConfigDataSpec props(Path path) {
    return add(new ByteSourcePropertiesConfigSource(Optional.empty(), Paths2.asByteSource(path)));
  }

  @Override
  public ConfigDataSpec props(Properties properties) {
    return add(new PropertiesConfigSource(Optional.empty(), properties));
  }

  @Override
  public ConfigDataSpec props(URL url) {
    return add(new ByteSourcePropertiesConfigSource(Optional.empty(), Resources.asByteSource(url)));
  }

  @Override
  public ConfigDataSpec props(Map<String, String> map) {
    return add(new MapConfigSource(Optional.empty(), map));
  }

  @Override
  public ConfigDataSpec sysProps() {
    return sysProps(DEFAULT_PROP_PREFIX);
  }

  @Override
  public ConfigDataSpec sysProps(String prefix) {
    return add(new PropertiesConfigSource(Optional.of(prefix), serverEnvironment.getProperties()));
  }

  @Override
  public ConfigDataSpec yaml(ByteSource byteSource) {
    return add(new YamlConfigSource(byteSource));
  }

  @Override
  public ConfigDataSpec yaml(Path path) {
    return add(new YamlConfigSource(path));
  }

  @Override
  public ConfigDataSpec yaml(URL url) {
    return add(new YamlConfigSource(url));
  }

  @Override
  public ConfigDataSpec onError(Action<? super Throwable> errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  public static ObjectMapper newDefaultObjectMapper(ServerEnvironment serverEnvironment) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.registerModule(new Jdk7Module());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JSR310Module());
    objectMapper.registerModule(new ConfigModule(serverEnvironment));
    JsonFactory factory = objectMapper.getFactory();
    factory.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
    factory.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
    return objectMapper;
  }

  @Override
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public ImmutableList<ConfigSource> getConfigSources() {
    return sources.build();
  }
}
