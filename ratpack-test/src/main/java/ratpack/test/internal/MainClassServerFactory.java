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

package ratpack.test.internal;

import ratpack.func.Factory;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.internal.ServerCapturer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MainClassServerFactory implements Factory<RatpackServer> {

  private final Class<?> mainClass;
  private final Registry registry;

  public MainClassServerFactory(Class<?> mainClass, Registry registry) {
    this.mainClass = mainClass;
    this.registry = registry;
  }

  @Override
  public RatpackServer create() throws Exception {
    return ServerCapturer.capture(new ServerCapturer.Overrides(0, registry), () -> {
      Method method;
      try {
        method = mainClass.getDeclaredMethod("main", String[].class);
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException("Class" + mainClass.getName() + " does not have a main(String...) class");
      }

      if (!Modifier.isStatic(method.getModifiers())) {
        throw new IllegalStateException(mainClass.getName() + ".main() must be static");
      }

      try {
        method.invoke(null, new Object[]{new String[]{}});
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new IllegalStateException("Could not invoke " + mainClass.getName() + ".main()", e);
      }

    }).orElseThrow(() -> new IllegalStateException(mainClass.getName() + ".main() did not start a Ratpack server"));
  }
}
