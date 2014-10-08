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

package ratpack.configuration.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ratpack.configuration.Configuration;
import ratpack.configuration.ConfigurationException;
import ratpack.configuration.ConfigurationSource;
import ratpack.configuration.LaunchConfigFactory;
import ratpack.launch.*;
import ratpack.launch.internal.LaunchConfigsInternal;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.ssl.SSLContexts;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

/**
 * A default LaunchConfigFactory that delegates to {@link ratpack.launch.LaunchConfigs} after putting the configuration class in the default registry.
 */
@JsonTypeName("default")
public class DefaultLaunchConfigFactory implements LaunchConfigFactory {
  private Path baseDir;
  private int port = LaunchConfig.DEFAULT_PORT;
  private InetAddress address;
  private boolean development;
  @Min(1)
  private int threads = LaunchConfig.DEFAULT_THREADS;
  private URI publicAddress;
  private ImmutableList<String> indexFiles = ImmutableList.of();
  private ImmutableMap<String, String> other = ImmutableMap.of();
  @Min(1)
  private int maxContentLength = LaunchConfig.DEFAULT_MAX_CONTENT_LENGTH;
  private boolean timeResponses;
  private boolean compressResponses;
  @Min(0)
  private long compressionMinSize = LaunchConfig.DEFAULT_COMPRESSION_MIN_SIZE;
  private ImmutableList<String> compressionMimeTypeWhiteList = ImmutableList.of();
  private ImmutableList<String> compressionMimeTypeBlackList = ImmutableList.of();
  private Path sslKeystore;
  private String sslKeystorePassword;

  @NotNull
  private Class<? extends HandlerFactory> handlerFactoryClass;

  private static Path determineDefaultBaseDir(ConfigurationSource configurationSource) {
    String workingDir = StandardSystemProperty.USER_DIR.value();
    String configResourceValue = configurationSource.getOverrideProperties().getProperty(LaunchConfigs.CONFIG_RESOURCE_PROPERTY, LaunchConfigs.CONFIG_RESOURCE_DEFAULT);
    URL configResourceUrl = configurationSource.getClassLoader().getResource(configResourceValue);
    Path configPath = LaunchConfigsInternal.determineConfigPath(workingDir, configResourceValue, configResourceUrl);
    return LaunchConfigsInternal.determineBaseDir(configPath);
  }

  @Override
  public LaunchConfig build(ConfigurationSource configurationSource, Configuration configuration) {
    Registry defaultRegistry = Registries.registry().add(Configuration.class, configuration).add(configuration).build();
    HandlerFactory handlerFactory;
    Verify.verifyNotNull(handlerFactoryClass);
    try {
      handlerFactory = handlerFactoryClass.newInstance();
    } catch (Exception e) {
      throw new LaunchException("Could not instantiate handler factory: " + handlerFactoryClass.getName(), e);
    }
    if (baseDir == null) {
      baseDir = determineDefaultBaseDir(configurationSource);
    }
    LaunchConfigBuilder builder = LaunchConfigBuilder.baseDir(baseDir).port(port).address(address).development(development).threads(threads).publicAddress(publicAddress).indexFiles(indexFiles).other(other).maxContentLength(maxContentLength).timeResponses(timeResponses).compressResponses(compressResponses).compressionMinSize(compressionMinSize).compressionWhiteListMimeTypes(compressionMimeTypeWhiteList).compressionBlackListMimeTypes(compressionMimeTypeBlackList).defaultRegistry(defaultRegistry);
    if (sslKeystore != null) {
      try (InputStream stream = Files.newInputStream(sslKeystore)) {
        builder.ssl(SSLContexts.sslContext(stream, sslKeystorePassword));
      } catch (IOException|GeneralSecurityException ex) {
        throw new ConfigurationException("Could not configure SSL from keystore " + sslKeystore);
      }
    }
    return builder.build(handlerFactory);
  }

  public Path getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(Path baseDir) {
    this.baseDir = baseDir;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public InetAddress getAddress() {
    return address;
  }

  public void setAddress(InetAddress address) {
    this.address = address;
  }

  public boolean isDevelopment() {
    return development;
  }

  public void setDevelopment(boolean development) {
    this.development = development;
  }

  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  public URI getPublicAddress() {
    return publicAddress;
  }

  public void setPublicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
  }

  public ImmutableList<String> getIndexFiles() {
    return indexFiles;
  }

  public void setIndexFiles(List<String> indexFiles) {
    this.indexFiles = ImmutableList.copyOf(indexFiles);
  }

  public ImmutableMap<String, String> getOther() {
    return other;
  }

  public void setOther(Map<String, String> other) {
    this.other = ImmutableMap.copyOf(other);
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }

  public void setMaxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
  }

  public boolean isTimeResponses() {
    return timeResponses;
  }

  public void setTimeResponses(boolean timeResponses) {
    this.timeResponses = timeResponses;
  }

  public boolean isCompressResponses() {
    return compressResponses;
  }

  public void setCompressResponses(boolean compressResponses) {
    this.compressResponses = compressResponses;
  }

  public long getCompressionMinSize() {
    return compressionMinSize;
  }

  public void setCompressionMinSize(long compressionMinSize) {
    this.compressionMinSize = compressionMinSize;
  }

  public ImmutableList<String> getCompressionMimeTypeWhiteList() {
    return compressionMimeTypeWhiteList;
  }

  public void setCompressionMimeTypeWhiteList(List<String> compressionMimeTypeWhiteList) {
    this.compressionMimeTypeWhiteList = ImmutableList.copyOf(compressionMimeTypeWhiteList);
  }

  public ImmutableList<String> getCompressionMimeTypeBlackList() {
    return compressionMimeTypeBlackList;
  }

  public void setCompressionMimeTypeBlackList(List<String> compressionMimeTypeBlackList) {
    this.compressionMimeTypeBlackList = ImmutableList.copyOf(compressionMimeTypeBlackList);
  }

  @JsonProperty("handlerFactory")
  public Class<? extends HandlerFactory> getHandlerFactoryClass() {
    return handlerFactoryClass;
  }

  @JsonProperty("handlerFactory")
  public void setHandlerFactoryClass(Class<? extends HandlerFactory> handlerFactoryClass) {
    this.handlerFactoryClass = handlerFactoryClass;
  }

  public Path getSslKeystore() {
    return sslKeystore;
  }

  public void setSslKeystore(Path sslKeystore) {
    this.sslKeystore = sslKeystore;
  }

  public String getSslKeystorePassword() {
    return sslKeystorePassword;
  }

  public void setSslKeystorePassword(String sslKeystorePassword) {
    this.sslKeystorePassword = sslKeystorePassword;
  }
}
