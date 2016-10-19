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
   *
   * @param type the declared type of the object
   * @param value the value to serialize
   * @param out the destination for the bytes
   * @param <T> the type of the object
   * @throws Exception if the value could not be serialized
   */
  <T> void serialize(Class<T> type, T value, OutputStream out) throws Exception;

  /**
   * Reads the bytes of the given input stream, creating a new object.
   *
   * @param type the expected type of the object
   * @param in the source of the bytes
   * @param <T> the type of the object
   * @return the object
   * @throws IOException any thrown by {@code in}
   * @throws Exception the the value could not be deserialized
   */
  <T> T deserialize(Class<T> type, InputStream in) throws Exception;

}
