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

import io.netty.util.AsciiString;

/**
 * A handle to the session ID.
 * <p>
 * It is generally not necessary to use this type directly.
 * It is used by the provided impl of {@link Session} to discover (and generate) the session ID.
 * <p>
 * The {@link SessionModule} provides a default implementation of this type that uses {@link SessionIdGenerator},
 * and a cookie, based on {@link SessionCookieConfig} to store the session id.
 */
public interface SessionId {

  /**
   * Get the session ID value, generating a new one if necessary.
   *
   * @return the session id value
   */
  AsciiString getValue();

  /**
   * Terminate the current session id, disassociating it from the current user.
   */
  void terminate();

}
