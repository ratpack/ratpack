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
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.launch.internal.DefaultLaunchConfig;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
 * </pre>
 *
 * @see #baseDir(java.io.File)
 */
@SuppressWarnings("UnusedDeclaration")
public class LaunchConfigBuilder {

  private final FileSystemBinding baseDir;

  private int port = LaunchConfig.DEFAULT_PORT;
  private InetAddress address;
  private boolean reloadable;
  private int mainThreads;
  private URI publicAddress;
  private ImmutableList.Builder<String> indexFiles = ImmutableList.builder();
  private ImmutableMap.Builder<String, String> other = ImmutableMap.builder();
  private ExecutorService backgroundExecutorService;
  private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
  private SSLContext sslContext;
  private int maxContentLength = LaunchConfig.DEFAULT_MAX_CONTENT_LENGTH;

  private LaunchConfigBuilder(Path baseDir) {
    this.baseDir = new DefaultFileSystemBinding(baseDir);
  }

  /**
   * Create a new builder, using the given file as the base dir.
   *
   * @param baseDir The base dir of the launch config
   * @return A new launch config builder
   *
   * @see ratpack.launch.LaunchConfig#getBaseDir()
   */
  public static LaunchConfigBuilder baseDir(File baseDir) {
    return baseDir(baseDir.toPath());
  }

  /**
   * Create a new builder, using the given file as the base dir.
   *
   * @param baseDir The base dir of the launch config
   * @return A new launch config builder
   *
   * @see ratpack.launch.LaunchConfig#getBaseDir()
   */
  public static LaunchConfigBuilder baseDir(Path baseDir) {
    return new LaunchConfigBuilder(baseDir.toAbsolutePath().normalize());
  }

  /**
   * Sets the port to bind to.
   * <p>
   * Default value is {@value ratpack.launch.LaunchConfig#DEFAULT_PORT}.
   *
   * @param port The port to bind to
   * @return this
   *
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
   *
   * @see LaunchConfig#getAddress()
   */
  public LaunchConfigBuilder address(InetAddress address) {
    this.address = address;
    return this;
  }

  /**
   * Whether or not the application is "reloadable".
   * <p>
   * Default value is {@code false}.
   *
   * @param reloadable Whether or not the application is "reloadable".
   * @return this
   *
   * @see LaunchConfig#isReloadable()
   */
  public LaunchConfigBuilder reloadable(boolean reloadable) {
    this.reloadable = reloadable;
    return this;
  }

  /**
   * How many request handling threads to use.
   * <p>
   * Default value is {@code 0}.
   *
   * @param threads the number of threads for handling application requests
   * @return this
   * @see LaunchConfig#getThreads()
   */
  public LaunchConfigBuilder threads(int threads) {
    this.mainThreads = threads;
    return this;
  }

  /**
   * The allocator to use when creating buffers in the application.
   * <p>
   * Default value is {@link PooledByteBufAllocator#DEFAULT}.
   *
   * @param byteBufAllocator The allocator to use when creating buffers in the application
   * @return this
   *
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
   *
   * @see LaunchConfig#getPublicAddress()
   */
  public LaunchConfigBuilder publicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
    return this;
  }

  /**
   * The max content length.
   *
   * Default value is {@value ratpack.launch.LaunchConfig#DEFAULT_MAX_CONTENT_LENGTH}
   *
   * @param maxContentLength The max content length to accept.
   * @return this
   *
   * @see LaunchConfig#getMaxContentLength()
   */
  public LaunchConfigBuilder maxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
    return this;
  }

  /**
   * Adds the given values as potential index file names.
   *
   * @param indexFiles the potential index file names.
   * @return this
   *
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
   *
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
   *
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
   *
   * @see LaunchConfig#getOther(String, String)
   */
  public LaunchConfigBuilder other(Map<String, String> other) {
    for (Map.Entry<String, String> entry : other.entrySet()) {
      other(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Builds the launch config, based on the current state and the handler factory.
   *
   * @param handlerFactory The handler factory for the application.
   * @return A newly constructed {@link LaunchConfig} based on this builder's state
   */
  public LaunchConfig build(HandlerFactory handlerFactory) {
    ExecutorService backgroundExecutorService = this.backgroundExecutorService;
    if (backgroundExecutorService == null) {
      backgroundExecutorService = Executors.newCachedThreadPool(new BackgroundThreadFactory());
    }
    return new DefaultLaunchConfig(
      baseDir,
      port,
      address,
      reloadable,
      mainThreads,
      backgroundExecutorService,
      byteBufAllocator,
      publicAddress,
      indexFiles.build(),
      other.build(),
      sslContext,
      maxContentLength,
      handlerFactory
    );
  }

  @SuppressWarnings("NullableProblems")
  private static class BackgroundThreadFactory implements ThreadFactory {

    private final ThreadGroup threadGroup = new ThreadGroup("ratpack-background-worker-group");
    private int i;

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(threadGroup, r, "ratpack-background-worker-" + i++);
    }
  }
}
