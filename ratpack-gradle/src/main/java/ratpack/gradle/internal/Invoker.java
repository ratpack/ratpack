/*
 * Copyright 2021 the original author or authors.
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

package ratpack.gradle.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Invoker {

  private final Method method;

  public Invoker(Method method) {
    this.method = method;
  }

  public static Object invokeParamless(String type, Object instance, String methodName) {
    return invokeParamless(loadClass(type), instance, methodName);
  }

  public static Object invokeParamless(Class<?> type, Object instance, String methodName) {
    return of(type, methodName).invoke(instance);
  }

  public static Invoker of(String type, String methodName, Class<?>... types) {
    return of(loadClass(type), methodName, types);
  }

  public static Invoker of(Class<?> type, String methodName, Class<?>... types) {
    try {
      return new Invoker(type.getMethod(methodName, types));
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public Object invoke(Object instance, Object... args) {
    try {
      return method.invoke(instance, args);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static Class<?> loadClass(String name) {
    try {
      return Invoker.class.getClassLoader().loadClass(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
