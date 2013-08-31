/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.launch;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class LaunchConfigFactory {

  public static final String SYSPROP_PREFIX_PROPERTY = "ratpack.syspropPrefix";
  public static final String SYSPROP_PREFIX_DEFAULT = "ratpack.";

  public static final String CONFIG_RESOURCE_PROPERTY = "configResource";
  public static final String CONFIG_RESOURCE_DEFAULT = "ratpack.properties";

  private LaunchConfigFactory() {
  }

  public static LaunchConfig createFromGlobalProperties(ClassLoader classLoader, Properties globalProperties, Properties defaultProperties) {
    String propertyPrefix = globalProperties.getProperty(SYSPROP_PREFIX_PROPERTY, SYSPROP_PREFIX_DEFAULT);
    return createFromGlobalProperties(classLoader, propertyPrefix, globalProperties, defaultProperties);
  }

  public static LaunchConfig createFromGlobalProperties(ClassLoader classLoader, String propertyPrefix, Properties globalProperties, Properties defaultProperties) {
    Properties deprefixed = new Properties();
    extractProperties(propertyPrefix, globalProperties, deprefixed);
    return createFromProperties(classLoader, deprefixed, defaultProperties);
  }

  public static Properties getDefaultPrefixedProperties() {
    Properties deprefixed = new Properties();
    extractProperties(SYSPROP_PREFIX_DEFAULT, System.getProperties(), deprefixed);
    return deprefixed;
  }

  private static void extractProperties(String propertyPrefix, Properties properties, Map<? super String, ? super String> destination) {
    for (String propertyName : properties.stringPropertyNames()) {
      if (propertyName.startsWith(propertyPrefix)) {
        destination.put(propertyName.substring(propertyPrefix.length()), properties.getProperty(propertyName));
      }
    }
  }

  public static LaunchConfig createFromProperties(ClassLoader classLoader, Properties overrideProperties, Properties defaultProperties) {
    String configResourceValue = overrideProperties.getProperty(CONFIG_RESOURCE_PROPERTY, CONFIG_RESOURCE_DEFAULT);
    File configFile = new File(CONFIG_RESOURCE_DEFAULT);
    if (!configFile.exists()) {
      URL configResourceUrl = classLoader.getResource(configResourceValue);
      if (configResourceUrl == null) {
        return createFromFile(classLoader, new File(System.getProperty("user.dir")), null, overrideProperties, defaultProperties);
      }

      try {
        URI uri = configResourceUrl.toURI();
        configFile = new File(uri);
      } catch (Exception e) {
        throw new LaunchException("Could not get file for config file resource '" + configResourceUrl + "'", e);
      }
    }

    return createFromFile(classLoader, configFile.getParentFile(), configFile, overrideProperties, defaultProperties);
  }

  public static LaunchConfig createFromFile(ClassLoader classLoader, File baseDir, File configFile, Properties overrideProperties, Properties defaultProperties) {
    Properties fileProperties = new Properties(defaultProperties);
    if (configFile != null && configFile.exists()) {
      try (InputStream inputStream = new FileInputStream(configFile)) {
        fileProperties.load(inputStream);
      } catch (Exception e) {
        throw new LaunchException("Could not read config file '" + configFile + "'", e);
      }
    }

    fileProperties.putAll(overrideProperties);

    return createWithBaseDir(classLoader, baseDir, fileProperties);
  }

  public static LaunchConfig createWithBaseDir(ClassLoader classLoader, File baseDir, Properties properties) {
    try {
      String handlerFactoryClassName = properties.getProperty(Property.HANDLER_FACTORY);
      if (handlerFactoryClassName == null) {
        throw new LaunchException("No handler factory class specified (config property: " + Property.HANDLER_FACTORY + ")");
      }

      Class<?> untypedClass = classLoader.loadClass(handlerFactoryClassName);
      if (!HandlerFactory.class.isAssignableFrom(untypedClass)) {
        throw new LaunchException("Given handler factory class '" + handlerFactoryClassName + "' does not implement '" + HandlerFactory.class.getName());
      }

      @SuppressWarnings("unchecked") Class<HandlerFactory> handlerFactoryClass = (Class<HandlerFactory>) untypedClass;

      int port = Integer.valueOf(properties.getProperty(Property.PORT, new Integer(LaunchConfig.DEFAULT_PORT).toString()));

      InetAddress address = null;
      String addressString = properties.getProperty(Property.ADDRESS);

      if (addressString != null) {
        try {
          address = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
          throw new IllegalStateException("Failed to resolve requested bind address: " + addressString, e);
        }
      }

      URI publicAddress = null;
      String publicAddressString = properties.getProperty(Property.PUBLIC_ADDRESS);

      if (publicAddressString != null) {
        try {
          publicAddress = new URI(publicAddressString);
        } catch (URISyntaxException ex) {
          throw new IllegalStateException("Failed to create URI from: " + publicAddressString, ex);
        }
      }

      boolean reloadable = Boolean.parseBoolean(properties.getProperty(Property.RELOADABLE, "false"));
      int mainThreads = Integer.valueOf(properties.getProperty(Property.MAIN_THREADS, "0"));

      String indexFilesValue = properties.getProperty(Property.INDEX_FILES, "");
      ImmutableList.Builder<String> trimmed = ImmutableList.builder();
      for (String value : indexFilesValue.split(",")) {
        value = value.trim();
        if (!value.isEmpty()) {
          trimmed.add(value);
        }
      }
      List<String> indexFiles = trimmed.build();

      Map<String, String> otherProperties = new HashMap<>();
      extractProperties("other.", properties, otherProperties);

      HandlerFactory handlerFactory;
      try {
        handlerFactory = handlerFactoryClass.newInstance();
      } catch (Exception e) {
        throw new LaunchException("Could not instantiate handler factory: " + handlerFactoryClass.getName(), e);
      }

      return LaunchConfigBuilder.baseDir(baseDir)
        .port(port)
        .address(address)
        .publicAddress(publicAddress)
        .reloadable(reloadable)
        .mainThreads(mainThreads)
        .indexFiles(indexFiles)
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

  public final class Property {
    private Property() {
    }

    /**
     * The port to listen for requests on. Defaults to {@link LaunchConfig#DEFAULT_PORT}.
     *
     * <p>
     *
     * <b>Value:</b> {@value}
     */
    public static final String PORT = "port";

    /**
     * The address to bind to. Defaults to {@code null} (all addresses).
     *
     * <p>
     *
     * If the value is not {@code null}, it will converted to an Inet Address via {@link java.net.InetAddress#getByName(String)}.
     *
     * <p>
     *
     * <b>Value:</b> {@value} - (inet address)
     */
    public static final String ADDRESS = "address";

    /**
     * Whether to reload the application if the script changes at runtime. Defaults to {@code false}.
     *
     * <p>
     *
     * <b>Value:</b> {@value} - (boolean)
     */
    public static final String RELOADABLE = "reloadable";

    /**
     * The full qualified classname of the handler factory (required).
     *
     * <p>
     *
     * <b>Value:</b> {@value} - (string)
     */
    public static final String HANDLER_FACTORY = "handlerFactory";

    /**
     * The number of worker threads to use. Defaults to 0.
     *
     * <p>
     *
     * <b>Value:</b> {@value} - (int)
     */
    public static final String MAIN_THREADS = "mainThreads";

    /**
     * The public address of the site.
     *
     * <p>
     * If the value is not {@code null}, it will converted to an URL.
     *
     * <p> <b>Value:</b> {@value} - (url)
     */
    public static final String PUBLIC_ADDRESS = "publicAddress";

    /**
     * The comma separated list of file names of files that can be served in place of a directory.
     *
     * <p>
     * If the value is not {@code null}, it will be converted to a string list by splitting on ",".
     *
     * <p> <b>Value:</b> {@value} - (comma separated string list)
     */
    public static final String INDEX_FILES = "indexFiles";
  }
}
