/*
 * Copyright 2015 the original author or authors.
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

import com.google.common.collect.ImmutableSet;
import ratpack.api.Nullable;
import ratpack.config.ConfigObject;
import ratpack.config.internal.DelegatingConfigData;
import ratpack.file.FileSystemBinding;
import ratpack.server.NoBaseDirException;
import ratpack.server.ServerConfig;

import io.netty.handler.ssl.SslContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;

public class DelegatingServerConfig extends DelegatingConfigData implements ServerConfig {

  private final ServerConfig delegate;

  public DelegatingServerConfig(ServerConfig delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  @Override
  public ImmutableSet<ConfigObject<?>> getRequiredConfig() {
    return delegate.getRequiredConfig();
  }

  @Override
  public int getPort() {
    return delegate.getPort();
  }

  @Override
  @Nullable
  public InetAddress getAddress() {
    return delegate.getAddress();
  }

  @Override
  public boolean isDevelopment() {
    return delegate.isDevelopment();
  }

  @Override
  public int getThreads() {
    return delegate.getThreads();
  }

  @Override
  public URI getPublicAddress() {
    return delegate.getPublicAddress();
  }

  @Override
  @Nullable
  public SslContext getSslContext() {
    return delegate.getSslContext();
  }

  @Override
  public boolean isRequireClientSslAuth() {
    return delegate.isRequireClientSslAuth();
  }

  @Override
  public int getMaxContentLength() {
    return delegate.getMaxContentLength();
  }

  @Override
  public boolean isHasBaseDir() {
    return delegate.isHasBaseDir();
  }

  @Override
  public FileSystemBinding getBaseDir() throws NoBaseDirException {
    return delegate.getBaseDir();
  }

  @Override
  public Optional<Integer> getConnectTimeoutMillis() {
    return delegate.getConnectTimeoutMillis();
  }

  @Override
  public Optional<Integer> getMaxMessagesPerRead() {
    return delegate.getMaxMessagesPerRead();
  }

  @Override
  public Optional<Integer> getReceiveBufferSize() {
    return delegate.getReceiveBufferSize();
  }

  @Override
  public Optional<Integer> getWriteSpinCount() {
    return delegate.getWriteSpinCount();
  }
}
