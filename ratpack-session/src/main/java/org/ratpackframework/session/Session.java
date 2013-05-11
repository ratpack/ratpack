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

package org.ratpackframework.session;

public interface Session  {

  /**
   * If there is an existing session, returns the ID in use.
   *
   * This will not initiate a session if there is none.
   *
   * @return Any existing session id.
   */
  String getExistingId();

  /**
   * Returns the session ID, initiating a session if necessary.
   *
   * @return The session id.
   */
  String getId();

  /**
   * Initiates a new session, terminating the
   *
   * Can only be called once per request, and not if getId() has already initiated a new session.
   */
  String regen();

  /**
   * Terminates the session with the client.
   *
   * Cannot be called during the same request that initiates a session.
   */
  void terminate();

}
