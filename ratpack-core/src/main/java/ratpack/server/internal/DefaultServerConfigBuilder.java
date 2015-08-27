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
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import ratpack.config.*;
import ratpack.config.internal.DefaultConfigData;
import ratpack.config.internal.DefaultConfigDataBuilder;
import ratpack.config.internal.module.SSLContextDeserializer;
import ratpack.config.internal.module.ServerConfigDataDeserializer;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public class DefaultServerConfigBuilder implements ServerConfigBuilder {

  private final ConfigDataBuilder configDataBuilder;
  private final ObjectNode serverConfigData;
  private final Map<String, Class<?>> required = Maps.newHashMap();
  private final BaseDirSupplier baseDirSupplier = new BaseDirSupplier();

  public DefaultServerConfigBuilder(ServerEnvironment serverEnvironment) {
    configDataBuilder = new DefaultConfigDataBuilder(serverEnvironment);
    this.configDataBuilder.jacksonModules(new ConfigModule(serverEnvironment, baseDirSupplier));
    this.serverConfigData = getObjectMapper().createObjectNode();
  }

  @Override
  public ObjectMapper getObjectMapper() {
    return configDataBuilder.getObjectMapper();
  }

  @Override
  public ServerConfigBuilder baseDir(Path baseDir) {
    this.baseDirSupplier.baseDir = FileSystemBinding.of(baseDir);
    return this;
  }

  @Override
  public ServerConfigBuilder port(int port) {
    serverConfigData.put("port", port);
    return this;
  }

  @Override
  public ServerConfigBuilder address(InetAddress address) {
    serverConfigData.putPOJO("address", address);
    return this;
  }

  @Override
  public ServerConfigBuilder development(boolean development) {
    serverConfigData.put("development", development);
    return this;
  }

  @Override
  public ServerConfigBuilder threads(int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("'threads' must be > 0");
    }
    serverConfigData.put("threads", threads);
    return this;
  }

  @Override
  public ServerConfigBuilder publicAddress(URI publicAddress) {
    serverConfigData.putPOJO("publicAddress", publicAddress);
    return this;
  }

  @Override
  public ServerConfigBuilder maxContentLength(int maxContentLength) {
    serverConfigData.put("maxContentLength", maxContentLength);
    return this;
  }

  @Override
  public ServerConfigBuilder connectTimeoutMillis(int connectTimeoutMillis) {
    serverConfigData.put("connectTimeoutMillis", connectTimeoutMillis);
    return this;
  }

  @Override
  public ServerConfigBuilder maxMessagesPerRead(int maxMessagesPerRead) {
    serverConfigData.put("maxMessagesPerRead", maxMessagesPerRead);
    return this;
  }

  @Override
  public ServerConfigBuilder receiveBufferSize(int receiveBufferSize) {
    serverConfigData.put("receiveBufferSize", receiveBufferSize);
    return this;
  }

  @Override
  public ServerConfigBuilder writeSpinCount(int writeSpinCount) {
    serverConfigData.put("writeSpinCount", writeSpinCount);
    return this;
  }

  @Override
  public ServerConfigBuilder ssl(SSLContext sslContext) {
    serverConfigData.putPOJO("ssl", sslContext);
    return this;
  }

  @Override
  public ServerConfigBuilder requireClientSslAuth(boolean requireClientSslAuth) {
    serverConfigData.put("requireClientSslAuth", requireClientSslAuth);
    return this;
  }

  @Override
  public ServerConfigBuilder configureObjectMapper(Action<ObjectMapper> action) {
    configDataBuilder.configureObjectMapper(action);
    return this;
  }

  @Override
  public ServerConfigBuilder add(ConfigSource configSource) {
    configDataBuilder.add(configSource);
    return this;
  }

  @Override
  public ServerConfigBuilder env(String prefix, ratpack.func.Function<String, String> mapFunc) {
    configDataBuilder.env(prefix, mapFunc);
    return this;
  }

  @Override
  public ServerConfigBuilder env(EnvironmentParser environmentParser) {
    configDataBuilder.env(environmentParser);
    return this;
  }

  @Override
  public ServerConfigBuilder env() {
    configDataBuilder.env();
    return this;
  }

  @Override
  public ServerConfigBuilder env(String prefix) {
    configDataBuilder.env(prefix);
    return this;
  }

  @Override
  public ServerConfigBuilder json(ByteSource byteSource) {
    configDataBuilder.json(byteSource);
    return this;
  }

  @Override
  public ServerConfigBuilder json(Path path) {
    configDataBuilder.json(path);
    return this;
  }

  @Override
  public ServerConfigBuilder json(URL url) {
    configDataBuilder.json(url);
    return this;
  }

  @Override
  public ServerConfigBuilder props(ByteSource byteSource) {
    configDataBuilder.props(byteSource);
    return this;
  }

  @Override
  public ServerConfigBuilder props(Path path) {
    configDataBuilder.props(path);
    return this;
  }

  @Override
  public ServerConfigBuilder props(Properties properties) {
    configDataBuilder.props(properties);
    return this;
  }

  @Override
  public ServerConfigBuilder props(URL url) {
    configDataBuilder.props(url);
    return this;
  }

  @Override
  public ServerConfigBuilder props(Map<String, String> map) {
    configDataBuilder.props(map);
    return this;
  }

  @Override
  public ServerConfigBuilder sysProps() {
    configDataBuilder.sysProps();
    return this;
  }

  @Override
  public ServerConfigBuilder sysProps(String prefix) {
    configDataBuilder.sysProps(prefix);
    return this;
  }

  @Override
  public ServerConfigBuilder yaml(ByteSource byteSource) {
    configDataBuilder.yaml(byteSource);
    return this;
  }

  @Override
  public ServerConfigBuilder yaml(Path path) {
    configDataBuilder.yaml(path);
    return this;
  }

  @Override
  public ServerConfigBuilder yaml(URL url) {
    configDataBuilder.yaml(url);
    return this;
  }

  @Override
  public ServerConfigBuilder require(String pointer, Class<?> type) {
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
  public ServerConfigBuilder onError(Action<? super Throwable> errorHandler) {
    configDataBuilder.onError(errorHandler);
    return this;
  }

  @Override
  public ServerConfigBuilder json(String path) {
    configDataBuilder.json(path);
    return this;
  }

  @Override
  public ServerConfigBuilder props(String path) {
    configDataBuilder.props(path);
    return this;
  }

  @Override
  public ServerConfigBuilder yaml(String path) {
    configDataBuilder.yaml(path);
    return this;
  }

  @Override
  public ImmutableList<ConfigSource> getConfigSources() {
    return configDataBuilder.getConfigSources();
  }

  @Override
  public ServerConfig build() {
    Iterable<ConfigSource> configSources = Iterables.concat(getConfigSources(), Collections.<ConfigSource>singleton((mapper, pathResolver) -> {
      ObjectNode node = mapper.createObjectNode();
      node.putObject("server").setAll(serverConfigData);
      return node;
    }));

    ConfigData configData = new DefaultConfigData(configDataBuilder.getObjectMapper(), configSources, MoreObjects.firstNonNull(baseDirSupplier.baseDir, FileSystemBinding.root()));
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

  public static class ConfigModule extends SimpleModule {
    public ConfigModule(ServerEnvironment serverEnvironment, Supplier<FileSystemBinding> baseDirSupplier) {
      super("ratpack");
      addDeserializer(ServerConfigData.class, new ServerConfigDataDeserializer(
        serverEnvironment.getPort(),
        serverEnvironment.isDevelopment(),
        serverEnvironment.getPublicAddress(),
        baseDirSupplier
      ));
      addDeserializer(SSLContext.class, new SSLContextDeserializer());
    }
  }

  private static class BaseDirSupplier implements Supplier<FileSystemBinding> {
    private FileSystemBinding baseDir;

    @Override
    public FileSystemBinding get() {
      return baseDir;
    }
  }
}
