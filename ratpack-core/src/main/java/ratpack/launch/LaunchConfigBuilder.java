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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.api.Nullable;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.launch.internal.DefaultLaunchConfig;
import ratpack.registry.Registries;
import ratpack.registry.Registry;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A builder for {@link LaunchConfig} objects.
 * <p>
 * The following is the minimum requirement for creating a launch config…
 * <pre class="tested">
 * import ratpack.launch.*;
 * import ratpack.handling.*;
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("some/path")).build(
 *   new HandlerFactory() {
 *     public Handler create(LaunchConfig launchConfig) {
 *       return new Handler() {
 *         public void handle(Context context) {
 *           context.getResponse().send("Hello World!");
 *         }
 *       };
 *     }
 *   }
 * );
 *
 * launchConfig.execController.close();
 * </pre>
 *
 * @see #baseDir(java.io.File)
 */
@SuppressWarnings("UnusedDeclaration")
public class LaunchConfigBuilder {

  private FileSystemBinding baseDir;

  private int port = LaunchConfig.DEFAULT_PORT;
  private InetAddress address;
  private boolean development;
  private int threads = LaunchConfig.DEFAULT_THREADS;
  private URI publicAddress;
  private ImmutableList.Builder<String> indexFiles = ImmutableList.builder();
  private ImmutableMap.Builder<String, String> other = ImmutableMap.builder();
  private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
  private SSLContext sslContext;
  private int maxContentLength = LaunchConfig.DEFAULT_MAX_CONTENT_LENGTH;
  private boolean timeResponses;
  private boolean compressResponses;
  private long compressionMinSize = LaunchConfig.DEFAULT_COMPRESSION_MIN_SIZE;
  private final ImmutableSet.Builder<String> compressionMimeTypeWhiteList = ImmutableSet.builder();
  private final ImmutableSet.Builder<String> compressionMimeTypeBlackList = ImmutableSet.builder();
  private Registry defaultRegistry = Registries.empty();

  private LaunchConfigBuilder() {
  }

  private LaunchConfigBuilder(Path baseDir) {
    this.baseDir = new DefaultFileSystemBinding(baseDir);
  }

  /**
   * Create a new builder, with no base dir.
   *
   * @return A new launch config builder
   */
  public static LaunchConfigBuilder noBaseDir() {
    return new LaunchConfigBuilder();
  }

  /**
   * Create a new builder, using the given file as the base dir.
   *
   * @param baseDir The base dir of the launch config
   * @return A new launch config builder
   * @see LaunchConfig#getBaseDir()
   */
  public static LaunchConfigBuilder baseDir(File baseDir) {
    return baseDir(baseDir.toPath());
  }

  /**
   * Create a new builder, using the given file as the base dir.
   *
   * @param baseDir The base dir of the launch config
   * @return A new launch config builder
   * @see LaunchConfig#getBaseDir()
   */
  public static LaunchConfigBuilder baseDir(Path baseDir) {
    return new LaunchConfigBuilder(baseDir.toAbsolutePath().normalize());
  }

  /**
   * Sets the port to bind to.
   * <p>
   * Default value is {@code 5050}.
   *
   * @param port The port to bind to
   * @return this
   * @see LaunchConfig#getPort()
   */
  public LaunchConfigBuilder port(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the address to bind to.
   * <p>
   * Default value is {@code null}.
   *
   * @param address The address to bind to
   * @return this
   * @see LaunchConfig#getAddress()
   */
  public LaunchConfigBuilder address(InetAddress address) {
    this.address = address;
    return this;
  }

  /**
   * Whether or not the application is "development".
   * <p>
   * Default value is {@code false}.
   *
   * @param development Whether or not the application is "development".
   * @return this
   * @see LaunchConfig#isDevelopment()
   */
  public LaunchConfigBuilder development(boolean development) {
    this.development = development;
    return this;
  }

  /**
   * The number of threads to use.
   * <p>
   * Defaults to {@link LaunchConfig#DEFAULT_THREADS}
   *
   * @param threads the size of the event loop thread pool
   * @return this
   * @see LaunchConfig#getThreads()
   */
  public LaunchConfigBuilder threads(int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("'threads' must be > 0");
    }
    this.threads = threads;
    return this;
  }

  /**
   * The allocator to use when creating buffers in the application.
   * <p>
   * Default value is {@link PooledByteBufAllocator#DEFAULT}.
   *
   * @param byteBufAllocator The allocator to use when creating buffers in the application
   * @return this
   * @see LaunchConfig#getBufferAllocator()
   */
  public LaunchConfigBuilder bufferAllocator(ByteBufAllocator byteBufAllocator) {
    this.byteBufAllocator = byteBufAllocator;
    return this;
  }

