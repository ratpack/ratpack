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
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.registry.Registries;
import ratpack.session.Session;

import java.util.Optional;

public class Pac4jAuthenticationHandler implements Handler {

  private final Class<? extends Client<?, ?>> clientType;

  public Pac4jAuthenticationHandler(Class<? extends Client<?, ?>> clientType) {
    this.clientType = clientType;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    getUserProfile(ctx).then(userProfile -> {
      if (userProfile.isPresent()) {
        ctx.next(Registries.just(userProfile.get()));
      } else {
        initiateAuthentication(ctx);
      }
    });
  }

  protected Promise<Optional<UserProfile>> getUserProfile(Context context) {
    return context.get(Session.class)
      .getData()
      .map(d -> d.get(Pac4jSessionKeys.USER_PROFILE_SESSION_KEY));
  }

  private void initiateAuthentication(Context ctx) {
    Request request = ctx.getRequest();
    Clients clients = ctx.get(Clients.class);
    Client<?, ?> client = clients.findClient(clientType);

    ctx.get(Session.class).getData().then(session -> {
      RatpackWebContext webContext = new RatpackWebContext(ctx, session);
      session.set(Pac4jSessionKeys.REQUESTED_URL_SESSION_KEY, request.getUri());

      try {
        client.redirect(webContext, true, request.isAjaxRequest());
      } catch (Exception e) {
        if (e instanceof RequiresHttpAction) {
          webContext.sendResponse((RequiresHttpAction) e);
          return;
        } else {
          ctx.error(new TechnicalException("Failed to redirect", e));
        }
      }

      webContext.sendResponse();
    });
  }

}
