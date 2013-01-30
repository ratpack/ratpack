/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.session.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("NullableProblems")
public class DefaultStoredSession implements StoredSession {

  private final ConcurrentMap<String, Object> storage = new ConcurrentHashMap<>();

  private final String id;

  public DefaultStoredSession(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public Object putIfAbsent(String key, Object value) {
    return storage.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return storage.remove(key, value);
  }

  @Override
  public boolean replace(String key, Object oldValue, Object newValue) {
    return storage.replace(key, oldValue, newValue);
  }

  @Override
  public Object replace(String key, Object value) {
    return storage.replace(key, value);
  }

  @Override
  public int size() {
    return storage.size();
  }

  @Override
  public boolean isEmpty() {
    return storage.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return storage.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return storage.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return storage.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    return storage.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return storage.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    storage.putAll(m);
  }

  @Override
  public void clear() {
    storage.clear();
  }

  @Override
  public Set<String> keySet() {
    return storage.keySet();
  }

  @Override
  public Collection<Object> values() {
    return storage.values();
  }

  @Override
  public Set<Entry<String,Object>> entrySet() {
    return storage.entrySet();
  }

}
