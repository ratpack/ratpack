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

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import ratpack.api.Nullable;
import ratpack.file.FileSystemBinding;
import ratpack.server.internal.DefaultServerConfigBuilder;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Server configuration holder
 */
public interface ServerConfig {

  /**
   * The default port for Ratpack applications, {@value}.
   */
  public static final int DEFAULT_PORT = 5050;

  /**
   * The default max content length.
   */
  public int DEFAULT_MAX_CONTENT_LENGTH = 1048576;

  /**
   * The default number of threads an application should use.
   *
   * Calculated as {@code Runtime.getRuntime().availableProcessors() * 2}.
   */
  public int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  /**
   * The default compression minimum size in bytes, {@value}.
   */
  public long DEFAULT_COMPRESSION_MIN_SIZE = 1024;

  static Builder embedded() {
    return noBaseDir().development(true).port(0);
  }

  static Builder embedded(Path baseDir) {
    return baseDir(baseDir).development(true).port(0);
  }

  static Builder noBaseDir() {
    return DefaultServerConfigBuilder.noBaseDir(ServerEnvironment.env());
  }

  static Builder findBaseDirProps() {
    return findBaseDirProps(Builder.DEFAULT_PROPERTIES_FILE_NAME);
  }

  static Builder findBaseDirProps(String propertiesPath) {
    return DefaultServerConfigBuilder.findBaseDirProps(ServerEnvironment.env(), propertiesPath);
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
  public int getPort();

  /**
   * The address of the interface that the application should bind to.
   * <p>
   * A value of null causes all interfaces to be bound. Defaults to null.
   *
   * @return The address of the interface that the application should bind to.
   */
  @Nullable
  public InetAddress getAddress();

  /**
   * Whether or not the server is in "development" mode.
   * <p>
   * A flag for indicating to Ratpack internals that the app is under development; diagnostics and reloading are more important than performance and security.
   * <p>
   * In development mode Ratpack will leak internal information through diagnostics and stacktraces by sending them to the response.
   *
   * @return {@code true} if the server is in "development" mode
   */
  public boolean isDevelopment();

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
  public int getThreads();

  /**
   * The public address of the site used for redirects.
   *
   * @return The url of the public address
   */
  public URI getPublicAddress();

  /**
   * The SSL context to use if the application will serve content over HTTPS.
   *
   * @return The SSL context or <code>null</code> if the application does not use SSL.
   */
  @Nullable
  public SSLContext getSSLContext();

  /**
   * The max content length to use for the HttpObjectAggregator.
   *
   * @return The max content length as an int.
   */
  public int getMaxContentLength();

  /**
   * Indicates whether responses should include a 'X-Response-Time' header with the number of milliseconds (to 5 decimal places) it took to process the request.
   * <p>
   * Timing starts when processing of the request starts.
   * That is, the number of milliseconds it took to determine the response to send to the client.
   * It does not include the time taken to send the response over the wire.
   *
   * @return whether or not responses should be timed.
   */
  public boolean isTimeResponses();

  /**
   * Whether or not responses should be compressed.
   *
   * @return whether or not responses should be compressed.
   */
  public boolean isCompressResponses();

  /**
   * The minimum size at which responses should be compressed, in bytes.
   *
   * @return the minimum size at which responses should be compressed.
   */
  public long getCompressionMinSize();

  /**
   * The response mime types which should be compressed.
   * <p>
   * If empty, defaults to all mime types not on the black list.
   *
   * @return the response mime types which should be compressed.
   */
  public ImmutableSet<String> getCompressionMimeTypeWhiteList();

  /**
   * The response mime types which should not be compressed.
   * <p>
   * If empty, uses a default that excludes many commonly used compressed types.
   *
   * @return the response mime types which should not be compressed.
   */
  public ImmutableSet<String> getCompressionMimeTypeBlackList();

  /**
   * Whether or not the base dir of the application has been set.
   *
   * @return whether or not the base dir of the application has been set.
   */
  public boolean isHasBaseDir();

  /**
   * The base dir of the application, which is also the initial {@link ratpack.file.FileSystemBinding}.
   *
   * @return The base dir of the application.
   * @throws NoBaseDirException if this launch config has no base dir set.
   */
  public FileSystemBinding getBaseDir() throws NoBaseDirException;

  interface Builder {

    String DEFAULT_ENV_PREFIX = "RATPACK_";
    String DEFAULT_PROP_PREFIX = "ratpack.";
    String DEFAULT_PROPERTIES_FILE_NAME = "ratpack.properties";

    Builder port(int port);

    /**
     * Sets the address to bind to.
     * <p>
     * Default value is {@code null}.
     *
     * @param address The address to bind to
     * @return this
     * @see ServerConfig#getAddress()
     */
    Builder address(InetAddress address);

    /**
     * Whether or not the application is "development".
     * <p>
     * Default value is {@code false}.
     *
     * @param development Whether or not the application is "development".
     * @return this
     * @see ServerConfig#isDevelopment()
     */
    Builder development(boolean development);

    /**
     * The number of threads to use.
     * <p>
     * Defaults to {@link ServerConfig#DEFAULT_THREADS}
     *
     * @param threads the size of the event loop thread pool
     * @return this
     * @see ServerConfig#getThreads()
     */
    Builder threads(int threads);

    /**
     * The public address of the application.
     * <p>
     * Default value is {@code null}.
     *
     * @param publicAddress The public address of the application
     * @return this
     * @see ServerConfig#getPublicAddress()
     */
    Builder publicAddress(URI publicAddress);

    /**
     * The max number of bytes a request body can be.
     *
     * Default value is {@code 1048576} (1 megabyte).
     *
     * @param maxContentLength The max content length to accept.
     * @return this
     * @see ServerConfig#getMaxContentLength()
     */
    Builder maxContentLength(int maxContentLength);

    /**
     * Whether to time responses.
     *
     * Default value is {@code false}.
     *
     * @param timeResponses Whether to time responses
     * @return this
     * @see ServerConfig#isTimeResponses()
     */
    Builder timeResponses(boolean timeResponses);

    /**
     * Whether to compress responses.
     *
     * Default value is {@code false}.
     *
     * @param compressResponses Whether to compress responses
     * @return this
     * @see ServerConfig#isCompressResponses()
     */
    Builder compressResponses(boolean compressResponses);

    /**
     * The minimum size at which responses should be compressed, in bytes.
     *
     * @param compressionMinSize The minimum size at which responses should be compressed, in bytes
     * @return this
     * @see ServerConfig#getCompressionMinSize()
     */
    Builder compressionMinSize(long compressionMinSize);

    /**
     * Adds the given values as compressible mime types.
     *
     * @param mimeTypes the compressible mime types.
     * @return this
     * @see ServerConfig#getCompressionMimeTypeWhiteList()
     */
    Builder compressionWhiteListMimeTypes(String... mimeTypes);

    /**
     * Adds the given values as compressible mime types.
     *
     * @param mimeTypes the compressible mime types.
     * @return this
     * @see ServerConfig#getCompressionMimeTypeWhiteList()
     */
    Builder compressionWhiteListMimeTypes(List<String> mimeTypes);

    /**
     * Adds the given values as non-compressible mime types.
     *
     * @param mimeTypes the non-compressible mime types.
     * @return this
     * @see ServerConfig#getCompressionMimeTypeBlackList()
     */
    Builder compressionBlackListMimeTypes(String... mimeTypes);

    /**
     * Adds the given values as non-compressible mime types.
     *
     * @param mimeTypes the non-compressible mime types.
     * @return this
     * @see ServerConfig#getCompressionMimeTypeBlackList()
     */
    Builder compressionBlackListMimeTypes(List<String> mimeTypes);

    /**
     * The SSL context to use if the application serves content over HTTPS.
     *
     * @param sslContext the SSL context.
     * @return this
     * @see ratpack.ssl.SSLContexts
     * @see ServerConfig#getSSLContext()
     */
    Builder ssl(SSLContext sslContext);

    /**
     * Adds a configuration source for environment variables starting with the prefix {@value ratpack.server.internal.DefaultServerConfigBuilder#DEFAULT_ENV_PREFIX}.
     *
     * @return this
     */
    Builder env();

    /**
     * Adds a configuration source for environment variables starting with the specified prefix.
     *
     * @param prefix the prefix which should be used to identify relevant environment variables;
     * the prefix will be removed before loading the data
     * @return this
     */
    Builder env(String prefix);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param byteSource the source of the properties data
     * @return this
     */
    Builder props(ByteSource byteSource);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param path the source of the properties data
     * @return this
     */
    Builder props(String path);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param path the source of the properties data
     * @return this
     */
    Builder props(Path path);

    /**
     * Adds a configuration source for a properties object.
     *
     * @param properties the properties object
     * @return this
     */
    Builder props(Properties properties);

    /**
     * Adds a configuration source for a Map (flat key-value pairs).
     *
     * @param map the map
     * @return this
     */
    Builder props(Map<String, String> map);

    /**
     * Adds a configuration source for a properties file.
     *
     * @param url the source of the properties data
     * @return this
     */
    Builder props(URL url);

    /**
     * Adds a configuration source for system properties starting with the prefix {@value ratpack.server.internal.DefaultServerConfigBuilder#DEFAULT_PROP_PREFIX}
     *
     * @return this
     */
    Builder sysProps();

    /**
     * Adds a configuration source for system properties starting with the specified prefix.
     *
     * @param prefix the prefix which should be used to identify relevant system properties;
     * the prefix will be removed before loading the data
     * @return this
     */
    Builder sysProps(String prefix);

    ServerConfig build();
  }
}
