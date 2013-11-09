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

package ratpack.util.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class Types {

  public static ParameterizedType findParameterizeImplType(Type type, Class<?> searchType) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      if (rawType.equals(searchType)) {
        return parameterizedType;
      } else {
        return findParameterizeImplType(rawType.getGenericSuperclass(), searchType);
      }
    } else if (type instanceof Class) {
      Class<?> classType = (Class) type;
      if (type.equals(Object.class)) {
        throw new IllegalStateException(type + " does not extend " + searchType);
      }
      return findParameterizeImplType(classType.getGenericSuperclass(), searchType);
    } else {
      throw new IllegalStateException("Unhandled type: " + type);
    }
  }

  public static <T> Class<T> findImplParameterTypeAtIndex(Class<?> type, Class<?> searchType, int typeIndex) {
    ParameterizedType parameterizedType = findParameterizeImplType(type.getGenericSuperclass(), searchType);
    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    if (typeIndex >= actualTypeArguments.length) {
      throw new IllegalArgumentException("Cannot get type at index " + typeIndex + " of " + searchType + " impl of " + type + " as it only has " + actualTypeArguments.length + " types");
    }

    Type tType = actualTypeArguments[typeIndex];

    if (tType instanceof Class) {
      @SuppressWarnings("unchecked") Class<T> castType = (Class<T>) tType;
      return castType;
    } else if (tType instanceof ParameterizedType) {
      @SuppressWarnings("unchecked") Class<T> castType = (Class<T>) ((ParameterizedType) tType).getRawType();
      return castType;
    } else {
      throw new IllegalStateException("Type parameter " + typeIndex + " of " + type + " of impl " + searchType + " is not concrete");
    }
  }
}
