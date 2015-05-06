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

package ratpack.session.clientside;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Serializes values of cookie session entries.
 * <p>
 * Session entries are defined in the form of key and value pairs.
 * Keys are of type {@code String} while values could be any type.
 */
public interface ValueSerializer {

  /**
   * Serializes cookie entry {@code value} to byte buffer.
   *
   * @see ratpack.session.clientside.SessionService
   *
   * @param bufAllocator byte buffer allocator
   * @param value a value of cookie attribute to serialize
   *
   * @return the byte array of serialized value of session entry
   * @throws Exception the exception
   */
  ByteBuf serialize(ByteBufAllocator bufAllocator, Object value) throws Exception;

  /**
   * Deserializes cookie entry {@code value} from string to typed object.
   * <p>
   * All cookie values are put into the cookie as encoded
   *
   * @param value a value to deserialize fo target {@code V} type
   *
   * @return the deserialized value
   * @throws Exception the exception
   */
  Object deserialize(String value) throws Exception;
}
