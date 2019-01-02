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
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.util.Exceptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ThreadFactory;

public abstract class TransportDetector {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransportDetector.class);

  private static final Transport TRANSPORT = determineTransport();

  private static Transport determineTransport() {
    Transport transport = new NativeTransport("io.netty.channel.epoll", "Epoll");
    if (transport.isAvailable()) {
      LOGGER.debug("Using epoll transport");
      return transport;
    }

    transport = new NativeTransport("io.netty.channel.kqueue", "KQueue");
    if (transport.isAvailable()) {
      LOGGER.debug("Using kqueue transport");
      return transport;
    }

    LOGGER.debug("Using nio transport");
    return new NioTransport();
  }

  public static Class<? extends ServerSocketChannel> getServerSocketChannelImpl() {
    return TRANSPORT.getServerSocketChannelImpl();
  }

  public static Class<? extends SocketChannel> getSocketChannelImpl() {
    return TRANSPORT.getSocketChannelImpl();
  }

  public static EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
    return TRANSPORT.eventLoopGroup(nThreads, threadFactory);
  }

  private interface Transport {

    boolean isAvailable();

    Class<? extends ServerSocketChannel> getServerSocketChannelImpl();

    Class<? extends SocketChannel> getSocketChannelImpl();

    EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory);
  }

  private static class NioTransport implements Transport {

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public Class<? extends ServerSocketChannel> getServerSocketChannelImpl() {
      return NioServerSocketChannel.class;
    }

    @Override
    public Class<? extends SocketChannel> getSocketChannelImpl() {
      return NioSocketChannel.class;
    }

    @Override
    public EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
      return new NioEventLoopGroup(nThreads, threadFactory);
    }
  }

  private static class NativeTransport implements Transport {

    private final NativeTransportImpl impl;

    NativeTransport(String packageName, String classPrefix) {
      String property = "ratpack." + classPrefix.toLowerCase() + ".disable";
      boolean disabled = Boolean.getBoolean(property);
      if (disabled || !isAvailable(packageName, classPrefix)) {
        this.impl = null;
      } else {
        this.impl = loadImpl(packageName, classPrefix);
      }
    }

    @Override
    public boolean isAvailable() {
      return impl != null;
    }

    @Override
    public Class<? extends ServerSocketChannel> getServerSocketChannelImpl() {
      return impl.serverSocketChannelClass;
    }

    @Override
    public Class<? extends SocketChannel> getSocketChannelImpl() {
      return impl.socketChannelClass;
    }

    @Override
    public EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
      return impl.eventLoopGroup(nThreads, threadFactory);
    }

    private static NativeTransportImpl loadImpl(String packageName, String classPrefix) {
      try {
        Class<? extends ServerSocketChannel> serverSocketChannelClass = loadClass(ServerSocketChannel.class, packageName, classPrefix, ServerSocketChannel.class.getSimpleName());
        Class<? extends SocketChannel> socketChannelClass = loadClass(SocketChannel.class, packageName, classPrefix, SocketChannel.class.getSimpleName());
        Class<? extends EventLoopGroup> eventLoopGroupClass = loadClass(EventLoopGroup.class, packageName, classPrefix, EventLoopGroup.class.getSimpleName());
        Constructor<? extends EventLoopGroup> constructor = eventLoopGroupClass.getConstructor(int.class, ThreadFactory.class);
        return new NativeTransportImpl(serverSocketChannelClass, socketChannelClass, constructor);
      } catch (ReflectiveOperationException e) {
        LOGGER.debug("Failed to load {}", classPrefix, e);
        return null;
      }
    }

    private static boolean isAvailable(String packageName, String classPrefix) {
      try {
        Class<?> clazz = loadClass(Object.class, packageName, classPrefix, null);
        return invokeIsAvailable(clazz);
      } catch (ClassNotFoundException e) {
        LOGGER.debug("{} was not found", packageName);
        return false;
      } catch (Exception e) {
        LOGGER.debug("{} failed to load", packageName, e);
        return false;
      }
    }

    private static <T> Class<? extends T> loadClass(Class<T> type, String packageName, String classPrefix, String classType) throws ClassNotFoundException {
      String name = packageName + "." + classPrefix;
      if (classType != null) {
        name += classType;
      }

      Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
      if (type.isAssignableFrom(clazz)) {
        @SuppressWarnings("unchecked") Class<? extends T> cast = (Class<? extends T>) clazz;
        return cast;
      } else {
        throw new ClassCastException("Cannot assign " + clazz + " to " + type);
      }
    }

    private static boolean invokeIsAvailable(Class<?> clazz) {
      Object result;
      try {
        result = clazz.getMethod("isAvailable").invoke(null);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LOGGER.debug("{}.isAvailable failed", clazz.getName(), e);
        return false;
      }

      if (result instanceof Boolean) {
        Boolean isAvailable = (Boolean) result;
        if (!isAvailable && LOGGER.isDebugEnabled()) {
          Object unavailabilityCause;
          try {
            unavailabilityCause = clazz.getMethod("unavailabilityCause").invoke(null);
          } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.debug("{}.unavailabilityCause() failed", clazz.getName(), e);
            return false;
          }

          if (unavailabilityCause instanceof Throwable) {
            //noinspection VerifyFormattedMessage
            LOGGER.debug("{} unavailability cause", clazz.getName(), unavailabilityCause);
          }
        }
        return isAvailable;
      } else {
        LOGGER.debug("{}.isAvailable returned {}", clazz.getName(), result);
        return false;
      }
    }
  }

  private static class NativeTransportImpl {
    private final Class<? extends ServerSocketChannel> serverSocketChannelClass;
    private final Class<? extends SocketChannel> socketChannelClass;
    private final Constructor<? extends EventLoopGroup> constructor;

    NativeTransportImpl(
      Class<? extends ServerSocketChannel> serverSocketChannelClass,
      Class<? extends SocketChannel> socketChannelClass,
      Constructor<? extends EventLoopGroup> constructor
    ) {
      this.serverSocketChannelClass = serverSocketChannelClass;
      this.socketChannelClass = socketChannelClass;
      this.constructor = constructor;
    }

    EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
      try {
        return constructor.newInstance(nThreads, threadFactory);
      } catch (ReflectiveOperationException e) {
        throw Exceptions.uncheck(e);
      }
    }


  }

}
