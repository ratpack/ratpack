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
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.pac4j.Pac4jCallbackHandlerBuilder;
import ratpack.session.Session;

import java.util.Optional;

/**
 * Retrieve the current pac4j user profile stored in session
 * and push it into the request.
 */
public class Pac4jProfileHandler implements Handler {

  @Override
  public void handle(Context ctx) throws Exception {
    getUserProfile(ctx).then(userProfile -> {
      userProfile.ifPresent(uP -> registerUserProfile(ctx, uP));
      ctx.next();
    });
  }

  protected void registerUserProfile(Context ctx, UserProfile userProfile) {
    ctx.getRequest().add(userProfile).add(UserProfile.class, userProfile);
  }

  protected Promise<Optional<UserProfile>> getUserProfile(Context context) {
    return context.get(Session.class)
      .getData()
      .map(d -> d.get(Pac4jCallbackHandlerBuilder.USER_PROFILE_SESSION_KEY));
  }
}
