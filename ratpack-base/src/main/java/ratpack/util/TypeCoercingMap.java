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

package ratpack.util;

import java.util.Map;

/**
 * A string valued map that can do simple type conversions from the string values to primitive types.
 *
 * @param <K> The type of the keys
 */
@SuppressWarnings("UnusedDeclaration")
public interface TypeCoercingMap<K> extends Map<K, String> {

  /**
   * Convert the value with given key to a Boolean, using {@link Boolean#valueOf(String)}.
   *
   * @param key The key of the value to convert
   * @return The result of {@link Boolean#valueOf(String)} if the value is not null, else null
   */
  Boolean asBool(K key);

  /**
   * Convert the value with given key to a Byte, using {@link Byte#valueOf(String)}.
   *
   * @param key The key of the value to convert
   * @return The result of {@link Byte#valueOf(String)} if the value is not null, else null
   */
  Byte asByte(K key);

  /**
   * Convert the value with given key to a Short, using {@link Short#valueOf(String)}.
   *
   * @param key The key of the value to convert
   * @return The result of {@link Short#valueOf(String)} if the value is not null, else null
   * @throws NumberFormatException if the value cannot be coerced
   */
  Short asShort(K key) throws NumberFormatException;

  /**
   * Convert the value with given key to a Integer, using {@link Integer#valueOf(String)}.
   *
   * @param key The key of the value to convert
   * @return The result of {@link Integer#valueOf(String)} if the value is not null, else null
   * @throws NumberFormatException if the value cannot be coerced
   */
  Integer asInt(K key) throws NumberFormatException;

  /**
   * Convert the value with given key to a Long, using {@link Long#valueOf(String)}.
   *
   * @param key The key of the value to convert
   * @return The result of {@link Long#valueOf(String)} if the value is not null, else null
   * @throws NumberFormatException if the value cannot be coerced
   */
  Long asLong(K key) throws NumberFormatException;

}
