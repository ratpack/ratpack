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

package ratpack.launch.internal;

import com.google.common.collect.Iterables;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import ratpack.api.Nullable;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.launch.LaunchConfigs.*;
import ratpack.launch.LaunchException;
import ratpack.ssl.SSLContexts;
import ratpack.util.internal.PropertiesUtil;
import ratpack.util.internal.TypeCoercingProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

import static ratpack.launch.LaunchConfig.*;
import static ratpack.launch.LaunchConfigs.*;
import static ratpack.launch.LaunchConfigs.Property.*;
import static ratpack.util.ExceptionUtils.uncheck;
import static ratpack.util.internal.PropertiesUtil.extractProperties;

public class LaunchConfigsInternal {
  private LaunchConfigsInternal() {}

  public static LaunchConfigData createFromGlobalProperties(ClassLoader classLoader, Properties globalProperties, Properties defaultProperties) {
    String propertyPrefix = globalProperties.getProperty(SYSPROP_PREFIX_PROPERTY, SYSPROP_PREFIX_DEFAULT);
    return createFromGlobalProperties(classLoader, propertyPrefix, globalProperties, defaultProperties);
  }

  public static LaunchConfigData createFromGlobalProperties(ClassLoader classLoader, String propertyPrefix, Properties globalProperties, Properties defaultProperties) {
    Properties deprefixed = new Properties();
    extractProperties(propertyPrefix, globalProperties, deprefixed);
    return createFromProperties(classLoader, deprefixed, defaultProperties);
  }

  public static LaunchConfigData createFromProperties(ClassLoader classLoader, Properties overrideProperties, Properties defaultProperties) {
    String configResourceValue = overrideProperties.getProperty(CONFIG_RESOURCE_PROPERTY, CONFIG_RESOURCE_DEFAULT);
    URL configResourceUrl = classLoader.getResource(configResourceValue);

    Path configPath;
    Path baseDir;

    if (configResourceUrl == null) {
      configPath = Paths.get(configResourceValue);
      if (!configPath.isAbsolute()) {
        configPath = Paths.get(System.getProperty("user.dir"), configResourceValue);
      }

      baseDir = configPath.getParent();
    } else {
      configPath = resourceToPath(configResourceUrl);
      baseDir = configPath.getParent();
      if (baseDir == null && configPath.getFileSystem().provider() instanceof ZipFileSystemProvider) {
        baseDir = Iterables.getFirst(configPath.getFileSystem().getRootDirectories(), null);
      }

      if (baseDir == null) {
        throw new LaunchException("Cannot determine base dir given config resource: " + configPath);
      }
    }

    return createFromFile(classLoader, baseDir, configPath, overrideProperties, defaultProperties);
  }

  public static LaunchConfigData createFromFile(ClassLoader classLoader, Path baseDir, @Nullable Path configFile, Properties overrideProperties, Properties defaultProperties) {
    Properties fileProperties = new Properties(defaultProperties);
    if (configFile != null && Files.exists(configFile)) {
      try (InputStream inputStream = Files.newInputStream(configFile)) {
        fileProperties.load(inputStream);
      } catch (Exception e) {
        throw new LaunchException("Could not read config file '" + configFile + "'", e);
      }
    }

    fileProperties.putAll(overrideProperties);

    return createWithBaseDir(classLoader, baseDir, fileProperties);
  }

  public static LaunchConfigData createWithBaseDir(ClassLoader classLoader, Path baseDir, Properties properties) {
    return createWithBaseDir(classLoader, baseDir, properties, System.getenv());
  }

  public static LaunchConfigData createWithBaseDir(ClassLoader classLoader, Path baseDir, Properties properties, Map<String, String> envVars) {
    return new LaunchConfigData(classLoader, baseDir, properties, envVars);
  }

