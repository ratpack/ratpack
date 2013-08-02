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

package org.ratpackframework.handling;

import com.google.common.collect.ImmutableList;
import org.ratpackframework.handling.internal.ServiceExtractor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public abstract class ServiceUsingHandler implements Handler {

  private final List<Class<?>> serviceTypes;
  private final Method handleMethod;

  protected ServiceUsingHandler() {
    Class<?> thisClass = this.getClass();

    Method handleMethod = null;
    for (Method method : thisClass.getDeclaredMethods()) {
      if (!method.getName().equals("handle")) {
        continue;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length < 2) {
        continue;
      }

      if (!parameterTypes[0].equals(Context.class)) {
        continue;
      }

      handleMethod = method;
      break;
    }

    if (handleMethod == null) {
      throw new NoSuitableHandleMethodException(thisClass);
    }

    this.handleMethod = handleMethod;
    Class<?>[] parameterTypes = handleMethod.getParameterTypes();
    this.serviceTypes = ImmutableList.copyOf(Arrays.asList(parameterTypes).subList(1, parameterTypes.length));
  }

  public void handle(Context context) {
    Object[] args = new Object[serviceTypes.size() + 1];
    args[0] = context;
    ServiceExtractor.extract(serviceTypes, context, args, 1);
    try {
      handleMethod.invoke(this, args);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      Throwable root = e.getTargetException();
      if (root instanceof RuntimeException) {
        throw (RuntimeException) root;
      } else {
        throw new RuntimeException(root);
      }
    }
  }

  public static class NoSuitableHandleMethodException extends RuntimeException {
    private static final long serialVersionUID = 0;
    public NoSuitableHandleMethodException(Class<?> clazz) {
      super("No injectable handle method found for " + clazz.getName());
    }
  }

}
