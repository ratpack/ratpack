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

package ratpack.launch;

import com.google.common.collect.Iterables;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import ratpack.api.Nullable;
import ratpack.ssl.SSLContexts;
import ratpack.util.internal.TypeCoercingProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * Static factory methods for creating {@link LaunchConfig} objects using various strategies. <p> Designed to be used to construct a launch config via a {@link Properties} instance. <p> Most methods
 * eventually delegate to {@link #createWithBaseDir(ClassLoader, java.nio.file.Path, java.util.Properties)}. <p> When things go wrong, a {@link LaunchException} will be thrown.
 */
public abstract class LaunchConfigs {

  /**
   * Value: {@value}.
   *
   * @see #createFromGlobalProperties(ClassLoader, java.util.Properties, java.util.Properties)
   */
  public static final String SYSPROP_PREFIX_PROPERTY = "ratpack.syspropPrefix";

  /**
   * Value: {@value}.
   *
   * @see #createFromGlobalProperties(ClassLoader, java.util.Properties, java.util.Properties)
   */
  public static final String SYSPROP_PREFIX_DEFAULT = "ratpack.";

  public static final String CONFIG_RESOURCE_PROPERTY = "configResource";
  public static final String CONFIG_RESOURCE_DEFAULT = "ratpack.properties";

  private LaunchConfigs() {
  }

  /**
   * Delegates to {@link #createFromGlobalProperties(ClassLoader, String, java.util.Properties, java.util.Properties)}, extracting the “propertyPrefix” from the “globalProperties”. <p> Determines the
   * “propertyPrefix” value and delegates to {@link #createFromGlobalProperties(ClassLoader, String, java.util.Properties, java.util.Properties)}. The value is determined by: {@code
   * globalProperties.getProperty(SYSPROP_PREFIX_PROPERTY, SYSPROP_PREFIX_DEFAULT)}
   *
   * @param classLoader The classloader to use to find the properties file.
   * @param globalProperties The environmental (override) properties
   * @param defaultProperties The default properties to use when a property is omitted
   * @return A launch config
   */
  public static LaunchConfig createFromGlobalProperties(ClassLoader classLoader, Properties globalProperties, Properties defaultProperties) {
    String propertyPrefix = globalProperties.getProperty(SYSPROP_PREFIX_PROPERTY, SYSPROP_PREFIX_DEFAULT);
    return createFromGlobalProperties(classLoader, propertyPrefix, globalProperties, defaultProperties);
  }

  /**
   * Delegates to {@link #createFromProperties(ClassLoader, java.util.Properties, java.util.Properties)} after extracting the “prefixed” properties from the global properties. <p> The properties of
   * {@code globalProperties} that begin with {@code propertyPrefix} are extracted into a new property set, with the property names having the prefix removed. This becomes the {@code
   * overrideProperties} for the delegation to {@link #createFromProperties(ClassLoader, java.util.Properties, java.util.Properties)}.
   *
   * @param classLoader The classloader to use to find the properties file
   * @param propertyPrefix The prefix of properties in {@code globalProperties} that should be extracted to form the override properties
   * @param globalProperties The environmental (override) properties
   * @param defaultProperties The default properties to use when a property is omitted
   * @return A launch config
   */
  public static LaunchConfig createFromGlobalProperties(ClassLoader classLoader, String propertyPrefix, Properties globalProperties, Properties defaultProperties) {
    Properties deprefixed = new Properties();
    extractProperties(propertyPrefix, globalProperties, deprefixed);
    return createFromProperties(classLoader, deprefixed, defaultProperties);
  }

  /**
   * Extracts the properties prefixed with {@value #SYSPROP_PREFIX_DEFAULT} from the system properties, without the prefix.
   *
   * @return the properties prefixed with {@value #SYSPROP_PREFIX_DEFAULT} from the system properties, without the prefix.
   */
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

  /**
   * Delegates to {@link #createFromFile(ClassLoader, java.nio.file.Path, java.nio.file.Path, java.util.Properties, java.util.Properties)}, after trying to find the config properties file. <p> The {@code
   * overrideProperties} are queried for the {@link #CONFIG_RESOURCE_PROPERTY} (defaulting to {@link #CONFIG_RESOURCE_DEFAULT}). The {@code classLoader} is asked for the classpath resource with this
   * name. <p> If the resource DOES NOT exist, {@link #createFromFile(ClassLoader, java.nio.file.Path, java.nio.file.Path, java.util.Properties, java.util.Properties)} is called with the current JVM user dir as
   * the {@code baseDir} and {@code null} as the {@code configFile}. <p> If the resource DOES exist, {@link #createFromFile(ClassLoader, java.nio.file.Path, java.nio.file.Path, java.util.Properties, java.util.Properties)} is called with that file resource as the {@code configFile}, and its parent directory as the {@code baseDir}.
   *
   * @param classLoader The classloader to use to find the properties file
   * @param overrideProperties The properties that provide the path to the config file resource, and any overrides
   * @param defaultProperties The default properties to use when a property is omitted
   * @return A launch config
   */
  public static LaunchConfig createFromProperties(ClassLoader classLoader, Properties overrideProperties, Properties defaultProperties) {
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

  /**
   * Delegates to {@link #createWithBaseDir(ClassLoader, java.nio.file.Path, java.util.Properties)}, after merging the properties from {@code configFile} and {@code overrideProperties}. <p> If {@code
   * configFile} exists and is not null, it is read as a properties file. It is then merged with {@code defaultProperties} & {@code overrideProperties}. The default properties are overridden by values
   * in the properties file, while the override properties will override these properties.
   *
   * @param classLoader The classloader to use to find the properties file
   * @param baseDir The {@link LaunchConfig#getBaseDir()} of the eventual launch config
   * @param configFile The configuration properties file
   * @param overrideProperties The properties that override default and file properties
   * @param defaultProperties The default properties to use when a property is omitted
   * @return a launch config
   */
  public static LaunchConfig createFromFile(ClassLoader classLoader, Path baseDir, @Nullable Path configFile, Properties overrideProperties, Properties defaultProperties) {
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

  /**
   * Delegates to {@link #createWithBaseDir(ClassLoader, java.nio.file.Path, java.util.Properties, java.util.Map)}, using {@link System#getenv()} as the environment variables.
   *
   * @param classLoader The classloader used to load the {@link LaunchConfigs.Property#HANDLER_FACTORY} class
   * @param baseDir The {@link LaunchConfig#getBaseDir()} value
   * @param properties The values to use to construct the launch config
   * @return A launch config
   */
  public static LaunchConfig createWithBaseDir(ClassLoader classLoader, Path baseDir, Properties properties) {
    return createWithBaseDir(classLoader, baseDir, properties, System.getenv());
  }

  /**
   * Constructs a launch config, based on the given properties and environment variables.
   * <p>
   * See {@link Property} for details on the valid properties.
   *
   * @param classLoader The classloader used to load the {@link LaunchConfigs.Property#HANDLER_FACTORY} class
   * @param baseDir The {@link LaunchConfig#getBaseDir()} value
   * @param properties The values to use to construct the launch config
   * @param envVars The environment variables to use to construct the launch config
   * @return A launch config
   */
  public static LaunchConfig createWithBaseDir(ClassLoader classLoader, Path baseDir, Properties properties, Map<String, String> envVars) {
    TypeCoercingProperties props = new TypeCoercingProperties(properties, classLoader);
    try {
      Class<HandlerFactory> handlerFactoryClass;
      handlerFactoryClass = props.asClass(Property.HANDLER_FACTORY, HandlerFactory.class);
      if (handlerFactoryClass == null) {
        throw new LaunchException("No handler factory class specified (config property: " + Property.HANDLER_FACTORY + ")");
      }

      int defaultPort = LaunchConfig.DEFAULT_PORT;
      if (envVars.containsKey("PORT")) {
        try {
          String stringValue = envVars.get("PORT");
          defaultPort = Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
          throw new LaunchException("Environment var 'PORT' is not an integer", e);
        }
      }

      int port = props.asInt(Property.PORT, defaultPort);
      InetAddress address = props.asInetAddress(Property.ADDRESS);
      URI publicAddress = props.asURI(Property.PUBLIC_ADDRESS);
      boolean reloadable = props.asBoolean(Property.RELOADABLE, false);
      int threads = props.asInt(Property.THREADS, LaunchConfig.DEFAULT_THREADS);
      List<String> indexFiles = props.asList(Property.INDEX_FILES);
      InputStream sslKeystore = props.asStream(Property.SSL_KEYSTORE_FILE);
      String sslKeystorePassword = props.asString(Property.SSL_KEYSTORE_PASSWORD, "");
      int maxContentLength = props.asInt(Property.MAX_CONTENT_LENGTH, LaunchConfig.DEFAULT_MAX_CONTENT_LENGTH);
      boolean timeResponses = props.asBoolean(Property.TIME_RESPONSES, false);
      boolean compressResponses = props.asBoolean(Property.COMPRESS_RESPONSES, false);

      Map<String, String> otherProperties = new HashMap<>();
      extractProperties("other.", properties, otherProperties);

      HandlerFactory handlerFactory;
      try {
        handlerFactory = handlerFactoryClass.newInstance();
      } catch (Exception e) {
        throw new LaunchException("Could not instantiate handler factory: " + handlerFactoryClass.getName(), e);
      }

      LaunchConfigBuilder launchConfigBuilder = LaunchConfigBuilder.baseDir(baseDir)
        .port(port)
        .address(address)
        .publicAddress(publicAddress)
        .reloadable(reloadable)
        .threads(threads)
        .maxContentLength(maxContentLength)
        .timeResponses(timeResponses)
        .compressResponses(compressResponses)
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


  /**
   * Constants for meaningful configuration properties.
   */
  public final class Property {
    private Property() {
    }

    /**
     * The port to listen for requests on.
     * <p>
     * It is also possible to set this property via an environment variable named {@code PORT}.
     * This makes deploying on cloud platforms such as Heroku more convenient.
     * If the environment variable and the system property are set, the system property takes precedence.
     * <p>
     * Defaults to {@link LaunchConfig#DEFAULT_PORT}.
     * <p>
     * <b>Value:</b> {@value}
     *
     * @see LaunchConfig#getPort()
     */
    public static final String PORT = "port";

    /**
     * The address to bind to. Defaults to {@code null} (all addresses). <p> If the value is not {@code null}, it will converted to an Inet Address via {@link java.net.InetAddress#getByName(String)}.
     * <p> <b>Value:</b> {@value} - (inet address)
     *
     * @see LaunchConfig#getAddress()
     */
    public static final String ADDRESS = "address";

    /**
     * Whether to reload the application if the script changes at runtime. Defaults to {@code false}. <p> <b>Value:</b> {@value} - (boolean)
     *
     * @see LaunchConfig#isReloadable()
     */
    public static final String RELOADABLE = "reloadable";

    /**
     * The full qualified classname of the handler factory (required). <p> This class MUST implement {@link HandlerFactory} and have a public no-arg constructor. <p> <b>Value:</b> {@value} - (string)
     *
     * @see LaunchConfig#getHandlerFactory()
     */
    public static final String HANDLER_FACTORY = "handlerFactory";

    /**
     * The number of worker threads to use. Defaults to 0. <p> <b>Value:</b> {@value} - (int)
     *
     * @see LaunchConfig#getThreads()
     */
    public static final String THREADS = "threads";

    /**
     * The public address of the site. <p> If the value is not {@code null}, it will converted to an URL. <p> <b>Value:</b> {@value} - (url)
     *
     * @see LaunchConfig#getPublicAddress()
     */
    public static final String PUBLIC_ADDRESS = "publicAddress";

    /**
     * The comma separated list of file names of files that can be served in place of a directory. <p> If the value is not {@code null}, it will be converted to a string list by splitting on ",". <p>
     * <b>Value:</b> {@value} - (comma separated string list)
     *
     * @see LaunchConfig#getIndexFiles()
     */
    public static final String INDEX_FILES = "indexFiles";

    /**
     * The absolute file path, URI or classpath location of the SSL keystore file.
     *
     * @see LaunchConfig#getSSLContext()
     */
    public static final String SSL_KEYSTORE_FILE = "ssl.keystore.file";

    /**
     * The password for the SSL keystore file.
     *
     * @see LaunchConfig#getSSLContext()
     */
    public static final String SSL_KEYSTORE_PASSWORD = "ssl.keystore.password";

    /**
     * The max conent lenght.
     *
     * @see LaunchConfig#getMaxContentLength()
     */
    public static final String MAX_CONTENT_LENGTH = "maxContentLength";

    /**
     * Whether to time responses.
     * <p>
     * The value of this property will be converted to a boolean by {@link Boolean#valueOf(String)}.
     *
     * @see LaunchConfig#isTimeResponses()
     */
    public static final String TIME_RESPONSES = "timeResponses";

    /**
     * Whether to compress responses.
     * <p>
     * The value of this property will be converted to a boolean by {@link Boolean#valueOf(String)}.
     *
     * @see LaunchConfig#isCompressResponses()
     */
    public static final String COMPRESS_RESPONSES = "compressResponses";
  }
}
