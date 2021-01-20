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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

public class RatpackServerProxy {

  public static final String CAPTURER_CLASS_NAME = "ratpack.core.server.internal.ServerCapturer";
  public static final String BLOCK_CLASS_NAME = "ratpack.func.Block";
  public static final String SERVER_INTERFACE_NAME = "ratpack.core.server.RatpackServer";
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

    Class<?> capturer = loadClass(classLoader, CAPTURER_CLASS_NAME);
    Class<?> blockType = loadClass(classLoader, BLOCK_CLASS_NAME);
    Class<?> serverType = loadClass(classLoader, SERVER_INTERFACE_NAME);

    Method captureMethod;
    try {
      captureMethod = capturer.getMethod("capture", blockType);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Could not find capture() on ServerCapturer", e);
    }

    final Method finalMain = main;
    Object block = Proxy.newProxyInstance(classLoader, new Class<?>[]{blockType}, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
          finalMain.invoke(null, new Object[]{appArgs});
        } catch (Exception e) {
          throw new RuntimeException("failed to invoke main(String...) on class: " + mainClassName, e);
        }
        return null;
      }
    });

    Object server;
    try {
      server = captureMethod.invoke(null, block);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke get() on ServerCapturer", e);
    }

    if (server == null) {
      throw new RuntimeException("main(String...) of " + mainClassName + " did not start a Ratpack server");
    }

    if (!serverType.isAssignableFrom(server.getClass())) {
      throw new RuntimeException("Captured " + server.getClass().getName() + ", not a " + SERVER_INTERFACE_NAME);
    }

    return new RatpackServerProxy(server, classLoader);
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
