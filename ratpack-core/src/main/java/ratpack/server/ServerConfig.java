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

package ratpack.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
import ratpack.api.Nullable;
import ratpack.config.ConfigData;
import ratpack.config.ConfigDataSpec;
import ratpack.config.ConfigSource;
import ratpack.config.EnvironmentParser;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.server.internal.DefaultServerConfigBuilder;
import ratpack.server.internal.ServerEnvironment;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Server configuration holder
 */
public interface ServerConfig extends ConfigData {

  /**
   * The default port for Ratpack applications, {@value}.
   */
  int DEFAULT_PORT = 5050;

  /**
   * The default max content length.
   */
  int DEFAULT_MAX_CONTENT_LENGTH = 1048576;

  /**
   * The default number of threads an application should use.
   *
   * Calculated as {@code Runtime.getRuntime().availableProcessors() * 2}.
   */
  int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  /**
   * Creates a builder configured to use no base dir, development mode and an ephemeral port.
   *
   * @return a server config builder
   */
  static Builder embedded() {
    return noBaseDir().development(true).port(0);
  }

  /**
   * Creates a builder configured to use the given base dir, development mode and an ephemeral port.
   *
   * @param baseDir the server base dir
   * @return a server config builder
   */
  static Builder embedded(Path baseDir) {
    return baseDir(baseDir).development(true).port(0);
  }

  /**
   * Creates a builder configured to use no base dir.
   *
   * @return a server config builder
   */
  static Builder noBaseDir() {
    return DefaultServerConfigBuilder.noBaseDir(ServerEnvironment.env());
  }

  /**
   * Creates a builder by finding a properties file with the default name ({@value ServerConfig.Builder#DEFAULT_BASE_DIR_MARKER_FILE_PATH}).
   *
   * @return a server config builder
   * @see #findBaseDir(String)
   */
  static Builder findBaseDir() {
    return findBaseDir(Builder.DEFAULT_BASE_DIR_MARKER_FILE_PATH);
  }

  /**
   * Creates a builder based on a properties file with the given path either on the classpath or relative to the working directory.
   * <p>
   * The file is first searched for relative to the JVM's working directory, and then as a classpath resource via the context class loader.
   * <p>
   * If found, the file will be loaded as a properties file, where entries effectively map to methods of this builder.
   * The parent directory of the file will be used as the {@link ServerConfig#getBaseDir()}.
   * <p>
   * It is typical for the properties file to be empty, and just be used to find the base dir.
   *
   * @param markerFilePath the relative path to the properties file
   * @return a server config builder
   */
  static Builder findBaseDir(String markerFilePath) {
    return DefaultServerConfigBuilder.findBaseDir(ServerEnvironment.env(), markerFilePath);
  }

  /**
   * Create a new builder, using the given file as the base dir.
   *
   * @param baseDir The base dir of the launch config
   * @return A new server config builder
   */
  static Builder baseDir(Path baseDir) {
    return DefaultServerConfigBuilder.baseDir(ServerEnvironment.env(), baseDir);
  }

  /**
   * Create a new builder, using the given file as the base dir.
   *
   * @param baseDir The base dir of the launch config
   * @return A new server config builder
   */
  static Builder baseDir(File baseDir) {
    return baseDir(baseDir.toPath());
  }

  /**
   * The port that the application should listen to requests on.
   * <p>
   * Defaults to {@value #DEFAULT_PORT}.
   *
   * @return The port that the application should listen to requests on.
   */
  int getPort();

  /**
   * The address of the interface that the application should bind to.
   * <p>
   * A value of null causes all interfaces to be bound. Defaults to null.
   *
   * @return The address of the interface that the application should bind to.
   */
  @Nullable
  InetAddress getAddress();

  /**
   * Whether or not the server is in "development" mode.
   * <p>
   * A flag for indicating to Ratpack internals that the app is under development; diagnostics and reloading are more important than performance and security.
   * <p>
   * In development mode Ratpack will leak internal information through diagnostics and stacktraces by sending them to the response.
   *
   * @return {@code true} if the server is in "development" mode
   */
  boolean isDevelopment();

  /**
   * The number of threads for handling application requests.
   * <p>
   * If the value is greater than 0, a thread pool (of this size) will be created for servicing requests and doing computation.
   * If the value is 0 (default) or less, a thread pool of size {@link Runtime#availableProcessors()} {@code * 2} will be used.
   * <p>
   * This effectively sizes the {@link ratpack.exec.ExecController#getExecutor()} thread pool size.
   *
   * @return the number of threads for handling application requests.
   */
  int getThreads();

  /**
   * The public address of the site used for redirects.
   *
   * @return The url of the public address
   */
  URI getPublicAddress();

  /**
   * The SSL context to use if the application will serve content over HTTPS.
   *
   * @return The SSL context or <code>null</code> if the application does not use SSL.
   */
  @Nullable
  SSLContext getSSLContext();

  /**
   * The max content length to use for the HttpObjectAggregator.
   *
   * @return The max content length as an int.
   */
  int getMaxContentLength();

  /**
   * Whether or not the base dir of the application has been set.
   *
   * @return whether or not the base dir of the application has been set.
   */
  boolean isHasBaseDir();