  /**
   * The public address of the application.
   * <p>
   * Default value is {@code null}.
   *
   * @param publicAddress The public address of the application
   * @return this
   * @see LaunchConfig#getPublicAddress()
   */
  public LaunchConfigBuilder publicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
    return this;
  }

  /**
   * The max number of bytes a request body can be.
   *
   * Default value is {@code 1048576} (1 megabyte).
   *
   * @param maxContentLength The max content length to accept.
   * @return this
   * @see LaunchConfig#getMaxContentLength()
   */
  public LaunchConfigBuilder maxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
    return this;
  }

  /**
   * Whether to time responses.
   *
   * Default value is {@code false}.
   *
   * @param timeResponses Whether to time responses
   * @return this
   * @see LaunchConfig#isTimeResponses()
   */
  public LaunchConfigBuilder timeResponses(boolean timeResponses) {
    this.timeResponses = timeResponses;
    return this;
  }

  /**
   * Whether to compress responses.
   *
   * Default value is {@code false}.
   *
   * @param compressResponses Whether to compress responses
   * @return this
   * @see LaunchConfig#isCompressResponses()
   */
  public LaunchConfigBuilder compressResponses(boolean compressResponses) {
    this.compressResponses = compressResponses;
    return this;
  }

  /**
   * The minimum size at which responses should be compressed, in bytes.
   *
   * @param compressionMinSize The minimum size at which responses should be compressed, in bytes
   * @return this
   * @see LaunchConfig#getCompressionMinSize()
   */
  public LaunchConfigBuilder compressionMinSize(long compressionMinSize) {
    this.compressionMinSize = compressionMinSize;
    return this;
  }

  /**
   * Adds the given values as compressible mime types.
   *
   * @param mimeTypes the compressible mime types.
   * @return this
   * @see LaunchConfig#getCompressionMimeTypeWhiteList()
   */
  public LaunchConfigBuilder compressionWhiteListMimeTypes(String... mimeTypes) {
    this.compressionMimeTypeWhiteList.add(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as compressible mime types.
   *
   * @param mimeTypes the compressible mime types.
   * @return this
   * @see LaunchConfig#getCompressionMimeTypeWhiteList()
   */
  public LaunchConfigBuilder compressionWhiteListMimeTypes(List<String> mimeTypes) {
    this.compressionMimeTypeWhiteList.addAll(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as non-compressible mime types.
   *
   * @param mimeTypes the non-compressible mime types.
   * @return this
   * @see LaunchConfig#getCompressionMimeTypeBlackList()
   */
  public LaunchConfigBuilder compressionBlackListMimeTypes(String... mimeTypes) {
    this.compressionMimeTypeBlackList.add(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as non-compressible mime types.
   *
   * @param mimeTypes the non-compressible mime types.
   * @return this
   * @see LaunchConfig#getCompressionMimeTypeBlackList()
   */
  public LaunchConfigBuilder compressionBlackListMimeTypes(List<String> mimeTypes) {
    this.compressionMimeTypeBlackList.addAll(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as potential index file names.
   *
   * @param indexFiles the potential index file names.
   * @return this
   * @see LaunchConfig#getIndexFiles()
   */
  public LaunchConfigBuilder indexFiles(String... indexFiles) {
    this.indexFiles.add(indexFiles);
    return this;
  }

  /**
   * Adds the given values as potential index file names.
   *
   * @param indexFiles the potential index file names.
   * @return this
   * @see LaunchConfig#getIndexFiles()
   */
  public LaunchConfigBuilder indexFiles(List<String> indexFiles) {
    this.indexFiles.addAll(indexFiles);
    return this;
  }

  /**
   * The SSL context to use if the application serves content over HTTPS.
   *
   * @param sslContext the SSL context.
   * @return this
   * @see ratpack.ssl.SSLContexts
   * @see LaunchConfig#getSSLContext()
   */
  public LaunchConfigBuilder ssl(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * Add an "other" property.
   *
   * @param key The key of the property
   * @param value The value of the property
   * @return this
   * @see LaunchConfig#getOther(String, String)
   */
  public LaunchConfigBuilder other(String key, String value) {
    other.put(key, value);
    return this;
  }

  /**
   * Add some "other" properties.
   *
   * @param other A map of properties to add to the launch config other properties
   * @return this
   * @see LaunchConfig#getOther(String, String)
   */
  public LaunchConfigBuilder other(Map<String, String> other) {
    for (Map.Entry<String, String> entry : other.entrySet()) {
      other(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * The default registry for the built launch config.
   *
   * @param defaultRegistry the default registry
   * @return this
   */
  public LaunchConfigBuilder defaultRegistry(Registry defaultRegistry) {
    this.defaultRegistry = defaultRegistry;
    return this;
  }

  /**
   * Builds the launch config, based on the current state and the handler factory.
   * <p>
   * Supplying {@code null} for the {@code handlerFactory} will result in a launch config that can't be used to start a server.
   * This is the same as calling {@link #build()}.
   *
   * @param handlerFactory The handler factory for the application
   * @return A newly constructed {@link LaunchConfig} based on this builder's state
   */
  public LaunchConfig build(@Nullable HandlerFactory handlerFactory) {
    return new DefaultLaunchConfig(
      baseDir,
      port,
      address,
      development,
      threads,
      byteBufAllocator,
      publicAddress,
      indexFiles.build(),
      other.build(),
      sslContext,
      maxContentLength,
      timeResponses,
      compressResponses,
      compressionMinSize,
      compressionMimeTypeWhiteList.build(),
      compressionMimeTypeBlackList.build(),
      handlerFactory,
      defaultRegistry
    );
  }

  /**
   * Builds the launch config, based on the current state and WITHOUT handler factory.
   * <p>
   * This variant is really only useful for using the resultant launch config for testing purposes.
   *
   * @return A newly constructed {@link LaunchConfig} based on this builder's state
   */
  public LaunchConfig build() {
    return build(null);
  }

}
