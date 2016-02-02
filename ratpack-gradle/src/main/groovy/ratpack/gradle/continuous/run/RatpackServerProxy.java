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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

public class RatpackServerProxy {

  public static final String CAPTURER_CLASS_NAME = "ratpack.server.internal.ServerCapturer";
  public static final String BLOCK_CLASS_NAME = "ratpack.func.Block";
  public static final String SERVER_CLASS_NAME = "ratpack.server.internal.DefaultRatpackServer";
  private final Object server;

  public RatpackServerProxy(Object server) {
    this.server = server;
  }

  public void stop() {
    try {
      server.getClass().getMethod("stop").invoke(server);
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

    Class<?> capturer;
    try {
      capturer = classLoader.loadClass(CAPTURER_CLASS_NAME);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not load " + CAPTURER_CLASS_NAME, e);
    }
    Class<?> blockType;
    try {
      blockType = classLoader.loadClass(BLOCK_CLASS_NAME);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not load " + BLOCK_CLASS_NAME, e);
    }

    Method captureMethod;
    try {
      captureMethod = capturer.getMethod("capture", blockType);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Could not find capture() on ServerCapturer");
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
      throw new RuntimeException("Failed to invoke get() on ServerCapturer");
    }

    if (server == null) {
      throw new RuntimeException("main(String...) of " + mainClassName + " did not start a Ratpack server");
    }

    if (!server.getClass().getName().equals(SERVER_CLASS_NAME)) {
      throw new RuntimeException("Captured " + server.getClass().getName() + ", not " + SERVER_CLASS_NAME);
    }

    return new RatpackServerProxy(server);
  }

  public boolean isRunning() {
    try {
      return (Boolean) server.getClass().getMethod("isRunning").invoke(server);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke isRunning on server", e);
    }
  }
}
