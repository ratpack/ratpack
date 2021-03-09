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

import ratpack.session.JavaSessionSerializer;
import ratpack.session.SessionTypeFilter;

import java.io.*;

public class JavaBuiltinSessionSerializer implements JavaSessionSerializer {

  @Override
  public <T> void serialize(Class<T> type, T value, OutputStream outputStream, SessionTypeFilter typeFilter) throws IOException {
    try (ObjectOutputStream objectOutputStream = new TypeFilteringObjectOutputStream(outputStream, typeFilter)) {
      objectOutputStream.writeObject(value);
    }
  }

  @Override
  public <T> T deserialize(Class<T> type, InputStream in, SessionTypeFilter typeFilter) throws IOException, ClassNotFoundException {
    try (ObjectInputStream objectInputStream = new TypeFilteringObjectInputStream(in, typeFilter)) {
      Object value = objectInputStream.readObject();
      if (type.isInstance(value)) {
        return type.cast(value);
      } else {
        throw new ClassCastException("Expected to read object of type " + type.getName() + " from string, but got: " + value.getClass().getName());
      }
    }
  }

  private static final class TypeFilteringObjectOutputStream extends ObjectOutputStream {
    private final SessionTypeFilter typeFilter;

    public TypeFilteringObjectOutputStream(OutputStream out, SessionTypeFilter typeFilter) throws IOException {
      super(out);
      this.typeFilter = typeFilter;
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
      typeFilter.assertAllowed(desc.forClass());
      super.writeClassDescriptor(desc);
    }
  }

  private static final class TypeFilteringObjectInputStream extends ObjectInputStream {
    private final SessionTypeFilter typeFilter;

    public TypeFilteringObjectInputStream(InputStream in, SessionTypeFilter typeFilter) throws IOException {
      super(in);
      this.typeFilter = typeFilter;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      Class<?> type = super.resolveClass(desc);
      typeFilter.assertAllowed(type);
      return type;
    }

  }
}
