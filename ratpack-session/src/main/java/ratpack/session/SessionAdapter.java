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

import ratpack.exec.Promise;

import java.util.Optional;
import java.util.Set;

// Will eventually replace Session
public interface SessionAdapter {
  Optional<String> get(String key);

  default String require(String key) {
    return get(key).orElseThrow(() -> new IllegalArgumentException("No value for string key " + key + " in session"));
  }

  <T> Optional<? extends T> get(Class<T> key);

  default <T> T require(Class<T> key) {
    return get(key).orElseThrow(() -> new IllegalArgumentException("No object of type " + key.getName() + " in session"));
  }

  <T> Optional<? extends T> get(Class<T> key, SessionValueSerializer serializer);

  default <T> T require(Class<T> key, SessionValueSerializer serializer) {
    return get(key, serializer).orElseThrow(() -> new IllegalArgumentException("No object of type " + key.getName() + " in session"));
  }

  void set(String key, String value);

  <T> void set(Class<T> key, T value);

  <T> void set(Class<T> key, T value, SessionValueSerializer serializer);

  <T> void set(T value);

  <T> void set(T value, SessionValueSerializer serializer);

  Set<String> getStringKeys();

  Set<Class<?>> getTypeKeys();

  void remove(String key);

  <T> void remove(Class<T> key);

  void clear();

  // Has the session been changed (i.e. set/remove/clear called) since read?
  boolean isDirty();

  // Store the session data right now - doesn't have to be called - we'll call automatically at end of request if dirty
  Promise<Boolean> save();

  Promise<Boolean> terminate();
}
