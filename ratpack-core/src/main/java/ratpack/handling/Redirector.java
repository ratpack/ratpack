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

package ratpack.handling;

import ratpack.api.NonBlocking;

/**
 * Contextual strategy for issuing redirects.
 * <p>
 * Ratpack provides a default contextual implementation that uses the {@link ratpack.server.ServerConfig#getPublicAddress()} to
 * make any relative redirect locations absolute in terms of the public address.
 * <p>
 * This strategy is typically sufficient and a user implementation of this type is not required.
 */
public interface Redirector {

  /**
   * Issue a redirect to the client.
   *
   * @param context The context to issue the redirect for
   * @param location The user given location value (i.e. the {@code location} arg to {@link Context#redirect(int, String)})
   * @param code The http code to issue with the redirect
   */
  @NonBlocking
  void redirect(Context context, String location, int code);
}
