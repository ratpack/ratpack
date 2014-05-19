/*
 * Copyright 2014 the original author or authors.
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

package ratpack.pac4j;

import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Context;

/**
 * An authorization strategy for integration with pac4j.
 * <p>
 * In particular, this allows control over which requests require authentication, the mechanism for performing access control, and the handling of authentication failures.
 * </p>
 *
 * @see ratpack.pac4j.AbstractAuthorizer
 */
public interface Authorizer {
  /**
   * Determines whether authentication is required for a given context.
   * <p>
   * If this method returns {@code true}, unauthenticated users are redirected to the identity provider.
   * </p>
   *
   * @param context The context to handle
   * @return Whether authentication is required for the context
   */
  boolean isAuthenticationRequired(Context context);

  /**
   * Handles authorization for the given context.
   * <p>
   * This method is only called for requests that require authentication.
   * If the request is properly authorized, it's sufficient to call {@link ratpack.handling.Context#next()}.
   * If the request is not properly authorized, it is this method's responsibility to either return an appropriate error response, or redirect to an error page.
   * </p>
   *
   * @param context The context to handle
   * @param userProfile The authenticated user profile
   * @throws Exception if anything goes wrong (exception will be implicitly passed to the context's {@link Context#error(Exception)} method)
   * @see #isAuthenticationRequired(ratpack.handling.Context)
   */
  void handleAuthorization(Context context, UserProfile userProfile) throws Exception;
}
