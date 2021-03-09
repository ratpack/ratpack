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

import ratpack.session.internal.InsecureSessionSerializerWarning;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A serializer converts objects to bytes and vice versa.
 * <p>
 * The {@link SessionModule} provides a default implementation that uses Java's in built serialization.
 *
 * @see JavaSessionSerializer
 */
public interface SessionSerializer {

  /**
   * Writes the given value to the output stream as bytes.
   * <p>
   * This method has been superseded by {@link #serialize(Class, Object, OutputStream, SessionTypeFilter)} in 1.9.
   * Implementations should not implement this method, but that instead.
   *
   * @param type the declared type of the object
   * @param value the value to serialize
   * @param out the destination for the bytes
   * @param <T> the type of the object
   * @throws Exception if the value could not be serialized
   * @deprecated since 1.9
   */
  @Deprecated
  default <T> void serialize(Class<T> type, T value, OutputStream out) throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Writes the given value to the output stream as bytes.
   * <p>
   * Implementations <b>MUST</b> take care to check that <i>all</i> types serialized are allowed to be as per
   * {@code typeFilter}. This includes the type of {@code value} and the transitive types referenced by it.
   * Implementations should use {@link SessionTypeFilter#assertAllowed(Class)}.
   * <p>
   * To enable backwards compatibility, the default implementation delegates to {@link #serialize(Class, Object, OutputStream)}
   * after logging a warning about the inherent security vulnerability in not checking the suitability of types.
   * All implementations should implement this method and not that method.
   *
   * @param type the declared type of the object
   * @param value the value to serialize
   * @param out the destination for the bytes
   * @param typeFilter the filter that determines whether a type is session safe and allowed to be serialized
   * @param <T> the type of the object
   * @throws Exception if the value could not be serialized
   */
  default <T> void serialize(Class<T> type, T value, OutputStream out, @SuppressWarnings("unused") SessionTypeFilter typeFilter) throws Exception {
    InsecureSessionSerializerWarning.log(getClass());
    serialize(type, value, out);
  }

  /**
   * Reads the bytes of the given input stream, creating a new object.
   * <p>
   * This method has been superseded by {@link #serialize(Class, Object, OutputStream, SessionTypeFilter)} in 1.9.
   * Implementations should not implement this method, but that instead.
   *
   * @param type the expected type of the object
   * @param in the source of the bytes
   * @param <T> the type of the object
   * @return the object
   * @throws IOException any thrown by {@code in}
   * @throws Exception the the value could not be deserialized
   * @deprecated since 1.9
   */
  @Deprecated
  default <T> T deserialize(Class<T> type, InputStream in) throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Reads the bytes of the given input stream, creating a new object.
   * <p>
   * Implementations <b>MUST</b> take care to check that <i>all</i> types to be deserialized are allowed to be as per
   * {@code typeFilter}. This includes the type of the object being deserialized and the transitive types referenced by it.
   * Implementations should use {@link SessionTypeFilter#assertAllowed(Class)}.
   * <p>
   * To enable backwards compatibility, the default implementation delegates to {@link #deserialize(Class, InputStream)}
   * after logging a warning about the inherent security vulnerability in not checking the suitability of types.
   * All implementations should implement this method and not that method.
   *
   * @param type the expected type of the object
   * @param in the source of the bytes
   * @param typeFilter the filter that determines whether a type is session safe and allowed to be deserialized
   * @param <T> the type of the object
   * @return the object
   * @throws IOException any thrown by {@code in}
   * @throws Exception the the value could not be deserialized
   */
  default <T> T deserialize(Class<T> type, InputStream in, @SuppressWarnings("unused") SessionTypeFilter typeFilter) throws Exception {
    InsecureSessionSerializerWarning.log(getClass());
    return deserialize(type, in);
  }

}
