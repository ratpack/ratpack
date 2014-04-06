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

import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.pac4j.Authorizer;
import ratpack.server.PublicAddress;
import ratpack.session.store.SessionStorage;

import java.util.concurrent.Callable;

import static ratpack.pac4j.internal.SessionConstants.SAVED_URI;
import static ratpack.pac4j.internal.SessionConstants.USER_PROFILE;

/**
 * Filters requests to apply authentication and authorization as required.
 *
 * @param <C> The {@link org.pac4j.core.credentials.Credentials} type
 * @param <U> The {@link org.pac4j.core.profile.UserProfile} type
 */
public class Pac4jAuthenticationHandler<C extends Credentials, U extends UserProfile> implements Handler {
  private final Client<C, U> client;
  private final Authorizer<U> authorizer;
  private final String callbackPath;

  /**
   * Constructs a new instance.
   *
   * @param client The client to use for authentication
   * @param authorizer The authorizer to user for authorization
   * @param callbackPath the path to use for callbacks from the identity provider
   */
  public Pac4jAuthenticationHandler(Client<C, U> client, Authorizer<U> authorizer, String callbackPath) {
    this.client = client;
    this.authorizer = authorizer;
    this.callbackPath = callbackPath;
  }

  @Override
  public void handle(final Context context) throws Exception {
    U userProfile = getUserProfile(context);
    if (authorizer.isAuthenticationRequired(context) && userProfile == null) {
      initiateAuthentication(context);
    } else {
      if (userProfile != null) {
        context.getRequest().register(userProfile);
        authorizer.handleAuthorization(context, userProfile);
      } else {
        context.next();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private U getUserProfile(Context context) {
    return (U) context.getRequest().get(SessionStorage.class).get(USER_PROFILE);
  }

  private void initiateAuthentication(final Context context) throws Exception {
    context.background(new Callable<String>() {
      @Override
      public String call() {
        if (client instanceof BaseClient) {
          PublicAddress publicAddress = context.get(PublicAddress.class);
          String callbackUrl = publicAddress.getAddress(context).toString() + "/" + callbackPath;
          ((BaseClient) client).setCallbackUrl(callbackUrl);
        }
        return client.getRedirectionUrl(new RatpackWebContext(context));
      }
    }).then(new Action<String>() {
      @Override
      public void execute(String redirectionUrl) {
        SessionStorage sessionStorage = context.getRequest().get(SessionStorage.class);
        sessionStorage.put(SAVED_URI, context.getRequest().getUri());
        context.redirect(redirectionUrl);
      }
    });
  }
}
