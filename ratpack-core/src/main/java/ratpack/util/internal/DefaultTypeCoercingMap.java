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

import ratpack.util.TypeCoercingMap;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

public class DefaultTypeCoercingMap<K> extends AbstractMap<K, String> implements TypeCoercingMap<K> {

  private final Map<K, String> delegate;

  public DefaultTypeCoercingMap(Map<K, String> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Boolean asBool(K key) {
    if (!containsKey(key)) {
      return null;
    }
    return Boolean.valueOf(get(key));
  }

  @Override
  public Byte asByte(K key) {
    if (!containsKey(key)) {
      return null;
    }
    return Byte.valueOf(get(key));
  }

  @Override
  public Short asShort(K key) {
    if (!containsKey(key)) {
      return null;
    }
    return Short.valueOf(get(key));
  }

  @Override
  public Integer asInt(K key) {
    if (!containsKey(key)) {
      return null;
    }
    return Integer.valueOf(get(key));
  }

  @Override
  public Long asLong(K key) {
    if (!containsKey(key)) {
      return null;
    }
    return Long.valueOf(get(key));
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Set<Entry<K, String>> entrySet() {
    return delegate.entrySet();
  }
}
