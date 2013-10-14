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
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.ratpackframework.launch.internal.DefaultLaunchConfig;
import org.ratpackframework.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
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
 * import org.ratpackframework.launch.*;
 * import org.ratpackframework.handling.*;
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

  private final File baseDir;

  private int port = LaunchConfig.DEFAULT_PORT;
  private InetAddress address;
  private boolean reloadable;
  private int mainThreads;
  private URI publicAddress;
  private ImmutableList.Builder<String> indexFiles = ImmutableList.builder();
  private ImmutableMap.Builder<String, String> other = ImmutableMap.builder();
  private ExecutorService blockingExecutorService;
  private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
  private SSLContext sslContext;
  private int maxContentLength = LaunchConfig.DEFAULT_MAX_CONTENT_LENGTH;

  private LaunchConfigBuilder(File baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * Create a new builder, using the given file as the base dir.
   *
   * @param baseDir The base dir of the launch config
   * @see org.ratpackframework.launch.LaunchConfig#getBaseDir()
   * @return A new launch config builder
   */
  public static LaunchConfigBuilder baseDir(File baseDir) {
    return new LaunchConfigBuilder(baseDir);
  }

  /**
   * Sets the port to bind to.
   * <p>
   * Default value is {@value org.ratpackframework.launch.LaunchConfig#DEFAULT_PORT}.
   *
   * @param port The port to bind to
   * @see LaunchConfig#getPort()
   * @return this
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
   * @see LaunchConfig#getAddress()
   * @return this
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
   * @see LaunchConfig#isReloadable()
   * @return this
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
   * @param mainThreads The port to bind to
   * @see LaunchConfig#getMainThreads()
   * @return this
   */
  public LaunchConfigBuilder mainThreads(int mainThreads) {
    this.mainThreads = mainThreads;
    return this;
  }

  /**
   * The executor service to use for blocking operations.
   * <p>
   * Default value is {@link Executors#newCachedThreadPool()}.
   *
   * @param executorService The executor service to use for blocking operations
   * @see LaunchConfig#getBlockingExecutorService()
   * @return this
   */
  public LaunchConfigBuilder blockingExecutorService(ExecutorService executorService) {
    this.blockingExecutorService = executorService;
    return this;
  }

  /**
   * The allocator to use when creating buffers in the application.
   * <p>
   * Default value is {@link PooledByteBufAllocator#DEFAULT}.
   *
   * @param byteBufAllocator The allocator to use when creating buffers in the application
   * @see LaunchConfig#getBufferAllocator()
   * @return this
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
   * @see LaunchConfig#getPublicAddress()
   * @return this
   */
  public LaunchConfigBuilder publicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
    return this;
  }

  /**
   * The max content length.
   *
   * Default value is {@value org.ratpackframework.launch.LaunchConfig#DEFAULT_MAX_CONTENT_LENGTH}
   *
   * @param maxContentLength The max content length to accept.
   * @see LaunchConfig#getMaxContentLength()
   * @return this
   */
  public LaunchConfigBuilder maxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
    return this;
  }

  /**
   * Adds the given values as potential index file names.
   *
   * @param indexFiles the potential index file names.
   * @see LaunchConfig#getIndexFiles()
   * @return this
   */
  public LaunchConfigBuilder indexFiles(String... indexFiles) {
    this.indexFiles.add(indexFiles);
    return this;
  }

  /**
   * Adds the given values as potential index file names.
   *
   * @param indexFiles the potential index file names.
   * @see LaunchConfig#getIndexFiles()
   * @return this
   */
  public LaunchConfigBuilder indexFiles(List<String> indexFiles) {
    this.indexFiles.addAll(indexFiles);
    return this;
  }

  /**
   * The SSL context to use if the application serves content over HTTPS.
   *
   * @param sslContext the SSL context.
   * @see LaunchConfig#getSSLContext()
   * @return this
   */
  public LaunchConfigBuilder sslContext(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * A convenience method for configuring an SSL context using a password-protected keystore file.
   *
   * @see SSLContexts#sslContext(java.io.InputStream, String)
   * @see LaunchConfig#getSSLContext()
   * @return this
   */
  public LaunchConfigBuilder ssl(InputStream keyStore, String password) throws GeneralSecurityException, IOException {
    return sslContext(SSLContexts.sslContext(keyStore, password));
  }

  /**
   * A convenience method for configuring an SSL context using a password-protected keystore file.
   *
   * @see SSLContexts#sslContext(java.net.URL, String)
   * @see LaunchConfig#getSSLContext()
   * @return this
   */
  public LaunchConfigBuilder ssl(URL keyStore, String password) throws GeneralSecurityException, IOException {
    return sslContext(SSLContexts.sslContext(keyStore, password));
  }

  /**
   * A convenience method for configuring an SSL context using a password-protected keystore file.
   *
   * @see SSLContexts#sslContext(java.io.File, String)
   * @see LaunchConfig#getSSLContext()
   * @return this
   */
  public LaunchConfigBuilder ssl(File keyStore, String password) throws GeneralSecurityException, IOException {
    return sslContext(SSLContexts.sslContext(keyStore, password));
  }

  /**
   * Add an "other" property.
   *
   * @param key The key of the property
   * @param value The value of the property
   * @see LaunchConfig#getOther(String, String)
   * @return this
   */
  public LaunchConfigBuilder other(String key, String value) {
    other.put(key, value);
    return this;
  }

  /**
   * Add some "other" properties.
   *
   * @param other A map of properties to add to the launch config other properties
   * @see LaunchConfig#getOther(String, String)
   * @return this
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
    ExecutorService blockingExecutorService = this.blockingExecutorService;
    if (blockingExecutorService == null) {
      blockingExecutorService = Executors.newCachedThreadPool(new BlockingThreadFactory());
    }
    return new DefaultLaunchConfig(baseDir, port, address, reloadable, mainThreads, blockingExecutorService, byteBufAllocator, publicAddress, indexFiles.build(), other.build(), handlerFactory, sslContext, maxContentLength);
  }

  @SuppressWarnings("NullableProblems")
  private static class BlockingThreadFactory implements ThreadFactory {

    private final ThreadGroup threadGroup = new ThreadGroup("ratpack-blocking-worker-group");
    private int i;

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(threadGroup, r, "ratpack-blocking-worker-" + i++);
    }
  }
}
