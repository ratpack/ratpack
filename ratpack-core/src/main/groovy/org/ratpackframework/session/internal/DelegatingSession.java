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

import org.ratpackframework.session.Session;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("NullableProblems")
public class DelegatingSession implements Session {

  private final StoredSession storedSession;
  private final SessionManager sessionManager;

  public DelegatingSession(StoredSession storedSession, SessionManager sessionManager) {
    this.storedSession = storedSession;
    this.sessionManager = sessionManager;
  }

  @Override
  public void invalidate() {
    sessionManager.clearSession(getId());
  }

  @Override
  public String getId() {
    return storedSession.getId();
  }

  @Override
  public Object putIfAbsent(String key, Object value) {
    return storedSession.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return storedSession.remove(key, value);
  }

  @Override
  public boolean replace(String key, Object oldValue, Object newValue) {
    return storedSession.replace(key, oldValue, newValue);
  }

  @Override
  public Object replace(String key, Object value) {
    return storedSession.replace(key, value);
  }

  @Override
  public int size() {
    return storedSession.size();
  }

  @Override
  public boolean isEmpty() {
    return storedSession.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return storedSession.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return storedSession.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return storedSession.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    return storedSession.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return storedSession.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    storedSession.putAll(m);
  }

  @Override
  public void clear() {
    storedSession.clear();
  }

  @Override
  public Set<String> keySet() {
    return storedSession.keySet();
  }

  @Override
  public Collection<Object> values() {
    return storedSession.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return storedSession.entrySet();
  }

}
