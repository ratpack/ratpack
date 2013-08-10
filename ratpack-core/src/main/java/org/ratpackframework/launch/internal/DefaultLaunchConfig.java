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

package org.ratpackframework.launch.internal;

import com.google.common.collect.ImmutableMap;
import org.ratpackframework.launch.HandlerFactory;
import org.ratpackframework.launch.LaunchConfig;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public class DefaultLaunchConfig implements LaunchConfig {

  private final File baseDir;
  private final HandlerFactory handlerFactory;
  private final int port;
  private final InetAddress address;
  private final boolean reloadable;
  private final int mainThreads;
  private final ExecutorService blockingExecutorService;
  private final URL publicAddress;
  private final ImmutableMap<String, String> other;

  public DefaultLaunchConfig(File baseDir, int port, InetAddress address, boolean reloadable, int mainThreads, ExecutorService blockingExecutorService, URL publicAddress, ImmutableMap<String, String> other, HandlerFactory handlerFactory) {
    this.baseDir = baseDir;
    this.port = port;
    this.address = address;
    this.reloadable = reloadable;
    this.mainThreads = mainThreads;
    this.blockingExecutorService = blockingExecutorService;
    this.publicAddress = publicAddress;
    this.other = other;
    this.handlerFactory = handlerFactory;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public HandlerFactory getHandlerFactory() {
    return handlerFactory;
  }

  public int getPort() {
    return port;
  }

  public InetAddress getAddress() {
    return address;
  }

  public boolean isReloadable() {
    return reloadable;
  }

  public int getMainThreads() {
    return mainThreads;
  }

  @Override
  public ExecutorService getBlockingExecutorService() {
    return blockingExecutorService;
  }

  public URL getPublicAddress() {
    return publicAddress;
  }

  public String getOther(String key, String defaultValue) {
    String value = other.get(key);
    return value == null ? defaultValue : value;
  }
}
