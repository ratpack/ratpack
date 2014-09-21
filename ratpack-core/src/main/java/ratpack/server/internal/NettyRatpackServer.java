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

package ratpack.server.internal;

import com.google.common.base.Throwables;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.file.BaseDirRequiredException;
import ratpack.func.Function;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchException;
import ratpack.server.RatpackServer;
import ratpack.server.Stopper;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static ratpack.util.ExceptionUtils.uncheck;

public class NettyRatpackServer implements RatpackServer {

  private final Logger logger = LoggerFactory.getLogger(getClass().getName());

  private final LaunchConfig launchConfig;
  private final Function<Stopper, ChannelInitializer<SocketChannel>> channelInitializerTransformer;

  private InetSocketAddress boundAddress;
  private Channel channel;

  private final Lock lifecycleLock = new ReentrantLock();
  private final AtomicBoolean running = new AtomicBoolean();

  public NettyRatpackServer(LaunchConfig launchConfig, Function<Stopper, ChannelInitializer<SocketChannel>> channelInitializerTransformer) {
    this.launchConfig = launchConfig;
    this.channelInitializerTransformer = channelInitializerTransformer;
  }

  @Override
  public LaunchConfig getLaunchConfig() {
    return launchConfig;
  }

  @Override
  public void start() throws LaunchException {
    lifecycleLock.lock();
    try {
      if (isRunning()) {
        return;
      }
      Stopper stopper = () -> {
        try {
          NettyRatpackServer.this.stop();
        } catch (Exception e) {
          throw uncheck(e);
        }
      };

      ServerBootstrap bootstrap = new ServerBootstrap();

      ChannelInitializer<SocketChannel> channelInitializer = channelInitializerTransformer.apply(stopper);

      bootstrap
        .group(launchConfig.getExecController().getEventLoopGroup())
        .childHandler(channelInitializer)
        .channel(NioServerSocketChannel.class)
        .childOption(ChannelOption.ALLOCATOR, launchConfig.getBufferAllocator());

      if (System.getProperty("io.netty.leakDetectionLevel", null) == null) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
      }

      channel = bootstrap.bind(buildSocketAddress()).sync().channel();

      boundAddress = (InetSocketAddress) channel.localAddress();

      if (logger.isInfoEnabled()) {
        logger.info(String.format("Ratpack started for http://%s:%s", getBindHost(), getBindPort()));
      }
      running.set(true);
    } catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, BaseDirRequiredException.class);
      Throwables.propagateIfInstanceOf(e, LaunchException.class);
      throw new LaunchException("Unable to launch due to exception", e);
    } finally {
      lifecycleLock.unlock();
    }
  }

  @Override
  public void stop() throws Exception {
    lifecycleLock.lock();
    try {
      if (!isRunning()) {
        return;
      }
      channel.close();
      partialShutdown();
      running.set(false);
    } finally {
      lifecycleLock.unlock();
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  private void partialShutdown() throws Exception {
    launchConfig.getExecController().close();
  }

  @Override
  public String getScheme() {
    return launchConfig.getSSLContext() == null ? "http" : "https";
  }

  public int getBindPort() {
    return boundAddress == null ? -1 : boundAddress.getPort();
  }

  public String getBindHost() {
    if (boundAddress == null) {
      return null;
    } else {
      return InetSocketAddressBackedBindAddress.determineHost(boundAddress);
    }
  }

  private InetSocketAddress buildSocketAddress() {
    return (launchConfig.getAddress() == null) ? new InetSocketAddress(launchConfig.getPort()) : new InetSocketAddress(launchConfig.getAddress(), launchConfig.getPort());
  }

}
