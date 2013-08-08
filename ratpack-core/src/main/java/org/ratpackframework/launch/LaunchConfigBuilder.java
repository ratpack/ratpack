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
import java.net.URL;
import java.util.Map;

public class LaunchConfigBuilder {

  private final File baseDir;

  private int port = LaunchConfig.DEFAULT_PORT;
  private InetAddress address;
  private boolean reloadable;
  private int workerThreads;
  private URL publicAddress;
  private ImmutableMap.Builder<String, String> other = ImmutableMap.builder();

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

  public LaunchConfigBuilder workerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
    return this;
  }

  public LaunchConfigBuilder publicAddress(URL publicAddress) {
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
    return new DefaultLaunchConfig(baseDir, port, address, reloadable, workerThreads, publicAddress, other.build(), handlerFactory);
  }
}