  /**
   * The base dir of the application, which is also the initial {@link ratpack.file.FileSystemBinding}.
   *
   * @return The base dir of the application.
   * @throws NoBaseDirException if this launch config has no base dir set.
   */
  FileSystemBinding getBaseDir() throws NoBaseDirException;

  interface Builder extends ConfigDataSpec {

    String DEFAULT_ENV_PREFIX = "RATPACK_";
    String DEFAULT_PROP_PREFIX = "ratpack.";

    /**
     * The default name for the base dir sentinel properties file.
     * <p>
     * Value: {@value}
     *
     * @see #findBaseDir()
     */
    String DEFAULT_BASE_DIR_MARKER_FILE_PATH = ".ratpack";

    /**
     * Sets the port to listen for requests on.
     * <p>
     * Defaults to {@value ServerConfig#DEFAULT_PORT}.
     *
     * @param port the port to listen for requests on
     * @return {@code this}
     * @see ServerConfig#getPort()
     */
    Builder port(int port);

    /**
     * Sets the address to bind to.
     * <p>
     * Default value is {@code null}.
     *
     * @param address The address to bind to
     * @return {@code this}
     * @see ServerConfig#getAddress()
     */
    Builder address(InetAddress address);

    /**
     * Whether or not the application is "development".
     * <p>
     * Default value is {@code false}.
     *
     * @param development Whether or not the application is "development".
     * @return {@code this}
     * @see ServerConfig#isDevelopment()
     */
    Builder development(boolean development);

    /**
     * The number of threads to use.
     * <p>
     * Defaults to {@link ServerConfig#DEFAULT_THREADS}
     *
     * @param threads the size of the event loop thread pool
     * @return {@code this}
     * @see ServerConfig#getThreads()
     */
    Builder threads(int threads);

    /**
     * The public address of the application.
     * <p>
     * Default value is {@code null}.
     *
     * @param publicAddress the public address of the application
     * @return {@code this}
     * @see ServerConfig#getPublicAddress()
     */
    Builder publicAddress(URI publicAddress);

    /**
     * The max number of bytes a request body can be.
     *
     * Default value is {@code 1048576} (1 megabyte).
     *
     * @param maxContentLength the max content length to accept
     * @return {@code this}
     * @see ServerConfig#getMaxContentLength()
     */
    Builder maxContentLength(int maxContentLength);

    /**
     * The SSL context to use if the application serves content over HTTPS.
     *
     * @param sslContext the SSL context
     * @return {@code this}
     * @see ratpack.ssl.SSLContexts
     * @see ServerConfig#getSSLContext()
     */
    Builder ssl(SSLContext sslContext);

    /**
     * Adds a configuration source for environment variables starting with the prefix {@value ratpack.server.internal.DefaultServerConfigBuilder#DEFAULT_ENV_PREFIX}.
     *
     * @return {@code this}
     */
    @Override
    Builder env();

    /**
     * Adds a configuration source for environment variables starting with the specified prefix.
     *
     * @param prefix the prefix which should be used to identify relevant environment variables;
     * the prefix will be removed before loading the data
     * @return {@code this}
     */
    @Override
    Builder env(String prefix);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param byteSource the source of the properties data
     * @return {@code this}
     */
    @Override
    Builder props(ByteSource byteSource);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param path the source of the properties data
     * @return {@code this}
     */
    @Override
    Builder props(String path);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param path the source of the properties data
     * @return {@code this}
     */
    @Override
    Builder props(Path path);

    /**
     * Adds a configuration source for a properties object.
     *
     * @param properties the properties object
     * @return {@code this}
     */
    @Override
    Builder props(Properties properties);

    /**
     * Adds a configuration source for a Map (flat key-value pairs).
     *
     * @param map the map
     * @return {@code this}
     */
    @Override
    Builder props(Map<String, String> map);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param url the source of the properties data
     * @return {@code this}
     */
    @Override
    Builder props(URL url);

    /**
     * Adds a configuration source for system properties starting with the prefix {@value ratpack.server.internal.DefaultServerConfigBuilder#DEFAULT_PROP_PREFIX}
     *
     * @return {@code this}
     */
    @Override
    Builder sysProps();

    /**
     * Adds a configuration source for system properties starting with the specified prefix.
     *
     * @param prefix the prefix which should be used to identify relevant system properties;
     * the prefix will be removed before loading the data
     * @return {@code this}
     */
    @Override
    Builder sysProps(String prefix);

    @Override
    Builder onError(Action<? super Throwable> errorHandler);

    @Override
    Builder configureObjectMapper(Action<ObjectMapper> action);

    @Override
    Builder add(ConfigSource configSource);

    @Override
    Builder env(String prefix, Function<String, String> mapFunc);

    @Override
    Builder env(EnvironmentParser environmentParser);

    @Override
    Builder json(ByteSource byteSource);

    @Override
    Builder json(Path path);

    @Override
    Builder json(String path);

    @Override
    Builder json(URL url);

    @Override
    Builder yaml(ByteSource byteSource);

    @Override
    Builder yaml(Path path);

    @Override
    Builder yaml(String path);

    @Override
    Builder yaml(URL url);

    /**
     * Builds the server config.
     *
     * @return a server config
     */
    @Override
    ServerConfig build();
  }
}
