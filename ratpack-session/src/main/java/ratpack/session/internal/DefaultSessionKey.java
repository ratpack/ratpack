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

package ratpack.session.internal;

import ratpack.func.Nullable;
import ratpack.session.SessionKey;

import java.util.Objects;

public class DefaultSessionKey<T> implements SessionKey<T> {

  @Nullable
  private final String name;
  @Nullable
  private final Class<T> type;

  public DefaultSessionKey(@Nullable String name, @Nullable Class<T> type) {
    if (name == null && type == null) {
      throw new IllegalArgumentException("one of name or type must not be null");
    }
    this.name = name;
    this.type = type;
  }

  @Nullable
  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public Class<T> getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultSessionKey<?> that = (DefaultSessionKey<?>) o;

    return Objects.equals(name, that.name) && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SessionKey[name='" + name + '\'' + ", type=" + (type == null ? null : type.getName()) + ']';
  }
}
