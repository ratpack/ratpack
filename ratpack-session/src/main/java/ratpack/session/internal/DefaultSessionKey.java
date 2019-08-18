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

import ratpack.api.Nullable;
import ratpack.session.SessionKey;

public class DefaultSessionKey<T> implements SessionKey<T> {

  private final String name;
  private final Class<T> type;

  public DefaultSessionKey(String name, Class<T> type) {
    this.name = name;
    this.type = type;
  }

  @Nullable
  @Override
  public String getName() {
    return name;
  }

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

    return !(name != null ? !name.equals(that.name) : that.name != null) && !(type != null ? !type.equals(that.type) : that.type != null);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SessionKey[name='" + name + '\'' + ", type=" + type.getName() + ']';
  }
}
