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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ratpack.api.Nullable;
import ratpack.file.FileSystemBinding;
import ratpack.server.NoBaseDirException;
import ratpack.server.ServerConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;

public class DefaultServerConfig implements ServerConfig {

  private final FileSystemBinding baseDir;
  private final int port;
  private final InetAddress address;
  private final boolean development;
  private final int threads;
  private final URI publicAddress;
  private final ImmutableList<String> indexFiles;
  private final SSLContext sslContext;
  private final int maxContentLength;
  private final boolean timeResponses;
  private final boolean compressResponses;
  private final long compressionMinSize;
  private final ImmutableSet<String> compressionMimeTypeWhiteList;
  private final ImmutableSet<String> compressionMimeTypeBlackList;

  public DefaultServerConfig(
    FileSystemBinding baseDir,
    int port,
    InetAddress address,
    boolean development,
    int threads,
    URI publicAddress,
    ImmutableList<String> indexFiles,
    SSLContext sslContext,
    int maxContentLength,
    boolean timeResponses,
    boolean compressResponses,
    long compressionMinSize,
    ImmutableSet<String> compressionMimeTypeWhiteList,
    ImmutableSet<String> compressionMimeTypeBlackList
  ) {
    this.baseDir = baseDir;
    this.port = port;
    this.address = address;
    this.development = development;
    this.threads = threads;
    this.timeResponses = timeResponses;
    this.compressResponses = compressResponses;
    this.compressionMinSize = compressionMinSize;
    this.compressionMimeTypeWhiteList = compressionMimeTypeWhiteList;
    this.compressionMimeTypeBlackList = compressionMimeTypeBlackList;
    this.publicAddress = publicAddress;
    this.indexFiles = indexFiles;
    this.sslContext = sslContext;
    this.maxContentLength = maxContentLength;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Nullable
  @Override
  public InetAddress getAddress() {
    return address;
  }

  @Override
  public boolean isDevelopment() {
    return development;
  }

  @Override
  public int getThreads() {
    return threads;
  }

  @Override
  public URI getPublicAddress() {
    return publicAddress;
  }

  @Nullable
  @Override
  public SSLContext getSSLContext() {
    return sslContext;
  }

  @Override
  public int getMaxContentLength() {
    return maxContentLength;
  }

  @Override
  public boolean isTimeResponses() {
    return timeResponses;
  }

  @Override
  public boolean isCompressResponses() {
    return compressResponses;
  }

  @Override
  public long getCompressionMinSize() {
    return compressionMinSize;
  }

  @Override
  public ImmutableSet<String> getCompressionMimeTypeWhiteList() {
    return compressionMimeTypeWhiteList;
  }

  @Override
  public ImmutableSet<String> getCompressionMimeTypeBlackList() {
    return compressionMimeTypeBlackList;
  }

  @Override
  public boolean isHasBaseDir() {
    return baseDir != null;
  }

  @Override
  public FileSystemBinding getBaseDir() throws NoBaseDirException {
    if (baseDir == null) {
      throw new NoBaseDirException("No base dir has been set");
    } else {
      return baseDir;
    }
  }

}
