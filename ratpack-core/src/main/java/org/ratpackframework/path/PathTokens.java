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

package org.ratpackframework.path;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * A map delegate that provides convenience methods for retrieving String values coerced to other basic types.
 */
public class PathTokens extends AbstractMap<String, String> {

  private final Map<String, String> delegate;

  public PathTokens(Map<String, String> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return delegate.entrySet();
  }

  public Boolean getAsBoolean(String key) {
    if (!containsKey(key)) {
      return null;
    }
    return Boolean.valueOf(get(key));
  }

  public Byte getAsByte(String key) {
    if (!containsKey(key)) {
      return null;
    }
    return Byte.valueOf(get(key));
  }

  public Short getAsShort(String key) {
    if (!containsKey(key)) {
      return null;
    }
    return Short.valueOf(get(key));
  }

  public Integer getAsInt(String key) {
    if (!containsKey(key)) {
      return null;
    }
    return Integer.valueOf(get(key));
  }

  public Long getAsLong(String key) {
    if (!containsKey(key)) {
      return null;
    }
    return Long.valueOf(get(key));
  }
}
