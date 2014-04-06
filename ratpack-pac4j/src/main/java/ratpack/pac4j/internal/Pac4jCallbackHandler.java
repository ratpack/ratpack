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

import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.pac4j.Authorizer;
import ratpack.session.store.SessionStorage;

import java.util.concurrent.Callable;

import static ratpack.pac4j.internal.SessionConstants.SAVED_URI;
import static ratpack.pac4j.internal.SessionConstants.USER_PROFILE;

/**
 * Handles callback requests from identity providers.
 *
 * @param <C> The {@link org.pac4j.core.credentials.Credentials} type
 * @param <U> The {@link org.pac4j.core.profile.UserProfile} type
 */
public class Pac4jCallbackHandler<C extends Credentials, U extends UserProfile> implements Handler {
  private static final String DEFAULT_REDIRECT_URI = "/";

  private final Client<C, U> client;
  private final Authorizer<U> authorizer;

  /**
   * Constructs a new instance.
   *
   * @param client The client to use for authentication
   * @param authorizer The authorizer to user for authorization
   */
  public Pac4jCallbackHandler(Client<C, U> client, Authorizer<U> authorizer) {
    this.client = client;
    this.authorizer = authorizer;
  }

  @Override
  public void handle(final Context context) throws Exception {
    context.background(new Callable<UserProfile>() {
      @Override
      public UserProfile call() throws RequiresHttpAction {
        return client.getUserProfile(client.getCredentials(new RatpackWebContext(context)));
      }
    }).onError(new Action<Throwable>() {
      @Override
      public void execute(Throwable error) {
        if (error instanceof RequiresHttpAction) {
          context.getResponse().send();
        } else {
          throw new TechnicalException("Failed to get user profile", error);
        }
      }
    }).then(new Action<UserProfile>() {
      @Override
      public void execute(UserProfile profile) {
        SessionStorage sessionStorage = context.getRequest().get(SessionStorage.class);
        saveUserProfileInSession(sessionStorage, profile);
        if (profile == null) {
          authorizer.handleAuthenticationFailure(context);
        } else {
          context.redirect(getSavedUri(sessionStorage));
        }
      }
    });
  }

  private static void saveUserProfileInSession(SessionStorage sessionStorage, UserProfile profile) {
    if (profile != null) {
      sessionStorage.put(USER_PROFILE, profile);
    } else {
      sessionStorage.remove(USER_PROFILE);
    }
  }

  private static String getSavedUri(SessionStorage sessionStorage) {
    String originalUri = (String) sessionStorage.remove(SAVED_URI);
    if (originalUri == null) {
      originalUri = DEFAULT_REDIRECT_URI;
    }
    return originalUri;
  }
}