  public static LaunchConfig createLaunchConfig(LaunchConfigData data) {
    Properties properties = data.getProperties();
    Map<String, String> envVars = data.getEnvVars();
    TypeCoercingProperties props = new TypeCoercingProperties(properties, data.getClassLoader());
    try {
      Class<HandlerFactory> handlerFactoryClass;
      handlerFactoryClass = props.asClass(HANDLER_FACTORY, HandlerFactory.class);
      if (handlerFactoryClass == null) {
        throw new LaunchException("No handler factory class specified (config property: " + HANDLER_FACTORY + ")");
      }

      int defaultPort = DEFAULT_PORT;
      if (envVars.containsKey(Environment.PORT)) {
        try {
          String stringValue = envVars.get(Environment.PORT);
          defaultPort = Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
          throw new LaunchException("Environment var '" + Environment.PORT + "' is not an integer", e);
        }
      }

      int port = props.asInt(PORT, defaultPort);
      InetAddress address = props.asInetAddress(ADDRESS);
      URI publicAddress = props.asURI(PUBLIC_ADDRESS);
      boolean reloadable = props.asBoolean(RELOADABLE, false);
      int threads = props.asInt(THREADS, DEFAULT_THREADS);
      List<String> indexFiles = props.asList(INDEX_FILES);
      InputStream sslKeystore = props.asStream(SSL_KEYSTORE_FILE);
      String sslKeystorePassword = props.asString(SSL_KEYSTORE_PASSWORD, "");
      int maxContentLength = props.asInt(MAX_CONTENT_LENGTH, DEFAULT_MAX_CONTENT_LENGTH);
      boolean timeResponses = props.asBoolean(TIME_RESPONSES, false);
      boolean compressResponses = props.asBoolean(COMPRESS_RESPONSES, false);
      long compressionMinSize = props.asLong(COMPRESSION_MIN_SIZE, DEFAULT_COMPRESSION_MIN_SIZE);
      List<String> compressionMimeTypeWhiteList = props.asList(COMPRESSION_MIME_TYPE_WHITE_LIST);
      List<String> compressionMimeTypeBlackList = props.asList(COMPRESSION_MIME_TYPE_BLACK_LIST);

      Map<String, String> otherProperties = new HashMap<>();
      PropertiesUtil.extractProperties("other.", properties, otherProperties);

      HandlerFactory handlerFactory;
      try {
        handlerFactory = handlerFactoryClass.newInstance();
      } catch (Exception e) {
        throw new LaunchException("Could not instantiate handler factory: " + handlerFactoryClass.getName(), e);
      }

      LaunchConfigBuilder launchConfigBuilder = LaunchConfigBuilder.baseDir(data.getBaseDir())
        .port(port)
        .address(address)
        .publicAddress(publicAddress)
        .reloadable(reloadable)
        .threads(threads)
        .maxContentLength(maxContentLength)
        .timeResponses(timeResponses)
        .compressResponses(compressResponses)
        .compressionMinSize(compressionMinSize)
        .compressionWhiteListMimeTypes(compressionMimeTypeWhiteList)
        .compressionBlackListMimeTypes(compressionMimeTypeBlackList)
        .indexFiles(indexFiles);

      if (sslKeystore != null) {
        try (InputStream stream = sslKeystore) {
          launchConfigBuilder.ssl(SSLContexts.sslContext(stream, sslKeystorePassword));
        }
      }

      return launchConfigBuilder
        .other(otherProperties)
        .build(handlerFactory);

    } catch (Exception e) {
      if (e instanceof LaunchException) {
        throw (LaunchException) e;
      } else {
        throw new LaunchException("Failed to create launch config with properties: " + properties.toString(), e);
      }
    }
  }

  private static Path resourceToPath(URL resource) {
    URI uri = toUri(resource);

    String scheme = uri.getScheme();
    if (scheme.equals("file")) {
      return Paths.get(uri);
    }

    if (!scheme.equals("jar")) {
      throw new LaunchException("Cannot deal with class path resource url: " + uri);
    }

    String s = uri.toString();
    int separator = s.indexOf("!/");
    String entryName = s.substring(separator + 2);
    URI fileURI = URI.create(s.substring(0, separator));
    FileSystem fs;
    try {
      fs = FileSystems.newFileSystem(fileURI, Collections.<String, Object>emptyMap());
    } catch (IOException e) {
      throw uncheck(e);
    }
    return fs.getPath(entryName);
  }

  private static URI toUri(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new LaunchException("Could not convert URL '" + url + "' to URI", e);
    }
  }
}
