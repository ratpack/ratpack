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

package ratpack.launch.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import ratpack.handling.internal.DefaultBackground;
import ratpack.file.FileSystemBinding;
import ratpack.handling.Background;
import ratpack.handling.Foreground;
import ratpack.handling.internal.ContextStorage;
import ratpack.handling.internal.DefaultForeground;
import ratpack.handling.internal.ThreadLocalContextStorage;
import ratpack.launch.HandlerFactory;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class DefaultLaunchConfig implements LaunchConfigInternal {

  private final FileSystemBinding baseDir;
  private final HandlerFactory handlerFactory;
  private final int port;
  private final InetAddress address;
  private final boolean reloadable;
  private final int threads;
  private final ListeningExecutorService backgroundExecutorService;
  private final Background background;
  private final Foreground foreground;
  private final ByteBufAllocator byteBufAllocator;
  private final URI publicAddress;
  private final ImmutableList<String> indexFiles;
  private final ImmutableMap<String, String> other;
  private final SSLContext sslContext;
  private final int maxContentLength;
  private final boolean timeResponses;
  private final boolean compressResponses;

  private final ContextStorage contextStorage = new ThreadLocalContextStorage();
  private final EventLoopGroup eventLoopGroup;

  public DefaultLaunchConfig(FileSystemBinding baseDir, int port, InetAddress address, boolean reloadable, int threads, ExecutorService backgroundExecutorService, ByteBufAllocator byteBufAllocator, URI publicAddress, ImmutableList<String> indexFiles, ImmutableMap<String, String> other, SSLContext sslContext, int maxContentLength, boolean timeResponses, boolean compressResponses, HandlerFactory handlerFactory) {
    this.baseDir = baseDir;
    this.port = port;
    this.address = address;
    this.reloadable = reloadable;
    this.threads = threads;
    this.timeResponses = timeResponses;
    this.compressResponses = compressResponses;
    this.backgroundExecutorService = MoreExecutors.listeningDecorator(backgroundExecutorService);
    this.byteBufAllocator = byteBufAllocator;
    this.publicAddress = publicAddress;
    this.indexFiles = indexFiles;
    this.other = other;
    this.handlerFactory = handlerFactory;
    this.sslContext = sslContext;
    this.maxContentLength = maxContentLength;
    this.eventLoopGroup = new NioEventLoopGroup(threads, new DefaultThreadFactory("ratpack-group"));
    this.background = new DefaultBackground(eventLoopGroup, this.backgroundExecutorService, contextStorage);
    this.foreground = new DefaultForeground(contextStorage, MoreExecutors.listeningDecorator(eventLoopGroup));
  }

  @Override
  public FileSystemBinding getBaseDir() {
    return baseDir;
  }

  @Override
  public HandlerFactory getHandlerFactory() {
    return handlerFactory;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public InetAddress getAddress() {
    return address;
  }

  @Override
  public boolean isReloadable() {
    return reloadable;
  }

  @Override
  public int getThreads() {
    return threads;
  }

  @Override
  public Background getBackground() {
    return background;
  }

  @Override
  public Foreground getForeground() {
    return foreground;
  }

  @Override
  public ByteBufAllocator getBufferAllocator() {
    return byteBufAllocator;
  }

  @Override
  public URI getPublicAddress() {
    return publicAddress;
  }

  @Override
  public List<String> getIndexFiles() {
    return indexFiles;
  }

  @Override
  public SSLContext getSSLContext() {
    return sslContext;
  }

  public String getOther(String key, String defaultValue) {
    String value = other.get(key);
    return value == null ? defaultValue : value;
  }

  @Override
  public Map<String, String> getOtherPrefixedWith(String prefix) {
    Map<String, String> result = new LinkedHashMap<>();
    int prefixLength = prefix.length();
    for (Map.Entry<String, String> property : other.entrySet()) {
      String key = property.getKey();
      if (key.startsWith(prefix) && key.length() > prefixLength) {
        result.put(key.substring(prefixLength), property.getValue());
      }
    }
    return result;
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
  public ContextStorage getContextStorage() {
    return contextStorage;
  }

  @Override
  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  @Override
  public ExecutorService getBackgroundExecutorService() {
    return backgroundExecutorService;
  }

}
