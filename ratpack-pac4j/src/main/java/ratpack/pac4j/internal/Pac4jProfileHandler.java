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

package ratpack.pac4j.internal;

import org.pac4j.core.profile.UserProfile;

import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.session.store.SessionStorage;

import static ratpack.pac4j.internal.SessionConstants.USER_PROFILE;

/**
 * Retrieve the current pac4j user profile stored in session
 * and push it into the request.
 */
public class Pac4jProfileHandler implements Handler {

  @Override
  public void handle(final Context context) throws Exception {
    UserProfile userProfile = getUserProfile(context);
    
    if (userProfile != null) {
      context.getRequest().register(userProfile);
      context.getRequest().register(UserProfile.class, userProfile);
    }
    
    context.next();
  }

  protected void removeUserProfile(final Context context) {
    final SessionStorage sessionStorage = context.getRequest().get(SessionStorage.class);
    sessionStorage.remove(SessionConstants.USER_PROFILE);
  }

  protected UserProfile getUserProfile(final Context context) {
    return (UserProfile) context.getRequest().get(SessionStorage.class).get(USER_PROFILE);
  }
}
