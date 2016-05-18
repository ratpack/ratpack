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

import io.netty.handler.codec.http.HttpMethod;
import ratpack.file.FileSystemBinding;
import ratpack.server.ServerConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Set;

public class ServerConfigData {

  private final FileSystemBinding baseDir;
  private int port;
  private InetAddress address;
  private boolean development;
  private int threads = ServerConfig.DEFAULT_THREADS;
  private URI publicAddress;
  private SSLContext sslContext;
  private boolean requireClientSslAuth;
  private int maxContentLength = ServerConfig.DEFAULT_MAX_CONTENT_LENGTH;
  private Optional<Integer> connectTimeoutMillis = Optional.empty();
  private Optional<Integer> maxMessagesPerRead = Optional.empty();
  private Optional<Integer> receiveBufferSize = Optional.empty();
  private Optional<Integer> writeSpinCount = Optional.empty();
  private int maxChunkSize = ServerConfig.DEFAULT_MAX_CHUNK_SIZE;
  private Set<HttpMethod> methodsCanHaveBody = ServerConfig.DEFAULT_METHODS_CAN_HAVE_BODY;

  public ServerConfigData(FileSystemBinding baseDir, int port, boolean development, URI publicAddress) {
    this.baseDir = baseDir;
    this.port = port;
    this.development = development;
    this.publicAddress = publicAddress;
  }

  public int getPort() {
    return port;
  }

  public InetAddress getAddress() {
    return address;
  }

  public boolean isDevelopment() {
    return development;
  }

  public int getThreads() {
    return threads;
  }

  public URI getPublicAddress() {
    return publicAddress;
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }

  public Optional<Integer> getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public Optional<Integer> getMaxMessagesPerRead() {
    return maxMessagesPerRead;
  }

  public Optional<Integer> getReceiveBufferSize() {
    return receiveBufferSize;
  }

  public Optional<Integer> getWriteSpinCount() {
    return writeSpinCount;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setAddress(InetAddress address) {
    this.address = address;
  }

  public void setAddress(String host) throws UnknownHostException {
    setAddress(InetAddress.getByName(host));
  }

  public void setDevelopment(boolean development) {
    this.development = development;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  public void setPublicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
  }

  public void setPublicAddress(String publicAddress) throws URISyntaxException {
    this.publicAddress = new URI(publicAddress);
  }

  public SSLContext getSslContext() {
    return sslContext;
  }

  public void setSslContext(SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  public boolean isRequireClientSslAuth() {
    return requireClientSslAuth;
  }

  public void setRequireClientSslAuth(boolean requireClientSslAuth) {
    this.requireClientSslAuth = requireClientSslAuth;
  }

  public void setMaxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
  }

  public void setConnectTimeoutMillis(int connectTimeoutMillis) {
    this.connectTimeoutMillis = Optional.of(connectTimeoutMillis);
  }

  public void setMaxMessagesPerRead(int maxMessagesPerRead) {
    this.maxMessagesPerRead = Optional.of(maxMessagesPerRead);
  }

  public void setReceiveBufferSize(int receiveBufferSize) {
    this.receiveBufferSize = Optional.of(receiveBufferSize);
  }

  public void setWriteSpinCount(int writeSpinCount) {
    this.writeSpinCount = Optional.of(writeSpinCount);
  }

  public int getMaxChunkSize() {
    return maxChunkSize;
  }

  public void setMaxChunkSize(int maxChunkSize) {
    this.maxChunkSize = maxChunkSize;
  }

  public FileSystemBinding getBaseDir() {
    return baseDir;
  }

  public Set<HttpMethod> getMethodsCanHaveBody() {
    return methodsCanHaveBody;
  }

  public void setMethodsCanHaveBody(Set<HttpMethod> methodsCanHaveBody) {
    this.methodsCanHaveBody = methodsCanHaveBody;
  }
}
