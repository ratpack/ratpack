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

package ratpack.util;

import java.util.Map;

/**
 * Builds a {@link MultiValueMap}.
 *
 * @param <K> the key type of the map
 * @param <V> the value type of the map
 */
public interface MultiValueMapBuilder<K, V> {

  /**
   *
   * @param key
   * @param value
   * @return
   */
  MultiValueMapBuilder<K, V> put(K key, V value);

  /**
   *
   * @param key
   * @param value
   * @return
   */
  MultiValueMapBuilder<K, V> putAll(K key, Iterable<? extends V> value);

  /**
   *
   * @param map
   * @return
   */
  <I extends Iterable<? extends V>> MultiValueMapBuilder<K, V> putAll(Map<K, I> map);

  /**
   *
   * @return
   */
  MultiValueMap<K, V> build();
}
