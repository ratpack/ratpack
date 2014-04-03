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

import io.netty.buffer.ByteBufAllocator;
import ratpack.api.Nullable;
import ratpack.handling.Background;
import ratpack.file.FileSystemBinding;
import ratpack.handling.Foreground;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * The configuration used to launch a server.
 *
 * @see LaunchConfigBuilder
 * @see LaunchConfigs
 * @see ratpack.server.RatpackServerBuilder#build(LaunchConfig)
 */
public interface LaunchConfig {

  /**
   * The default port for Ratpack applications, {@value}.
   */
  public static final int DEFAULT_PORT = 5050;

  /**
   * The default max content length.
   */
  public int DEFAULT_MAX_CONTENT_LENGTH = 65536;

  /**
   * The base dir of the application, which is also the initial {@link ratpack.file.FileSystemBinding}.
   *
   * @return The base dir of the application.
   */
  public FileSystemBinding getBaseDir();

  /**
   * The handler factory that can create the root handler for the application.
   *
   * @return The handler factory that can create the root handler for the application.
   */
  public HandlerFactory getHandlerFactory();

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
   * Whether or not the server is in "reloadable" (i.e. development) mode.
   * <p>
   * Different parts of the application may respond to this as they see fit.
   *
   * @return {@code true} if the server is in "reloadable" mode
   */
  public boolean isReloadable();

  /**
   * The number of threads for handling application requests.
   * <p>
   * If the value is greater than 0, a thread pool (of this size) will be created for servicing requests and doing computation.
   * If the value is 0 (default) or less, a thread pool of size {@link Runtime#availableProcessors()} {@code * 2} will be used.
   * <p>
   * This effectively sizes the {@link Foreground#getExecutor()} thread pool size.
   *
   * @return the number of threads for handling application requests.
   */
  public int getThreads();

  /**
   * The “background”, for performing blocking operations.
   *
   * @see ratpack.handling.Context#background(java.util.concurrent.Callable)
   * @return the “background”, for performing blocking operations
   */
  public Background getBackground();

  /**
   * The application foreground.
   *
   * @return the application foreground
   * @see Foreground
   */
  public Foreground getForeground();

  /**
   * The allocator for buffers needed by the application.
   * <p>
   * Defaults to Netty's {@link io.netty.buffer.PooledByteBufAllocator}.
   *
   * @return The allocator for buffers needed by the application.
   */
  public ByteBufAllocator getBufferAllocator();

  /**
   * The public address of the site used for redirects.
   *
   * @return The url of the public address
   */
  public URI getPublicAddress();

  /**
   * The names of files that can be served if a request is made to serve a directory.
   *
   * @return The names of files that can be served if a request is made to serve a directory.
   */
  public List<String> getIndexFiles();

  /**
   * The SSL context to use if the application will serve content over HTTPS.
   *
   * @return The SSL context or <code>null</code> if the application does not use SSL.
   */
  @Nullable
  public SSLContext getSSLContext();

  /**
   * Provides access to any "other" properties that were specified.
   * <p>
   * Extensions and plugins can use other properties for their configuration.
   *
   * @param key The property key
   * @param defaultValue The value to return if the property was not set
   * @return The other property for {@code key}, or the {@code defaultValue} if it is not set
   */
  public String getOther(String key, String defaultValue);


  /**
   * Provides access to all "other" properties whose name starts with a given prefix.
   * <p>
   * The prefix is removed from keys of the result map.
   *
   * @param prefix Property name prefix that should be used for filtering
   * @return A map of all "other" properties whose name starts with the prefix with the prefix removed from key names
   */
  public Map<String, String> getOtherPrefixedWith(String prefix);

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
   */
  public boolean isTimeResponses();

  /**
   * Whether or not responses should be compressed.
   *
   * @return whether or not responses should be compressed.
   */
  public boolean isCompressResponses();

}
