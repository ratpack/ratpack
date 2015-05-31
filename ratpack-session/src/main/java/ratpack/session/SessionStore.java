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

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.server.Service;

/**
 * A persistent store of session data.
 * <p>
 * Ratpack's session support cooperates with the implementation of this type found in the context registry.
 * The {@link SessionModule} provides a default implementation that stores the data in local memory.
 * In order to persist session data in the store of your choice, simply override the binding for this type with your own implementation.
 * <p>
 * The store methods return {@link Promise} and {@link Operation} in order to support non blocking IO.
 * <p>
 * The store should not make any attempt to interpret the bytes that it is storing/loading.
 */
public interface SessionStore extends Service {

  /**
   * Writes the session data for the given id.
   * <p>
   * The given byte buffer will not be modified by the caller, and will be released by the caller.
   *
   * @param sessionId the identifier for the session
   * @param sessionData the session data
   * @return the store operation
   */
  Operation store(AsciiString sessionId, ByteBuf sessionData);

  /**
   * Reads the session data for the given id.
   * <p>
   * The caller will release the promised byte buffer.
   *
   * @param sessionId the identifier for the session
   * @return a promise for the session data
   */
  Promise<ByteBuf> load(AsciiString sessionId);

  /**
   * Removes the session data for the given id.
   *
   * @param sessionId the session id
   * @return the remove operation
   */
  Operation remove(AsciiString sessionId);

  /**
   * The current number of sessions.
   * <p>
   * The exact meaning of this value is implementation dependent.
   * {@code -1} may be returned if the store does not support getting the size.
   *
   * @return a promise for the store size
   */
  Promise<Long> size();
}
