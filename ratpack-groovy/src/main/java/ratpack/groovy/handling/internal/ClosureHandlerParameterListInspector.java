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

package ratpack.groovy.handling.internal;

import com.google.common.collect.ImmutableList;
import groovy.lang.Closure;

import java.util.Collections;
import java.util.List;

public abstract class ClosureHandlerParameterListInspector {

  private ClosureHandlerParameterListInspector() {
  }

  public static List<Class<?>> retrieveParameterTypes(Closure<?> closure) {
    Class[] parameterTypes = closure.getParameterTypes();
    if (parameterTypes.length == 1 && parameterTypes[0].equals(Object.class)) {
      return Collections.emptyList();
    } else {
      for (Class<?> clazz : parameterTypes) {
        if (clazz.isArray()) {
          throw new IllegalStateException("Handler closure parameters cannot be array types (type: " + clazz.getName() + ", closure: " + closure.getClass().getName() + ")");
        }
      }

      return ImmutableList.<Class<?>>copyOf(parameterTypes);
    }
  }
}
