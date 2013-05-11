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

package org.ratpackframework.session.store;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class DefaultSessionStorage implements SessionStorage {

  private final ConcurrentMap<String, Object> delegate;

  public DefaultSessionStorage(ConcurrentMap<String, Object> delegate) {
    this.delegate = delegate;
  }

  public Object putIfAbsent(String key, Object value) {
    return delegate.putIfAbsent(key, value);
  }

  public boolean remove(Object key, Object value) {
    return delegate.remove(key, value);
  }

  public boolean replace(String key, Object oldValue, Object newValue) {
    return delegate.replace(key, oldValue, newValue);
  }

  public Object replace(String key, Object value) {
    return delegate.replace(key, value);
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
    return delegate.containsValue(value);
  }

  public Object get(Object key) {
    return delegate.get(key);
  }

  public Object put(String key, Object value) {
    return delegate.put(key, value);
  }

  public Object remove(Object key) {
    return delegate.remove(key);
  }

  public void putAll(Map<? extends String, ?> m) {
    delegate.putAll(m);
  }

  public void clear() {
    delegate.clear();
  }

  public Set<String> keySet() {
    return delegate.keySet();
  }

  public Collection<Object> values() {
    return delegate.values();
  }

  public Set<Entry<String, Object>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}
