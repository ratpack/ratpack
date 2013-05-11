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

package org.ratpackframework.bootstrap;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.ratpackframework.Action;
import org.ratpackframework.bootstrap.internal.NettyRatpackServer;
import org.ratpackframework.bootstrap.internal.NoopInit;
import org.ratpackframework.bootstrap.internal.RatpackChannelInitializer;
import org.ratpackframework.routing.Handler;

import java.net.InetSocketAddress;

public class RatpackServerBuilder {

  public static final int DEFAULT_PORT = 5050;
  private final Handler handler;

  private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

  private int port = DEFAULT_PORT;
  private String host;
  private Action<RatpackServer> init = new NoopInit();

  public RatpackServerBuilder(Handler handler) {
    this.handler = handler;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Action<RatpackServer> getInit() {
    return init;
  }

  public void setInit(Action<RatpackServer> init) {
    this.init = init;
  }

  public int getWorkerThreads() {
    return workerThreads;
  }

  public void setWorkerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
  }

  public RatpackServer build() {
    InetSocketAddress address = buildSocketAddress();
    ChannelInitializer<SocketChannel> channelInitializer = buildChannelInitializer();
    return new NettyRatpackServer(
        address, channelInitializer, init
    );
  }

  protected InetSocketAddress buildSocketAddress() {
    return (host == null) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
  }

  private ChannelInitializer<SocketChannel> buildChannelInitializer() {
    return new RatpackChannelInitializer(workerThreads, handler);
  }

}
