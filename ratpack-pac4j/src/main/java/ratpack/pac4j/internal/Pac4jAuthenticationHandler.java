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
import org.pac4j.core.client.Clients;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Context;
import ratpack.http.Request;
import ratpack.pac4j.Authorizer;
import ratpack.pac4j.Pac4jCallbackHandlerBuilder;
import ratpack.session.Session;

/**
 * Filters requests to apply authentication and authorization as required.
 */
public class Pac4jAuthenticationHandler extends Pac4jProfileHandler {
  private final String name;
  private final Authorizer authorizer;

  /**
   * Constructs a new instance.
   *
   * @param name The name of the client to use for authentication
   * @param authorizer The authorizer to user for authorization
   */
  public Pac4jAuthenticationHandler(String name, Authorizer authorizer) {
    this.name = name;
    this.authorizer = authorizer;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    getUserProfile(ctx).then(userProfile -> {
      if (authorizer.isAuthenticationRequired(ctx) && !userProfile.isPresent()) {
        initiateAuthentication(ctx);
      } else {
        if (userProfile.isPresent()) {
          UserProfile user = userProfile.get();
          registerUserProfile(ctx, user);
          authorizer.handleAuthorization(ctx, user);
        } else {
          ctx.next();
        }
      }
    });
  }

  private void initiateAuthentication(Context ctx) {
    Request request = ctx.getRequest();
    Clients clients = request.get(Clients.class);
    Client<?, ?> client = clients.findClient(name);

    ctx.get(Session.class).getData().then(session -> {
      RatpackWebContext webContext = new RatpackWebContext(ctx, session);
      session.set(Pac4jCallbackHandlerBuilder.REQUESTED_URL_SESSION_KEY, request.getUri());

      try {
        client.redirect(webContext, true, request.isAjaxRequest());
      } catch (Exception e) {
        if (e instanceof RequiresHttpAction) {
          webContext.sendResponse((RequiresHttpAction) e);
          return;
        } else {
          throw new TechnicalException("Failed to redirect", e);
        }
      }

      webContext.sendResponse();
    });
  }

}
