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

import com.google.common.base.MoreObjects;
import com.google.common.collect.*;
import ratpack.util.MultiValueMap;
import ratpack.util.Types;

import java.util.*;
import java.util.stream.Collectors;

public class ImmutableDelegatingMultiValueMap<K, V> implements MultiValueMap<K, V> {

  private static final ImmutableDelegatingMultiValueMap<Object, Object> EMPTY = new ImmutableDelegatingMultiValueMap<>(ImmutableMap.of());

  @SuppressWarnings("unchecked")
  public static <K, V> MultiValueMap<K, V> empty() {
    return Types.cast(EMPTY);
  }

  private final Map<K, List<V>> delegate;

  public ImmutableDelegatingMultiValueMap(Map<K, List<V>> map) {
    this.delegate = map;
  }

  @SuppressWarnings("unchecked")
  public List<V> getAll(K key) {
    return MoreObjects.firstNonNull(delegate.get(key), Collections.<V>emptyList());
  }

  @SuppressWarnings("unchecked")
  public Map<K, List<V>> getAll() {
    return delegate;
  }

  public int size() {
    return delegate.size();
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  public boolean containsKey(Object key) {
    return delegate.containsKey(key);
  }

  public boolean containsValue(Object value) {
    for (List<? extends V> values : delegate.values()) {
      //noinspection SuspiciousMethodCalls
      if (values.contains(value)) {
        return true;
      }
    }
    return false;
  }

  public V get(Object key) {
    List<? extends V> result = delegate.get(key);
    return result != null && result.size() > 0 ? result.get(0) : null;
  }

  public V put(K key, V value) {
    throw new UnsupportedOperationException("This implementation is immutable");
  }

  public V remove(Object key) {
    throw new UnsupportedOperationException("This implementation is immutable");
  }

  @SuppressWarnings("NullableProblems")
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException("This implementation is immutable");
  }

  public void clear() {
    throw new UnsupportedOperationException("This implementation is immutable");
  }

  @Override
  public ListMultimap<K, V> asMultimap() {
    ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
    for (Entry<? extends K, ? extends List<? extends V>> entry : delegate.entrySet()) {
      builder.putAll(entry.getKey(), entry.getValue());
    }
    return builder.build();
  }

  @SuppressWarnings({"unchecked", "NullableProblems"})
  public Set<K> keySet() {
    return delegate.keySet();
  }

  @SuppressWarnings("NullableProblems")
  public Collection<V> values() {
    return Lists.newArrayList(Iterables.concat(delegate.values()));
  }

  @SuppressWarnings("NullableProblems")
  public Set<Entry<K, V>> entrySet() {
    return delegate.entrySet().stream()
      .filter(entry -> entry.getValue().size() > 0)
      .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get(0)))
      .collect(Collectors.toSet());
  }

  @Override
  public String toString() {
    if (delegate.isEmpty()) {
      return "[:]";
    }
    StringBuilder buffer = new StringBuilder("[");
    boolean first = true;
    for (Entry<? extends K, ? extends List<? extends V>> entry : delegate.entrySet()) {
      if (first) {
        first = false;
      } else {
        buffer.append(", ");
      }
      buffer.append(entry.getKey().toString());
      buffer.append(":[");
      boolean firstValue = true;
      for (V value : entry.getValue()) {
        if (firstValue) {
          firstValue = false;
        } else {
          buffer.append(", ");
        }
        buffer.append(value.toString());
      }
      buffer.append("]");
    }
    buffer.append("]");
    return buffer.toString();
  }
}
