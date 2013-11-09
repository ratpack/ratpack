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

package ratpack.groovy.templating.internal;

import ratpack.groovy.templating.TemplateModel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapBackedTemplateModel implements TemplateModel {

  private final Map<String, Object> backing;

  public MapBackedTemplateModel(Map<String, Object> backing) {
    this.backing = backing;
  }

  public int size() {
    return backing.size();
  }

  public boolean isEmpty() {
    return backing.isEmpty();
  }

  public boolean containsKey(Object key) {
    return backing.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return backing.containsValue(value);
  }

  public Object get(Object key) {
    return backing.get(key);
  }

  public Object put(String key, Object value) {
    return backing.put(key, value);
  }

  public Object remove(Object key) {
    return backing.remove(key);
  }

  @SuppressWarnings("NullableProblems")
  public void putAll(Map<? extends String, ?> m) {
    backing.putAll(m);
  }

  public void clear() {
    backing.clear();
  }

  @SuppressWarnings("NullableProblems")
  public Set<String> keySet() {
    return backing.keySet();
  }

  @SuppressWarnings("NullableProblems")
  public Collection<Object> values() {
    return backing.values();
  }

  @SuppressWarnings("NullableProblems")
  public Set<Entry<String, Object>> entrySet() {
    return backing.entrySet();
  }

  public <K, V> Map<K, V> map(String key, Class<K> keyType, Class<V> valueType) {
    Object value = get(key);
    if (value == null) {
      return null;
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    Map<K, V> mapValue = (Map<K, V>) value;
    return mapValue;
  }

  public Map<String, Object> map(String key) {
    return map(key, String.class, Object.class);
  }

  public <T> T get(String key, Class<T> type) {
    Object value = get(key);
    if (value == null) {
      return null;
    } else {
      if (type.isInstance(value)) {
        return type.cast(value);
      } else {
        throw new RuntimeException(String.format("Template model object with key %s is of type %s, not requested %s (toString: %s)", key, type, value.getClass().getName(), value));
      }
    }
  }
}
