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
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.util.Exceptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

public abstract class TransportDetector {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransportDetector.class);

  private static final Transport TRANSPORT = determineTransport();

  private static Transport determineTransport() {
    String transportName = System.getProperty("ratpack.nativeTransport");
    if (transportName != null) {
      NativeTransport nativeTransport = nativeTransportForName(transportName);
      if (!nativeTransport.isPresent()) {
        throw new IllegalStateException("Classes for native transport '" + nativeTransport + "' are not present");
      }

      if (nativeTransport.isAvailable()) {
        LOGGER.debug("Using explicitly requested transport {}", nativeTransport);
        return nativeTransport;
      } else {
        Throwable unavailabilityCause = nativeTransport.unavailabilityCause();
        if (unavailabilityCause == null) {
          throw new IllegalStateException("Requested native transport '" + nativeTransport + "' is unavailable - with no cause");
        } else {
          throw new IllegalStateException("Requested native transport '" + nativeTransport + "' is unavailable", unavailabilityCause);
        }
      }
    }

    for (NativeTransport nativeTransport : NativeTransport.values()) {
      if (nativeTransport.stable && nativeTransport.isAvailable()) {
        LOGGER.debug("Using " + nativeTransport + " transport");
        return nativeTransport;
      }
    }

    LOGGER.debug("Using nio transport");
    return NioTransport.INSTANCE;
  }

  private static NativeTransport nativeTransportForName(String transportName) {
    transportName = transportName.toUpperCase(Locale.ROOT);
    try {
      return NativeTransport.valueOf(transportName);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Requested unknown native transport '" + transportName + "' (known: " + Arrays.toString(NativeTransport.values()) + ")");
    }
  }

  public static Class<? extends ServerSocketChannel> getServerSocketChannelImpl() {
    return transport().getServerSocketChannelImpl();
  }

  private static Transport transport() {
    if (Boolean.getBoolean("ratpack.nativeTransport.disable")) {
      return NioTransport.INSTANCE;
    } else {
      return TRANSPORT;
    }
  }

  public static Class<? extends SocketChannel> getSocketChannelImpl() {
    return transport().getSocketChannelImpl();
  }

  public static Class<? extends DatagramChannel> getDatagramChannelImpl() {
    return transport().getDatagramChannelImpl();
  }

  public static EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
    return transport().eventLoopGroup(nThreads, threadFactory);
  }

  private interface Transport {


    Class<? extends ServerSocketChannel> getServerSocketChannelImpl();

    Class<? extends SocketChannel> getSocketChannelImpl();

    Class<? extends DatagramChannel> getDatagramChannelImpl();

    EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory);
  }

  private static class NioTransport implements Transport {

    private static final NioTransport INSTANCE = new NioTransport();

    private NioTransport() {
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
    public Class<? extends DatagramChannel> getDatagramChannelImpl() {
      return NioDatagramChannel.class;
    }

    @Override
    public EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
      return new NioEventLoopGroup(nThreads, threadFactory);
    }
  }

  private enum NativeTransport implements Transport {

    IO_URING("io.netty.incubator.channel.uring", "IOUring", false),
    EPOLL("io.netty.channel.epoll", "Epoll", true),
    KQUEUE("io.netty.channel.kqueue", "KQueue", true);

    @Nullable
    private final Class<?> entryPoint;

    @Nullable
    private final NativeTransportImpl impl;
    private final boolean stable;

    NativeTransport(String packageName, String classPrefix, boolean stable) {
      this.stable = stable;
      String property = "ratpack." + name().toLowerCase() + ".disable";
      boolean disabled = Boolean.getBoolean(property);
      if (disabled) {
        this.impl = null;
        this.entryPoint = null;
      } else {
        this.entryPoint = loadEntryPoint(packageName, classPrefix);
        if (entryPoint == null || !isAvailable(entryPoint)) {
          this.impl = null;
        } else {
          this.impl = loadImpl(packageName, classPrefix);
        }
      }
    }

    public boolean isPresent() {
      return entryPoint != null;
    }

    public boolean isAvailable() {
      return impl != null;
    }

    @Nullable
    private Throwable unavailabilityCause() {
      if (entryPoint == null) {
        return null;
      }
      Object unavailabilityCause;
      try {
        unavailabilityCause = entryPoint.getMethod("unavailabilityCause").invoke(null);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LOGGER.debug("{}.unavailabilityCause() failed", entryPoint.getName(), e);
        return null;
      }

      if (unavailabilityCause instanceof Throwable) {
        return (Throwable) unavailabilityCause;
      } else {
        LOGGER.debug("{}.unavailabilityCause() returned non throwable: {}", entryPoint.getName(), unavailabilityCause);
        return null;
      }
    }

    @Override
    public Class<? extends ServerSocketChannel> getServerSocketChannelImpl() {
      return requireNonNull(impl).serverSocketChannelClass;
    }

    @Override
    public Class<? extends SocketChannel> getSocketChannelImpl() {
      return requireNonNull(impl).socketChannelClass;
    }

    @Override
    public Class<? extends DatagramChannel> getDatagramChannelImpl() {
      return requireNonNull(impl).datagramChannelClass;
    }

    @Override
    public EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
      return requireNonNull(impl).eventLoopGroup(nThreads, threadFactory);
    }

    private static NativeTransportImpl loadImpl(String packageName, String classPrefix) {
      try {
        Class<? extends ServerSocketChannel> serverSocketChannelClass = loadClass(ServerSocketChannel.class, packageName, classPrefix, ServerSocketChannel.class.getSimpleName());
        Class<? extends SocketChannel> socketChannelClass = loadClass(SocketChannel.class, packageName, classPrefix, SocketChannel.class.getSimpleName());
        Class<? extends DatagramChannel> datagramChannelClass = loadClass(DatagramChannel.class, packageName, classPrefix, DatagramChannel.class.getSimpleName());
        Class<? extends EventLoopGroup> eventLoopGroupClass = loadClass(EventLoopGroup.class, packageName, classPrefix, EventLoopGroup.class.getSimpleName());
        Constructor<? extends EventLoopGroup> constructor = eventLoopGroupClass.getConstructor(int.class, ThreadFactory.class);
        return new NativeTransportImpl(serverSocketChannelClass, socketChannelClass, datagramChannelClass, constructor);
      } catch (ReflectiveOperationException e) {
        LOGGER.debug("Failed to load {}", classPrefix, e);
        return null;
      }
    }

    @Nullable
    private static Class<?> loadEntryPoint(String packageName, String classPrefix) {
      try {
        return loadClass(Object.class, packageName, classPrefix, null);
      } catch (ClassNotFoundException e) {
        LOGGER.debug("{} was not found", packageName);
        return null;
      } catch (Exception e) {
        LOGGER.debug("{} failed to load", packageName, e);
        return null;
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

    private static boolean isAvailable(Class<?> clazz) {
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
            //noinspection RedundantCast
            LOGGER.debug("{} unavailability cause", clazz.getName(), (Throwable) unavailabilityCause);
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
    private final Class<? extends DatagramChannel> datagramChannelClass;
    private final Constructor<? extends EventLoopGroup> constructor;

    NativeTransportImpl(
      Class<? extends ServerSocketChannel> serverSocketChannelClass,
      Class<? extends SocketChannel> socketChannelClass,
      Class<? extends DatagramChannel> datagramChannelClass,
      Constructor<? extends EventLoopGroup> constructor
    ) {
      this.serverSocketChannelClass = serverSocketChannelClass;
      this.socketChannelClass = socketChannelClass;
      this.datagramChannelClass = datagramChannelClass;
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
