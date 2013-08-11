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

package org.ratpackframework.api;

import java.lang.reflect.*;

/**
 * Representation of a type literal that supports generic type.
 *
 * @param <T> The type.
 */
public abstract class TypeLiteral<T> {

  private final Class<? super T> rawType;

  protected TypeLiteral() {
    @SuppressWarnings("unchecked")
    Class<? super T> rawType = (Class<? super T>) getRawType(getSuperclassTypeParameter(getClass()));
    this.rawType = rawType;
  }

  public Class<? super T> getRawType() {
    return rawType;
  }

  private static Type getSuperclassTypeParameter(Class<?> subclass) {
    Type superclass = subclass.getGenericSuperclass();
    if (superclass instanceof Class) {
      throw new RuntimeException("Missing type parameter.");
    }
    ParameterizedType parameterized = (ParameterizedType) superclass;
    return parameterized.getActualTypeArguments()[0];
  }

  private static Class<?> getRawType(Type type) {
    if (type instanceof Class<?>) {
      // type is a normal class.
      return (Class<?>) type;

    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;

      Type rawType = parameterizedType.getRawType();
      return (Class<?>) rawType;
    } else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType)type).getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();

    } else if (type instanceof TypeVariable) {
      return Object.class;

    } else {
      throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
        + "GenericArrayType, but <" + type + "> is of type " + type.getClass().getName());
    }
  }

}
