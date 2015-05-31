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

package ratpack.session;

import ratpack.api.Nullable;
import ratpack.session.internal.DefaultSessionKey;
import ratpack.util.Types;

import java.util.Objects;

/**
 * A key for an object in the session.
 * <p>
 * A key is a combination of type and name.
 * A key must have a type, or name, or both.
 * <p>
 * A key stored in the session always has a type parameter.
 * That is, all keys returned by {@link SessionData#getKeys()} are guaranteed to have a type parameter.
 * A key with a name only can be used to retrieve an item from the session where the type is not known ahead of time.
 *
 * @param <T> the type
 */
public interface SessionKey<T> {

  /**
   * The name.
   *
   * @return the name
   */
  @Nullable
  String getName();

  /**
   * The type.
   *
   * @return the type
   */
  @Nullable
  Class<T> getType();

  /**
   * Creates a key of the given type with no name.
   *
   * @param type the type
   * @param <T> the type
   * @return a session key
   */
  static <T> SessionKey<T> of(Class<T> type) {
    return of(null, type);
  }

  /**
   * Creates a key of the given name and type.
   * <p>
   * If both arguments are {@code null}, an {@link IllegalArgumentException} will be thrown.
   *
   * @param name the name
   * @param type the type
   * @param <T> the type
   * @return a session key
   */
  static <T> SessionKey<T> of(@Nullable String name, @Nullable Class<T> type) {
    if (name == null && type == null) {
      throw new IllegalArgumentException("one of name or type must not be null");
    }
    return new DefaultSessionKey<>(name, type);
  }

  /**
   * Creates a key of the given name with no type.
   *
   * @param name the name
   * @return a session key
   */
  static SessionKey<?> of(String name) {
    return of(Objects.requireNonNull(name, "name cannot be null"), null);
  }

  /**
   * Creates a key of the given name, and the type of the given object (as provided by {@link Object#getClass()}).
   *
   * @param name the name
   * @param value the object who's runtime type will be used as the type of the key
   * @param <T> the type
   * @return a session key
   */
  static <T> SessionKey<T> ofType(String name, T value) {
    Class<T> type = Types.cast(value.getClass());
    return of(name, type);
  }

  /**
   * Creates a key of  type of the given object (as provided by {@link Object#getClass()}), and no name.
   *
   * @param value the object who's runtime type will be used as the type of the key
   * @param <T> the type
   * @return a session key
   */
  static <T> SessionKey<T> ofType(T value) {
    return ofType(null, value);
  }
}
