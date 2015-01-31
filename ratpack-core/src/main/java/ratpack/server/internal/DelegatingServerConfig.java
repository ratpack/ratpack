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
import ratpack.file.FileSystemBinding;
import ratpack.server.NoBaseDirException;
import ratpack.server.ServerConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;

public class DelegatingServerConfig implements ServerConfig {

  private final ServerConfig delegate;

  public DelegatingServerConfig(ServerConfig delegate) {
    this.delegate = delegate;
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
  public List<String> getIndexFiles() {
    return delegate.getIndexFiles();
  }

  @Override
  @Nullable
  public SSLContext getSSLContext() {
    return delegate.getSSLContext();
  }

  @Override
  public int getMaxContentLength() {
    return delegate.getMaxContentLength();
  }

  @Override
  public boolean isTimeResponses() {
    return delegate.isTimeResponses();
  }

  @Override
  public boolean isCompressResponses() {
    return delegate.isCompressResponses();
  }

  @Override
  public long getCompressionMinSize() {
    return delegate.getCompressionMinSize();
  }

  @Override
  public ImmutableSet<String> getCompressionMimeTypeWhiteList() {
    return delegate.getCompressionMimeTypeWhiteList();
  }

  @Override
  public ImmutableSet<String> getCompressionMimeTypeBlackList() {
    return delegate.getCompressionMimeTypeBlackList();
  }

  @Override
  public boolean isHasBaseDir() {
    return delegate.isHasBaseDir();
  }

  @Override
  public FileSystemBinding getBaseDir() throws NoBaseDirException {
    return delegate.getBaseDir();
  }
}
