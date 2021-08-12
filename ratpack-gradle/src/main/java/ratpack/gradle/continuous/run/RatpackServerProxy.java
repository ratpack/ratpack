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

package ratpack.gradle.continuous.run;

import ratpack.gradle.internal.Invoker;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class RatpackServerProxy {

  public static final String CAPTURER_CLASS_NAME = "ratpack.server.internal.ServerCapturer";
  public static final String BLOCK_CLASS_NAME = "ratpack.func.Block";
  public static final String SERVER_INTERFACE_NAME = "ratpack.server.RatpackServer";
  private final Object server;
  private final ClassLoader classLoader;

  public RatpackServerProxy(Object server, ClassLoader classLoader) {
    this.server = server;
    this.classLoader = classLoader;
  }

  public void stop() {
    try {
      Invoker.invokeParamless(classLoader.loadClass(SERVER_INTERFACE_NAME), server, "stop");
    } catch (Exception e) {
      throw new RuntimeException("Failed to stop server", e);
    }
  }

  public static RatpackServerProxy capture(ClassLoader classLoader, final String mainClassName, final String[] appArgs) {

    Class<?> capturer = loadClass(classLoader, CAPTURER_CLASS_NAME);
    Class<?> serverType = loadClass(classLoader, SERVER_INTERFACE_NAME);

    AtomicReference<Object> serverOrErrorRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Method captureWithMethod;
    try {
      captureWithMethod = capturer.getMethod("captureWith", Consumer.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Could not find capture() on ServerCapturer", e);
    }

    Consumer<Object> consumer = server -> {
      if (serverType.isAssignableFrom(server.getClass())) {
        serverOrErrorRef.compareAndSet(null, server);
      } else {
        serverOrErrorRef.compareAndSet(null, new RuntimeException("Captured " + server.getClass().getName() + ", not a " + SERVER_INTERFACE_NAME));
      }

      latch.countDown();
    };

    new Thread(() -> {

      try {
        Object capturerReleaser;
        try {
          capturerReleaser = captureWithMethod.invoke(null, consumer);
        } catch (Exception e) {
          throw new RuntimeException("Failed to invoke get() on ServerCapturer", e);
        }

        try {
          findMainMethod(classLoader, mainClassName).invoke(null, new Object[]{appArgs});
        } catch (Exception e) {
          throw new RuntimeException("failed to invoke main(String...) on class: " + mainClassName, e);
        } finally {
          if (capturerReleaser instanceof Closeable) {
            ((Closeable) capturerReleaser).close();
          }
        }
      } catch (Throwable t) {
        serverOrErrorRef.compareAndSet(null, t);
        latch.countDown();
      }
    }, "ratpack-starter").start();

    int waitSecs = 30;
    try {
      if (latch.await(30, TimeUnit.SECONDS)) {
        return toServerProxy(classLoader, serverOrErrorRef.get());
      } else {
        throw new IllegalStateException("Start did not create server or fail within " + waitSecs + " seconds");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private static RatpackServerProxy toServerProxy(ClassLoader classLoader, Object serverOrError) {
    if (serverOrError instanceof Throwable) {
      if (serverOrError instanceof Error) {
        throw (Error) serverOrError;
      } else if (serverOrError instanceof RuntimeException) {
        throw (RuntimeException) serverOrError;
      } else {
        throw new RuntimeException((Throwable) serverOrError);
      }
    } else {
      return new RatpackServerProxy(serverOrError, classLoader);
    }
  }

  private static Method findMainMethod(ClassLoader classLoader, String mainClassName) {
    Class<?> mainClass;
    try {
      mainClass = classLoader.loadClass(mainClassName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Did not find specified main class: " + mainClassName, e);
    }

    Method main;
    try {
      main = mainClass.getMethod("main", String[].class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Did not find main(String...) method on main class: " + mainClassName, e);
    }

    if (!Modifier.isStatic(main.getModifiers())) {
      throw new RuntimeException("main(String...) is not static on class: " + mainClassName);
    }
    return main;
  }

  private static Class<?> loadClass(ClassLoader classLoader, String className) {
    try {
      return classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not load " + className, e);
    }
  }

  public boolean isRunning() {
    try {
      return (Boolean) server.getClass().getMethod("isRunning").invoke(server);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke isRunning on server", e);
    }
  }

}
