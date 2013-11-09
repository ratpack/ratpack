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

package ratpack.session.store;

/**
 * A store of sessions.
 * <p>
 * Implementations may impose their own behavior on storage.
 * For example: maximum concurrent session limits, idle timeouts etc.
 */
public interface SessionStore {

  /**
   * Retrieve the session storage for the given id, creating it on demand if necessary.
   *
   * @param sessionId The id of the session to retrieve the storage for
   * @return The session storage
   */
  SessionStorage get(String sessionId);

  /**
   * The number of currently stored sessions.
   *
   * @return The number of currently stored sessions
   */
  long size();
}
