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

package ratpack.util;


import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;
import ratpack.registry.internal.TypeCaching;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Static utility methods for dealing with types.
 */
public abstract class Types {

  private Types() {
  }

  /**
   * Simply casts the argument to {@code T}.
   * <p>
   * This method will throw {@link ClassCastException} if {@code o} is not compatible with {@code T}.
   *
   * @param o the object to cast
   * @param <T> the target type
   * @return the input object cast to the target type
   */
  public static <T> T cast(Object o) {
    @SuppressWarnings("unchecked") T cast = (T) o;
    return cast;
  }

  /**
   * Create a type token for the given runtime type.
   *
   * This method should be preferred over {@link TypeToken#of(Type)} as the result may be interned when not in development.
   *
   * @param type the type
   * @return a type token for the given type
   * @since 1.1
   */
  public static TypeToken<?> token(Type type) {
    return TypeCaching.typeToken(type);
  }

  /**
   * Create a type token for the given class.
   *
   * This method should be preferred over {@link TypeToken#of(Class)} as the result may be interned when not in development.
   *
   * @param clazz the class
   * @param <T> the type
   * @return a type token for the given class
   * @since 1.1
   */
  public static <T> TypeToken<T> token(Class<T> clazz) {
    return TypeCaching.typeToken(clazz);
  }

  /**
   * Intern the given type token.
   *
   * @param typeToken the type token to intern
   * @param <T> the type
   * @return the given type token interned
   * @since 1.5
   */
  public static <T> TypeToken<T> intern(TypeToken<T> typeToken) {
    return TypeCaching.typeToken(typeToken);
  }

  /**
   * Creates a type token for a list of of the given type.
   * <pre class="java">{@code
   * import ratpack.util.Types;
   * import com.google.common.reflect.TypeToken;
   *
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) {
   *     assertEquals(Types.listOf(String.class), new TypeToken<List<String>>() {});
   *   }
   * }
   * }</pre>
   *
   * @param type the list element type
   * @param <T> the list element type
   * @return a type token for a list of of the given type.
   */
  public static <T> TypeToken<List<T>> listOf(Class<T> type) {
    return new TypeToken<List<T>>() {
    }.where(new TypeParameter<T>() {
    }, token(type));
  }

  /**
   * Creates a type token for a promise of of the given type.
   * <pre class="java">{@code
   * import ratpack.util.Types;
   * import ratpack.exec.Promise;
   * import com.google.common.reflect.TypeToken;
   *
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   {@literal @}SuppressWarnings("deprecation")
   *   public static void main(String... args) {
   *     assertEquals(Types.promiseOf(String.class), new TypeToken<Promise<String>>() {});
   *   }
   * }
   * }</pre>
   *
   * @param type the promise element type
   * @param <T> the promise element type
   * @return a type token for a promise of of the given type.
   * @deprecated since 1.5, no replacement.
   */
  @Deprecated
  public static <T> TypeToken<Promise<T>> promiseOf(Class<T> type) {
    return promiseOf(token(type));
  }

  /**
   * Creates a type token for a promise of the given type.
   *
   * <pre class="java">{@code
   * import ratpack.util.Types;
   * import ratpack.exec.Promise;
   * import com.google.common.reflect.TypeToken;
   *
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   {@literal @}SuppressWarnings("deprecation")
   *   public static void main(String... args) {
   *     assertEquals(
   *       Types.promiseOf(new TypeToken<List<String>>() {}),
   *       new TypeToken<Promise<List<String>>>() {}
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param type the promise element type
   * @param <T> the promise element type
   * @return a type token for a promise of of the given type.
   * @deprecated since 1.5, no replacement.
   */
  @Deprecated
  public static <T> TypeToken<Promise<T>> promiseOf(TypeToken<T> type) {
    return new TypeToken<Promise<T>>() {
    }.where(new TypeParameter<T>() {
    }, type);
  }

}
