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
 * An abstract implementation of {@link ratpack.pac4j.Authorizer} that provides sensible defaults.
 * <p>
 * Unless overridden, all authenticated requests will be considered authorized and authentication failures will be handled with the default client error handling for status {@code 403 Forbidden}.
 * </p>
 */
public abstract class AbstractAuthorizer implements Authorizer {
  /**
   * Unless overridden, all authenticated requests will be considered authorized.
   *
   * @param context The context to handle
   * @param userProfile The authenticated user profile
   * @throws Exception if anything goes wrong (exception will be implicitly passed to the context's {@link Context#error(Throwable)} method)
   */
  @Override
  public void handleAuthorization(Context context, UserProfile userProfile) throws Exception {
    context.next();
  }
}
