/*
 * Copyright 2014 the original author or authors.
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

package ratpack.server.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import ratpack.config.ConfigData;
import ratpack.config.ConfigObject;
import ratpack.config.ConfigSource;
import ratpack.config.EnvironmentParser;
import ratpack.config.internal.DefaultConfigDataSpec;
import ratpack.func.Action;
import ratpack.server.ServerConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class DefaultServerConfigBuilder extends DefaultConfigDataSpec implements ServerConfig.Builder {

  private final ObjectNode serverConfigData;
  private final Map<String, Class<?>> required = Maps.newHashMap();

  public DefaultServerConfigBuilder(ServerEnvironment serverEnvironment, Optional<Path> baseDir, Optional<ObjectMapper> objectMapper) {
    super(serverEnvironment, objectMapper);
    this.serverConfigData = getObjectMapper().createObjectNode();
    if (baseDir.isPresent()) {
      serverConfigData.putPOJO("baseDir", baseDir.get());
    }
  }

  @Override
  public ServerConfig.Builder port(int port) {
    serverConfigData.put("port", port);
    return this;
  }

  @Override
  public ServerConfig.Builder address(InetAddress address) {
    serverConfigData.putPOJO("address", address);
    return this;
  }

  @Override
  public ServerConfig.Builder development(boolean development) {
    serverConfigData.put("development", development);
    return this;
  }

  @Override
  public ServerConfig.Builder threads(int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("'threads' must be > 0");
    }
    serverConfigData.put("threads", threads);
    return this;
  }

  @Override
  public ServerConfig.Builder publicAddress(URI publicAddress) {
    serverConfigData.putPOJO("publicAddress", publicAddress);
    return this;
  }

  @Override
  public ServerConfig.Builder maxContentLength(int maxContentLength) {
    serverConfigData.put("maxContentLength", maxContentLength);
    return this;
  }

  @Override
  public ServerConfig.Builder ssl(SSLContext sslContext) {
    serverConfigData.putPOJO("ssl", sslContext);
    return this;
  }

  @Override
  public ServerConfig.Builder requireClientSslAuth(boolean requireClientSslAuth) {
    serverConfigData.put("requireClientSslAuth", requireClientSslAuth);
    return this;
  }

  @Override
  public ServerConfig.Builder configureObjectMapper(Action<ObjectMapper> action) {
    super.configureObjectMapper(action);
    return this;
  }

  @Override
  public ServerConfig.Builder add(ConfigSource configSource) {
    super.add(configSource);
    return this;
  }

  @Override
  public ServerConfig.Builder env(String prefix, ratpack.func.Function<String, String> mapFunc) {
    super.env(prefix, mapFunc);
    return this;
  }

  @Override
  public ServerConfig.Builder env(EnvironmentParser environmentParser) {
    super.env(environmentParser);
    return this;
  }

  @Override
  public ServerConfig.Builder env() {
    super.env();
    return this;
  }

  @Override
  public ServerConfig.Builder env(String prefix) {
    super.env(prefix);
    return this;
  }

  @Override
  public ServerConfig.Builder json(ByteSource byteSource) {
    super.json(byteSource);
    return this;
  }

  @Override
  public ServerConfig.Builder json(Path path) {
    super.json(path);
    return this;
  }

  @Override
  public ServerConfig.Builder json(URL url) {
    super.json(url);
    return this;
  }

  @Override
  public ServerConfig.Builder props(ByteSource byteSource) {
    super.props(byteSource);
    return this;
  }

  @Override
  public ServerConfig.Builder props(Path path) {
    super.props(path);
    return this;
  }

  @Override
  public ServerConfig.Builder props(Properties properties) {
    super.props(properties);
    return this;
  }

  @Override
  public ServerConfig.Builder props(URL url) {
    super.props(url);
    return this;
  }

  @Override
  public ServerConfig.Builder props(Map<String, String> map) {
    super.props(map);
    return this;
  }

  @Override
  public ServerConfig.Builder sysProps() {
    super.sysProps();
    return this;
  }

  @Override
  public ServerConfig.Builder sysProps(String prefix) {
    super.sysProps(prefix);
    return this;
  }

  @Override
  public ServerConfig.Builder yaml(ByteSource byteSource) {
    super.yaml(byteSource);
    return this;
  }

  @Override
  public ServerConfig.Builder yaml(Path path) {
    super.yaml(path);
    return this;
  }

  @Override
  public ServerConfig.Builder yaml(URL url) {
    super.yaml(url);
    return this;
  }

  @Override
  public ServerConfig.Builder require(String pointer, Class<?> type) {
    Class<?> previous = required.put(
      Objects.requireNonNull(pointer, "pointer cannot be null"),
      Objects.requireNonNull(type, "type cannot be null")
    );
    if (previous != null) {
      throw new IllegalArgumentException("Cannot require config of type '" + type + "' at '" + pointer + "' as '" + previous + " has already been registered for this path");
    }
    return this;
  }

  @Override
  public ServerConfig.Builder onError(Action<? super Throwable> errorHandler) {
    super.onError(errorHandler);
    return this;
  }

  @Override
  public ServerConfig.Builder json(String path) {
    super.json(path);
    return this;
  }

  @Override
  public ServerConfig.Builder props(String path) {
    super.props(path);
    return this;
  }

  @Override
  public ServerConfig.Builder yaml(String path) {
    super.yaml(path);
    return this;
  }

  @Override
  public ImmutableList<ConfigSource> getConfigSources() {
    ConfigSource internalConfigSource = objectMapper -> {
      ObjectNode node = objectMapper.createObjectNode();
      node.putObject("server").setAll(serverConfigData);
      return node;
    };
    return ImmutableList.<ConfigSource>builder().addAll(super.getConfigSources()).add(internalConfigSource).build();
  }

  @Override
  public ServerConfig build() {
    ConfigData configData = super.build();
    ImmutableSet<ConfigObject<?>> requiredConfig = extractRequiredConfig(configData, required);
    return new DefaultServerConfig(configData, requiredConfig);
  }

  private static ImmutableSet<ConfigObject<?>> extractRequiredConfig(ConfigData configData, Map<String, Class<?>> required) {
    RuntimeException badConfig = new IllegalStateException("Failed to build required config items");
    ImmutableSet.Builder<ConfigObject<?>> config = ImmutableSet.builder();
    for (Map.Entry<String, Class<?>> requiredConfig : required.entrySet()) {
      String path = requiredConfig.getKey();
      Class<?> type = requiredConfig.getValue();
      try {
        config.add(configData.getAsConfigObject(path, type));
      } catch (Exception e) {
        badConfig.addSuppressed(new IllegalStateException("Could not bind config at '" + path + "' to '" + type + "'", e));
      }
    }

    if (badConfig.getSuppressed().length > 0) {
      throw badConfig;
    } else {
      return config.build();
    }
  }

  public static ServerConfig.Builder noBaseDir(ServerEnvironment serverEnvironment) {
    return new DefaultServerConfigBuilder(serverEnvironment, Optional.empty(), Optional.empty());
  }

  public static ServerConfig.Builder baseDir(ServerEnvironment serverEnvironment, Path baseDir) {
    return new DefaultServerConfigBuilder(serverEnvironment, Optional.of(baseDir.toAbsolutePath().normalize()), Optional.empty());
  }

  public static ServerConfig.Builder findBaseDir(ServerEnvironment serverEnvironment, String markerFilePath) {
    return BaseDirFinder.find(Thread.currentThread().getContextClassLoader(), markerFilePath)
      .map(b -> baseDir(serverEnvironment, b.getBaseDir()))
      .orElseThrow(() -> new IllegalStateException("Could not find marker file '" + markerFilePath + "' via context class loader"));
  }

}
