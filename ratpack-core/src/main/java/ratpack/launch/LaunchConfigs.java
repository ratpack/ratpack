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

import com.google.common.base.StandardSystemProperty;
import ratpack.api.Nullable;
import ratpack.launch.internal.LaunchConfigsInternal;
import ratpack.registry.Registries;
import ratpack.registry.Registry;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static ratpack.launch.internal.LaunchConfigsInternal.createLaunchConfig;
import static ratpack.util.internal.PropertiesUtil.extractProperties;

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
    return createLaunchConfig(LaunchConfigsInternal.createFromGlobalProperties(StandardSystemProperty.USER_DIR.value(), classLoader, globalProperties, defaultProperties, Registries.empty()));
  }

  /**
   * Delegates to {@link #createFromGlobalProperties(ClassLoader, String, java.util.Properties, java.util.Properties)}, extracting the “propertyPrefix” from the “globalProperties”. <p> Determines the
   * “propertyPrefix” value and delegates to {@link #createFromGlobalProperties(ClassLoader, String, java.util.Properties, java.util.Properties)}. The value is determined by: {@code
   * globalProperties.getProperty(SYSPROP_PREFIX_PROPERTY, SYSPROP_PREFIX_DEFAULT)}
   *
   * @param classLoader The classloader to use to find the properties file.
   * @param globalProperties The environmental (override) properties
   * @param defaultProperties The default properties to use when a property is omitted
   * @param defaultRegistry The default registry to use
   * @return A launch config
   */
  public static LaunchConfig createFromGlobalProperties(ClassLoader classLoader, Properties globalProperties, Properties defaultProperties, Registry defaultRegistry) {
    return createLaunchConfig(LaunchConfigsInternal.createFromGlobalProperties(StandardSystemProperty.USER_DIR.value(), classLoader, globalProperties, defaultProperties, defaultRegistry));
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
    return createLaunchConfig(LaunchConfigsInternal.createFromGlobalProperties(StandardSystemProperty.USER_DIR.value(), classLoader, propertyPrefix, globalProperties, defaultProperties, Registries.empty()));
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
    return createLaunchConfig(LaunchConfigsInternal.createFromProperties(StandardSystemProperty.USER_DIR.value(), classLoader, overrideProperties, defaultProperties, Registries.empty()));
  }

  /**
   * Delegates to {@link #createWithBaseDir(ClassLoader, java.nio.file.Path, java.util.Properties)}, after merging the properties from {@code configFile} and {@code overrideProperties}. <p> If{@code
   * configFile} exists and is not null, it is read as a properties file. It is then merged with {@code defaultProperties} &amp; {@code overrideProperties}. The default properties are overridden by
   * values in the properties file, while the override properties will override these properties.
   *
   * @param classLoader The classloader to use to find the properties file
   * @param baseDir The {@link LaunchConfig#getBaseDir()} of the eventual launch config
   * @param configFile The configuration properties file
   * @param overrideProperties The properties that override default and file properties
   * @param defaultProperties The default properties to use when a property is omitted
   * @return a launch config
   */
  public static LaunchConfig createFromFile(ClassLoader classLoader, Path baseDir, @Nullable Path configFile, Properties overrideProperties, Properties defaultProperties) {
    return createLaunchConfig(LaunchConfigsInternal.createFromFile(classLoader, baseDir, configFile, overrideProperties, defaultProperties, Registries.empty()));
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
    return createLaunchConfig(LaunchConfigsInternal.createWithBaseDir(classLoader, baseDir, properties, Registries.empty()));
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
    return createLaunchConfig(LaunchConfigsInternal.createWithBaseDir(classLoader, baseDir, properties, envVars, Registries.empty()));
  }

  /**
   * Constants for meaningful configuration environment variables.
   */
  public final class Environment {
    private Environment() {
    }

    /**
     * The port to listen for requests on.
     * <p>
     * It is also possible to set this via property {@link ratpack.launch.LaunchConfigs.Property#PORT}.
     * If the environment variable and the system property are set, the system property takes precedence.
     * <p>
     * Defaults to {@link LaunchConfig#DEFAULT_PORT}.
     * <p>
     * <b>Value:</b> {@value}
     *
     * @see LaunchConfig#getPort()
     */
    public static final String PORT = "PORT";
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
     * It is also possible to set this property via an environment variable {@link ratpack.launch.LaunchConfigs.Environment#PORT}.
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
     * Whether to have the application in development mode.
     * <p>
     * If the script changes at runtime, there will be reloads. In this mode diagnostics and reloading are treated more important than performance and security.
     * <p>
     * Defaults to {@code false}. <p> <b>Value:</b> {@value} - (boolean)
     *
     * @see LaunchConfig#isDevelopment()
     */
    public static final String DEVELOPMENT = "development";

    /**
     * The full qualified classname of the handler factory (required). <p> This class MUST implement {@link HandlerFactory} and have a public no-arg constructor. <p> <b>Value:</b> {@value} - (string)
     *
     * @see LaunchConfig#getHandlerFactory()
     */
    public static final String HANDLER_FACTORY = "handlerFactory";

    /**
     * The full qualified classname of the configuration factory (optional). <p> This class MUST implement {@code ratpack.configuration.ConfigurationFactory} and have a public no-arg constructor. </p> <b>Value:</b> {@value} - (string)
     *
     * This property is used by the {@code ratpack-configuration} module.
     */
    public static final String CONFIGURATION_FACTORY = "configurationFactory";

    /**
     * The full qualified classname of the configuration class (optional). <p> This class MUST extend {@code ratpack.configuration.Configuration} and have a public no-arg constructor. </p> <b>Value:</b> {@value} - (string)
     *
     * This property is used by the {@code ratpack-configuration} module.
     */
    public static final String CONFIGURATION_CLASS = "configurationClass";

    /**
     * The path to the desired configuration file. <p> This class MUST implement <b>Value:</b> {@value} - (string)
     *
     * This property is used by the {@code ratpack-configuration} module.
     */
    public static final String CONFIGURATION_FILE = "configurationFile";

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
     * The max content length.
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

    /**
     * The minimum size at which responses should be compressed, in bytes.
     *
     * @see LaunchConfig#getCompressionMinSize()
     */
    public static final String COMPRESSION_MIN_SIZE = "compression.minSize";

    /**
     * The comma separated list of response mime types which should be compressed.
     * <p>
     * If the value is not {@code null}, it will be converted to a string list by splitting on ",".
     * <p>
     * <b>Value:</b> {@value} - (comma separated string list)
     *
     * @see LaunchConfig#getCompressionMimeTypeWhiteList()
     */
    public static final String COMPRESSION_MIME_TYPE_WHITE_LIST = "compression.mimeType.whiteList";

    /**
     * The comma separated list of response mime types which should not be compressed.
     * <p>
     * If the value is not {@code null}, it will be converted to a string list by splitting on ",".
     * <p>
     * <b>Value:</b> {@value} - (comma separated string list)
     *
     * @see LaunchConfig#getCompressionMimeTypeBlackList()
     */
    public static final String COMPRESSION_MIME_TYPE_BLACK_LIST = "compression.mimeType.blackList";
  }
}
