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

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import ratpack.configuration.*;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigs;
import ratpack.launch.LaunchException;
import ratpack.launch.internal.LaunchConfigData;
import ratpack.launch.internal.LaunchConfigsInternal;
import ratpack.registry.Registries;
import ratpack.util.internal.PropertiesUtil;
import ratpack.util.internal.TypeCoercingProperties;

import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static ratpack.launch.LaunchConfig.*;
import static ratpack.launch.LaunchConfigs.Property.*;

/**
 * A simple configuration factory that just instantiates the desired configuration class and populates the default launch config factory based on the configuration properties.
 */
public class DefaultConfigurationFactory implements ConfigurationFactory {
  @Override
  public <T extends Configuration> T build(Class<T> configurationClass, ConfigurationSource configurationSource) throws ConfigurationException {
    T configuration;
    try {
      configuration = configurationClass.newInstance();
    } catch (ReflectiveOperationException ex) {
      throw new ConfigurationException("Could not instantiate configuration class " + configurationClass.getName(), ex);
    }
    LaunchConfigData launchConfigData = LaunchConfigsInternal.createFromGlobalProperties(
      StandardSystemProperty.USER_DIR.value(), configurationSource.getClassLoader(), configurationSource.getOverrideProperties(), configurationSource.getDefaultProperties(), Registries.empty()
    );
    LaunchConfigFactory launchConfigFactory = configuration.getLaunchConfigFactory();
    if (launchConfigFactory instanceof DefaultLaunchConfigFactory) {
      populateLaunchConfigFactory(launchConfigData, (DefaultLaunchConfigFactory) launchConfigFactory);
    }
    return configuration;
  }

  private void populateLaunchConfigFactory(LaunchConfigData data, DefaultLaunchConfigFactory launchConfigFactory) {
    Properties properties = data.getProperties();
    Map<String, String> envVars = data.getEnvVars();
    TypeCoercingProperties props = data.getTypeCoercingProperties();
    Class<HandlerFactory> handlerFactoryClass;
    try {
      handlerFactoryClass = props.asClass(HANDLER_FACTORY, HandlerFactory.class);
      if (handlerFactoryClass == null) {
        throw new LaunchException("No handler factory class specified (config property: " + HANDLER_FACTORY + ")");
      }

      int defaultPort = LaunchConfig.DEFAULT_PORT;
      if (envVars.containsKey(LaunchConfigs.Environment.PORT)) {
        try {
          String stringValue = envVars.get(LaunchConfigs.Environment.PORT);
          defaultPort = Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
          throw new LaunchException("Environment var '" + LaunchConfigs.Environment.PORT + "' is not an integer", e);
        }
      }

      int port = props.asInt(PORT, defaultPort);
      InetAddress address = props.asInetAddress(ADDRESS);
      URI publicAddress = props.asURI(PUBLIC_ADDRESS);
      boolean development = props.asBoolean(DEVELOPMENT, false);
      int threads = props.asInt(THREADS, DEFAULT_THREADS);
      List<String> indexFiles = props.asList(INDEX_FILES);
      String sslKeystore = props.asString(SSL_KEYSTORE_FILE, null);
      String sslKeystorePassword = props.asString(SSL_KEYSTORE_PASSWORD, "");
      int maxContentLength = props.asInt(MAX_CONTENT_LENGTH, DEFAULT_MAX_CONTENT_LENGTH);
      boolean timeResponses = props.asBoolean(TIME_RESPONSES, false);
      boolean compressResponses = props.asBoolean(COMPRESS_RESPONSES, false);
      long compressionMinSize = props.asLong(COMPRESSION_MIN_SIZE, DEFAULT_COMPRESSION_MIN_SIZE);
      List<String> compressionMimeTypeWhiteList = props.asList(COMPRESSION_MIME_TYPE_WHITE_LIST);
      List<String> compressionMimeTypeBlackList = props.asList(COMPRESSION_MIME_TYPE_BLACK_LIST);

      Map<String, String> otherProperties = Maps.newHashMap();
      PropertiesUtil.extractProperties("other.", properties, otherProperties);

      launchConfigFactory.setBaseDir(data.getBaseDir());
      launchConfigFactory.setPort(port);
      launchConfigFactory.setAddress(address);
      launchConfigFactory.setDevelopment(development);
      launchConfigFactory.setThreads(threads);
      launchConfigFactory.setPublicAddress(publicAddress);
      launchConfigFactory.setIndexFiles(indexFiles);
      launchConfigFactory.setOther(otherProperties);
      if (!Strings.isNullOrEmpty(sslKeystore)) {
        launchConfigFactory.setSslKeystore(Paths.get(sslKeystore));
      }
      launchConfigFactory.setSslKeystorePassword(sslKeystorePassword);
      launchConfigFactory.setMaxContentLength(maxContentLength);
      launchConfigFactory.setTimeResponses(timeResponses);
      launchConfigFactory.setCompressResponses(compressResponses);
      launchConfigFactory.setCompressionMinSize(compressionMinSize);
      launchConfigFactory.setCompressionMimeTypeWhiteList(compressionMimeTypeWhiteList);
      launchConfigFactory.setCompressionMimeTypeBlackList(compressionMimeTypeBlackList);
      launchConfigFactory.setHandlerFactoryClass(handlerFactoryClass);
    } catch (Exception e) {
      if (e instanceof LaunchException) {
        throw (LaunchException) e;
      } else {
        throw new LaunchException("Failed to create launch config with properties: " + properties.toString(), e);
      }
    }
  }
}
