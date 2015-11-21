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

package ratpack.util.internal;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import ratpack.util.MultiValueMapBuilder;
import ratpack.util.MultiValueMap;

import java.util.Map;

public class DefaultMultiValueMapBuilder<K, V> implements MultiValueMapBuilder<K, V> {

  private ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();

  public DefaultMultiValueMapBuilder() {

  }

  @Override
  public DefaultMultiValueMapBuilder<K, V> put(K key, V value) {
    builder.put(key, value);
    return this;
  }

  @Override
  public DefaultMultiValueMapBuilder<K, V> putAll(K key, Iterable<? extends V> values) {
    builder.putAll(key, values);
    return this;
  }

  @Override
  public <I extends Iterable<? extends V>> MultiValueMapBuilder<K, V> putAll(Map<K, I> map) {
    for(Map.Entry<K, I> e : map.entrySet()) {
      builder.putAll(e.getKey(), e.getValue());
    }
    return this;
  }

  @Override
  public MultiValueMap<K, V> build() {
    return new ImmutableDelegatingMultiValueMap<>(Multimaps.asMap(builder.build()));
  }
}
