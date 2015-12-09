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
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import ratpack.config.*;
import ratpack.config.internal.DefaultConfigData;
import ratpack.config.internal.DefaultConfigDataBuilder;
import ratpack.config.internal.module.SSLContextDeserializer;
import ratpack.config.internal.module.SslContextDeserializer;
import ratpack.config.internal.module.ServerConfigDataDeserializer;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

import io.netty.handler.ssl.SslContext;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DefaultServerConfigBuilder implements ServerConfigBuilder {

  private final ConfigDataBuilder configDataBuilder;
  private final Map<String, Class<?>> required = Maps.newHashMap();
  private final BaseDirSupplier baseDirSupplier = new BaseDirSupplier();

  public DefaultServerConfigBuilder(ServerEnvironment serverEnvironment) {
    configDataBuilder = new DefaultConfigDataBuilder(serverEnvironment);
    this.configDataBuilder.jacksonModules(new ConfigModule(serverEnvironment, baseDirSupplier));
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

  private ServerConfigBuilder addToServer(Consumer<? super ObjectNode> action) {
    add((m, f) -> {
      ObjectNode rootNode = new ObjectNode(getObjectMapper().getNodeFactory());
      ObjectNode server = rootNode.putObject("server");
      action.accept(server);
      return rootNode;
    });
    return this;
  }

  @Override
  public ServerConfigBuilder port(int port) {
    return addToServer(n -> n.put("port", port));
  }

  @Override
  public ServerConfigBuilder address(InetAddress address) {
    return addToServer(n -> n.putPOJO("address", address));
  }

  @Override
  public ServerConfigBuilder development(boolean development) {
    return addToServer(n -> n.put("development", development));
  }

  @Override
  public ServerConfigBuilder threads(int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("'threads' must be > 0");
    }
    return addToServer(n -> n.put("threads", threads));
  }

  @Override
  public ServerConfigBuilder publicAddress(URI publicAddress) {
    return addToServer(n -> n.putPOJO("publicAddress", publicAddress));
  }

  @Override
  public ServerConfigBuilder maxContentLength(int maxContentLength) {
    return addToServer(n -> n.put("maxContentLength", maxContentLength));
  }

  @Override
  public ServerConfigBuilder connectTimeoutMillis(int connectTimeoutMillis) {
    return addToServer(n -> n.put("connectTimeoutMillis", connectTimeoutMillis));
  }

  @Override
  public ServerConfigBuilder maxMessagesPerRead(int maxMessagesPerRead) {
    return addToServer(n -> n.put("maxMessagesPerRead", maxMessagesPerRead));
  }

  @Override
  public ServerConfigBuilder receiveBufferSize(int receiveBufferSize) {
    return addToServer(n -> n.put("receiveBufferSize", receiveBufferSize));
  }

  @Override
  public ServerConfigBuilder writeSpinCount(int writeSpinCount) {
    return addToServer(n -> n.put("writeSpinCount", writeSpinCount));
  }

  @Override
  public ServerConfigBuilder ssl(SslContext sslContext) {
    return addToServer(n -> n.putPOJO("ssl", sslContext));
  }

  @Override
  public ServerConfigBuilder ssl(SSLContext sslContext) {
    return addToServer(n -> n.putPOJO("ssl", sslContext));
  }

  @Override
  public ServerConfigBuilder requireClientSslAuth(boolean requireClientSslAuth) {
    return addToServer(n -> n.put("requireClientSslAuth", requireClientSslAuth));
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
  public ServerConfigBuilder args(String[] args) {
    configDataBuilder.args(args);
    return this;

  }

  @Override
  public ServerConfigBuilder args(String separator, String[] args) {
    configDataBuilder.args(separator, args);
    return this;
  }

  @Override
  public ServerConfigBuilder args(String prefix, String separator, String[] args) {
    configDataBuilder.args(prefix, separator, args);
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
    ConfigData configData = new DefaultConfigData(configDataBuilder.getObjectMapper(), getConfigSources(), MoreObjects.firstNonNull(baseDirSupplier.baseDir, FileSystemBinding.root()));
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
      addDeserializer(SslContext.class, new SslContextDeserializer());
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
