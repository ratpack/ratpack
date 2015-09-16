/*
 * Copyright 2013 the original author or authors.
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

import java.util.UUID;

/**
 * Strategy interface for generating unique session ids.
 * <p>
 * The {@link SessionModule} provides a default implementation based on {@link UUID}.
 */
public interface SessionIdGenerator {

  /**
   * Generates a new identifier to be used as a session id.
   *
   * @return a new session id
   */
  AsciiString generateSessionId();

}
