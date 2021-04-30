/*
 * Copyright 2021 the original author or authors.
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

package ratpack.session.serialization.kryo;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Registration;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.KryoObjectInput;
import com.esotericsoftware.kryo.kryo5.io.KryoObjectOutput;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.ExternalizableSerializer;
import com.esotericsoftware.kryo.kryo5.util.DefaultClassResolver;
import com.google.inject.Singleton;
import io.netty.util.concurrent.FastThreadLocal;
import ratpack.session.JavaSessionSerializer;
import ratpack.session.SessionModule;
import ratpack.session.SessionTypeFilter;

import java.io.*;

/**
 * A <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> based session data serialization implementation.
 * <p>
 * For use in combination with {@link SessionModule}.
 * To use, override the {@link JavaSessionSerializer} binding provided by that module with an instance of this class.
 *
 * This serializer supports session type filtering via {@link SessionTypeFilter}.
 *
 * @since 1.9
 */
@Singleton
public class KryoSessionSerializer implements JavaSessionSerializer {

  private static final class KryoRef {

    private final SessionTypeFilter filter;
    private final Kryo kryo;

    KryoRef(SessionTypeFilter filter, Kryo kryo) {
      this.filter = filter;
      this.kryo = kryo;
    }
  }

  private static final class FilteringClassResolver extends DefaultClassResolver {

    private final SessionTypeFilter filter;

    FilteringClassResolver(SessionTypeFilter filter) {
      this.filter = filter;
    }

    @Override
    @SuppressWarnings("rawtypes") // not actually redundant, need by javac
    protected void writeName(Output output, Class type, Registration registration) {
      filter.assertAllowed(type.getName());
      super.writeName(output, type, registration);
    }

    @Override
    @SuppressWarnings("rawtypes") // not actually redundant, need by javac
    protected Registration readName(Input input) {
      Registration registration = super.readName(input);
      filter.assertAllowed(registration.getType().getName());
      return registration;
    }
  }

  private static final FastThreadLocal<KryoRef> KRYO = new FastThreadLocal<>();

  private Kryo kryo(SessionTypeFilter typeFilter) {
    KryoRef kryoRef = KRYO.get();
    if (kryoRef == null || !kryoRef.filter.equals(typeFilter)) {
      kryoRef = new KryoRef(typeFilter, createKryo(typeFilter));
      KRYO.set(kryoRef);
    }
    return kryoRef.kryo;
  }

  private Kryo createKryo(SessionTypeFilter filter) {
    final Kryo kryo;
    kryo = new Kryo(new FilteringClassResolver(filter), null);
    kryo.setRegistrationRequired(false);
    kryo.addDefaultSerializer(Externalizable.class, new ExternalizableSerializer());

    configureKryo(kryo);
    return kryo;
  }

  /**
   * A hook for potential subclasses to configure Kryo instances used.
   * <p>
   * Internally, kryo instances are pooled and reused.
   * This method may be called any time, as new instances are needed.
   * All instances should be configured identically.
   *
   * @param kryo the instance to configure
   */
  protected void configureKryo(@SuppressWarnings("unused") Kryo kryo) {

  }

  @Override
  public <T> void serialize(Class<T> type, T value, OutputStream out, SessionTypeFilter typeFilter) throws Exception {
    try (ObjectOutput objectOut = new KryoObjectOutput(kryo(typeFilter), new Output(out))) {
      objectOut.writeObject(value);
    }
  }

  @Override
  public <T> T deserialize(Class<T> type, InputStream in, SessionTypeFilter typeFilter) throws Exception {
    try (ObjectInput objectInput = new KryoObjectInput(kryo(typeFilter), new Input(in))) {
      Object obj = objectInput.readObject();
      if (type.isInstance(obj)) {
        return type.cast(obj);
      } else {
        throw new IllegalStateException("Expected " + type + " got " + obj.getClass());
      }
    }
  }

}
