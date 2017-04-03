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

package ratpack.util.internal;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ThreadFactory;

public abstract class ChannelImplDetector {

  private static final boolean EPOLL = !Boolean.getBoolean("ratpack.epoll.disable") && Epoll.isAvailable();

  public static Class<? extends ServerSocketChannel> getServerSocketChannelImpl() {
    return EPOLL ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
  }

  public static Class<? extends SocketChannel> getSocketChannelImpl() {
    return EPOLL ? EpollSocketChannel.class : NioSocketChannel.class;
  }

  public static EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
    return EPOLL ? new EpollEventLoopGroup(nThreads, threadFactory) : new NioEventLoopGroup(nThreads, threadFactory);
  }

}
