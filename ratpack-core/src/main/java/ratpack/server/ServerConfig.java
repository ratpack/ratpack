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
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import ratpack.api.Nullable;
import ratpack.config.ConfigData;
import ratpack.config.ConfigDataSpec;
import ratpack.config.ConfigSource;
import ratpack.config.EnvironmentParser;
import ratpack.config.ConfigObject;
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
 * The configuration of the server.
 * <p>
 * This object represents the basic information needed to bootstrap the server (e.g. {@link #getPort()}),
 * but also provides access to any externalised config objects to be used by the application via {@link #get(String, Class)}
 * (see also: {@link #getRequiredConfig()}).
 * A server config object is-a {@link ConfigData} object.
 * <p>
 * Server config objects are programmatically built via a {@link ratpack.server.ServerConfig.Builder}, which can be obtained via
 * static methods of this type such as {@link #findBaseDir()}, {@link #noBaseDir()}, {@link #embedded()} etc.
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
   * Creates a server config builder with the {@link ServerConfig#getBaseDir() base dir} as the “directory” on the classpath that contains a file called {@code .ratpack}.
   * <p>
   * Calling this method is equivalent to calling {@link #findBaseDir(String) findBaseDir(".ratpack")}.
   *
   * @return a server config builder
   * @see #findBaseDir(String)
   */
  static Builder findBaseDir() {
    return findBaseDir(Builder.DEFAULT_BASE_DIR_MARKER_FILE_PATH);
  }

  /**
   * Creates a server config builder with the {@link ServerConfig#getBaseDir() base dir} as the “directory” on the classpath that contains the marker file at the given path.
   * <p>
   * The classpath search is performed using {@link ClassLoader#getResource(String)} using the current thread's {@link Thread#getContextClassLoader() context class loader}.
   * <p>
   * If the resource is not found, an {@link IllegalStateException} will be thrown.
   * <p>
   * If the resource is found, the enclosing directory of the resource will be converted to a {@link Path} and set as the base dir.
   * This allows a directory within side a JAR (that is on the classpath) to be used as the base dir potentially.
   *
   * @param markerFilePath the path to the marker file on the classpath
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
   * The config objects that were declared as required when this server config was built.
   * <p>
   * Required config is declared via the {@link ServerConfig.Builder#require(String, Class)} when building.
   * All required config is made part of the base registry (which the server registry joins with),
   * which automatically makes the config objects available to the server registry.
   *
   *
   * @return the declared required config
   * @see ServerConfig.Builder#require(String, Class)
   */
  ImmutableSet<ConfigObject<?>> getRequiredConfig();

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
     * {@inheritDoc}
     */
    @Override
    Builder env();

    /**
     * {@inheritDoc}
     */
    @Override
    Builder env(String prefix);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder props(ByteSource byteSource);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder props(String path);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder props(Path path);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder props(Properties properties);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder props(Map<String, String> map);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder props(URL url);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder sysProps();

    /**
     * {@inheritDoc}
     */
    @Override
    Builder sysProps(String prefix);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder onError(Action<? super Throwable> errorHandler);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder configureObjectMapper(Action<ObjectMapper> action);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder add(ConfigSource configSource);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder env(String prefix, Function<String, String> mapFunc);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder env(EnvironmentParser environmentParser);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder json(ByteSource byteSource);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder json(Path path);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder json(String path);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder json(URL url);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder yaml(ByteSource byteSource);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder yaml(Path path);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder yaml(String path);

    /**
     * {@inheritDoc}
     */
    @Override
    Builder yaml(URL url);

    /**
     * Declares that it is required that the server config provide an object of the given type at the given path.
     * <p>
     * The {@link #build()} method will fail if the config is not able to provide the requested object.
     * <p>
     * All objects declared using this method will also automatically be implicitly added to the base registry.
     * <p>
     * The {@code pointer} argument is of the same format given to the {@link ConfigData#get(String, Class)} method.
     * <pre class="java">{@code
     * import junit.framework.Assert;
     * import ratpack.server.ServerConfig;
     * import ratpack.test.embed.EmbeddedApp;
     *
     * import java.util.Collections;
     *
     * public class Example {
     *   static class MyConfig {
     *     public String value;
     *   }
     *
     *   public static void main(String... args) throws Exception {
     *     EmbeddedApp.of(a -> a
     *         .serverConfig(ServerConfig.embedded()
     *             .props(Collections.singletonMap("config.value", "foo"))
     *             .require("/config", MyConfig.class)
     *         )
     *         .handlers(c -> c
     *             .get(ctx -> ctx.render(ctx.get(MyConfig.class).value))
     *         )
     *     ).test(httpClient ->
     *       Assert.assertEquals("foo", httpClient.getText())
     *     );
     *   }
     * }
     * }</pre>
     *
     * @param pointer a <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a> specifying the point in the configuration data to bind from
     * @param type the class of the type to bind to
     * @return {@code this}
     */
    Builder require(String pointer, Class<?> type);

    /**
     * Builds the server config.
     *
     * @return a server config
     */
    @Override
    ServerConfig build();
  }
}
