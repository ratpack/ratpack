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

package ratpack.groovy.template;

import com.google.common.reflect.TypeToken;

import java.util.Map;

public interface TextTemplateModel extends Map<String, Object> {

  <K, V> Map<K, V> map(String key, Class<K> keyType, Class<V> valueType);

  Map<String, Object> map(String key);

  /**
   * Retrieve the given model item, of the given type.
   * <p>
   * If there is no model item with the given key, {@code null} is returned.
   * <p>
   * If the model item with the given key is assignment compatible with the given type it is returned.
   * <p>
   * If the model item is NOT assignment compatible with the given type, an {@link IllegalArgumentException} will be thrown.
   * <p>
   * Note: generic types are completely ignored for assignment compatiblity, due to Java's type erasure.
   *
   * @param key the model item key
   * @param type the target type of the model item
   * @param <T> the target type of the model item
   * @return the model item
   */
  <T> T get(String key, Class<T> type);

  /**
   * Retrieve the given model item, of the given type.
   * <p>
   * If there is no model item with the given key, {@code null} is returned.
   * <p>
   * If the model item with the given key is assignment compatible with the given type it is returned.
   * <p>
   * If the model item is NOT assignment compatible with the given type, an {@link IllegalArgumentException} will be thrown.
   * <p>
   * Note: generic types are completely ignored for assignment compatiblity, due to Java's type erasure.
   *
   * @param key the model item key
   * @param type the target type of the model item
   * @param <T> the target type of the model item
   * @return the model item
   */
  <T> T get(String key, TypeToken<T> type);

}
