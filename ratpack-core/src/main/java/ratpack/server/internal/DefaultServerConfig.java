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

package ratpack.server.internal;

import ratpack.api.Nullable;
import ratpack.config.ConfigData;
import ratpack.config.internal.DelegatingConfigData;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.server.NoBaseDirException;
import ratpack.server.ServerConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;

public class DefaultServerConfig extends DelegatingConfigData implements ServerConfig {

  private final ServerConfigData serverConfigData;
  private final Optional<FileSystemBinding> baseDir;

  public DefaultServerConfig(ConfigData configData) {
    super(configData);
    this.serverConfigData = get("/server", ServerConfigData.class);
    baseDir = Optional.ofNullable(serverConfigData.getBaseDir()).map(DefaultFileSystemBinding::new);
  }

  @Override
  public int getPort() {
    return serverConfigData.getPort();
  }

  @Nullable
  @Override
  public InetAddress getAddress() {
    return serverConfigData.getAddress();
  }

  @Override
  public boolean isDevelopment() {
    return serverConfigData.isDevelopment();
  }

  @Override
  public int getThreads() {
    return serverConfigData.getThreads();
  }

  @Override
  public URI getPublicAddress() {
    return serverConfigData.getPublicAddress();
  }

  @Nullable
  @Override
  public SSLContext getSSLContext() {
    return serverConfigData.getSslContext();
  }

  @Override
  public int getMaxContentLength() {
    return serverConfigData.getMaxContentLength();
  }

  @Override
  public boolean isHasBaseDir() {
    return serverConfigData.getBaseDir() != null;
  }

  @Override
  public FileSystemBinding getBaseDir() throws NoBaseDirException {
    return baseDir.orElseThrow(() -> new NoBaseDirException("No base dir has been set"));
  }

}
