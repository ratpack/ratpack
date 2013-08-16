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

import org.ratpackframework.api.Nullable;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;

public interface LaunchConfig {

  public static final int DEFAULT_PORT = 5050;

  public File getBaseDir();

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
   * If the value is greater than 0, a thread pool (of this size) will be created for servicing requests. This allows handlers
   * to perform blocking operations.
   * <p>
   * If the value is 0 or less, no thread pool will be used to handle requests. This means that the handler will be called on the
   * same thread that accepted the request. This means that handlers SHOULD NOT block in their operation.
   * <p>
   * The default value for this property is calculated as: {@code Runtime.getRuntime().availableProcessors() * 2}
   *
   * @return The number of threads to use to execute the handler.
   */
  public int getMainThreads();

  public ExecutorService getBlockingExecutorService();

  /**
   * The public address of the site used for redirects.
   *
   * @return The url of the public address;
   */
  public URI getPublicAddress();

  public String getOther(String key, String defaultValue);

}
