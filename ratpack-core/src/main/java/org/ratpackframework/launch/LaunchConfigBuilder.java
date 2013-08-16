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

import com.google.common.collect.ImmutableMap;
import org.ratpackframework.launch.internal.DefaultLaunchConfig;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@SuppressWarnings("UnusedDeclaration")
public class LaunchConfigBuilder {

  private final File baseDir;

  private int port = LaunchConfig.DEFAULT_PORT;
  private InetAddress address;
  private boolean reloadable;
  private int mainThreads;
  private URI publicAddress;
  private ImmutableMap.Builder<String, String> other = ImmutableMap.builder();
  private ExecutorService blockingExecutorService;

  private LaunchConfigBuilder(File baseDir) {
    this.baseDir = baseDir;
  }

  public static LaunchConfigBuilder baseDir(File baseDir) {
    return new LaunchConfigBuilder(baseDir);
  }

  public LaunchConfigBuilder port(int port) {
    this.port = port;
    return this;
  }

  public LaunchConfigBuilder address(InetAddress address) {
    this.address = address;
    return this;
  }

  public LaunchConfigBuilder reloadable(boolean reloadable) {
    this.reloadable = reloadable;
    return this;
  }

  public LaunchConfigBuilder mainThreads(int mainThreads) {
    this.mainThreads = mainThreads;
    return this;
  }

  public LaunchConfigBuilder blockingExecutorService(ExecutorService executorService) {
    this.blockingExecutorService = executorService;
    return this;
  }

  public LaunchConfigBuilder publicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
    return this;
  }

  public LaunchConfigBuilder other(String key, String value) {
    other.put(key, value);
    return this;
  }

  public LaunchConfigBuilder other(Map<String, String> other) {
    for (Map.Entry<String, String> entry : other.entrySet()) {
      other(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public LaunchConfig build(HandlerFactory handlerFactory) {
    ExecutorService blockingExecutorService = this.blockingExecutorService;
    if (blockingExecutorService == null) {
      blockingExecutorService = Executors.newCachedThreadPool(new BlockingThreadFactory());
    }
    return new DefaultLaunchConfig(baseDir, port, address, reloadable, mainThreads, blockingExecutorService, publicAddress, other.build(), handlerFactory);
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
