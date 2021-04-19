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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class DefaultSessionKey<T> implements SessionKey<T>, Externalizable {

  @Nullable
  private String name;
  @Nullable
  private Class<T> type;

  public DefaultSessionKey() {
  }

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

  private enum Type {
    NAME_ONLY, TYPE_ONLY, NAME_AND_TYPE;

    static Type typeOf(DefaultSessionKey<?> key) {
      if (key.name == null) {
        return Type.TYPE_ONLY;
      } else if (key.type == null) {
        return Type.NAME_ONLY;
      } else {
        return Type.NAME_AND_TYPE;
      }
    }
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeByte(1);
    out.writeByte(Type.typeOf(this).ordinal());
    if (name != null) {
      out.writeUTF(name);
    }
    if (type != null) {
      out.writeUTF(type.getName());
    }
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    int version = in.readUnsignedByte();
    if (version != 1) {
      throw new UnsupportedOperationException("Unexpected version: " + version);
    }
    int typeOrdinal = in.readUnsignedByte();
    if (typeOrdinal >= Type.values().length) {
      throw new IllegalArgumentException("invalid type ordinal: " + typeOrdinal);
    }
    Type type = Type.values()[typeOrdinal];

    if (type != Type.TYPE_ONLY) {
      this.name = in.readUTF();
    }

    if (type != Type.NAME_ONLY) {
      String className = in.readUTF();
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      @SuppressWarnings("unchecked") Class<T> cast = (Class<T>) classLoader.loadClass(className);
      this.type = cast;
    }
  }

  @Override
  public String toString() {
    return "SessionKey[name='" + name + '\'' + ", type=" + (type == null ? null : type.getName()) + ']';
  }
}
