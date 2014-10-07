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

package ratpack.configuration.util.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import static com.google.common.base.Preconditions.checkNotNull;

public class Generics {
  private Generics() { /* singleton */ }

  /**
   * Finds the type parameter for the given class.
   *
   * @param klass    a parameterized class
   * @return the class's type parameter
   */
  public static Class<?> getTypeParameter(Class<?> klass) {
      return getTypeParameter(klass, Object.class);
  }

  /**
   * Finds the type parameter for the given class which is assignable to the bound class.
   *
   * @param klass    a parameterized class
   * @param bound    the type bound
   * @param <T>      the type bound
   * @return the class's type parameter
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getTypeParameter(Class<?> klass, Class<? super T> bound) {
      Type t = checkNotNull(klass);
      while (t instanceof Class<?>) {
          t = ((Class<?>) t).getGenericSuperclass();
      }
      /* This is not guaranteed to work for all cases with convoluted piping
       * of type parameters: but it can at least resolve straight-forward
       * extension with single type parameter (as per [Issue-89]).
       * And when it fails to do that, will indicate with specific exception.
       */
      if (t instanceof ParameterizedType) {
          // should typically have one of type parameters (first one) that matches:
          for (Type param : ((ParameterizedType) t).getActualTypeArguments()) {
              if (param instanceof Class<?>) {
                  final Class<T> cls = determineClass(bound, param);
                  if (cls != null) { return cls; }
              }
              else if (param instanceof TypeVariable) {
                  for (Type paramBound : ((TypeVariable<?>) param).getBounds()) {
                      if (paramBound instanceof Class<?>) {
                          final Class<T> cls = determineClass(bound, paramBound);
                          if (cls != null) { return cls; }
                      }
                  }
              }
          }
      }
      throw new IllegalStateException("Cannot figure out type parameterization for " + klass.getName());
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> determineClass(Class<? super T> bound, Type candidate) {
      if (candidate instanceof Class<?>) {
          final Class<?> cls = (Class<?>) candidate;
          if (bound.isAssignableFrom(cls)) {
              return (Class<T>) cls;
          }
      }

      return null;
  }
}
