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

import com.google.common.collect.ImmutableList;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.path.PathBinding;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.server.PublicAddress;
import ratpack.session.Session;
import ratpack.util.Types;

import java.util.List;
import java.util.Optional;

public class Pac4jCallbackHandler implements Handler {

  private final String path;
  private final ImmutableList<Client<?, ?>> clients;

  public Pac4jCallbackHandler(String path, List<Client<?, ?>> clients) {
    this.path = path;
    this.clients = ImmutableList.copyOf(clients);
  }

  @Override
  public void handle(Context ctx) throws Exception {
    PathBinding pathBinding = ctx.get(PathBinding.class);
    String boundTo = pathBinding.getBoundTo();
    String pastBinding = pathBinding.getPastBinding();
    PublicAddress publicAddress = ctx.get(PublicAddress.class);
    String absoluteCallbackUrl = publicAddress.getAddress(ctx) + boundTo + "/" + path;

    @SuppressWarnings("rawtypes")
    List<Client> cast = Types.cast(this.clients);
    Clients clients = new Clients(absoluteCallbackUrl, cast);

    if (pastBinding.equals(path)) {
      ctx.get(Session.class).getData().then(sessionData -> {
        RatpackWebContext webContext = new RatpackWebContext(ctx, sessionData);
        try {
          Client<?, ?> client = clients.findClient(webContext);
          UserProfile profile = getProfile(webContext, client);
          if (profile != null) {
            sessionData.set(Pac4jSessionKeys.USER_PROFILE_SESSION_KEY, profile);
          }
          Optional<String> originalUrl = sessionData.get(Pac4jSessionKeys.REQUESTED_URL_SESSION_KEY);
          sessionData.remove(Pac4jSessionKeys.REQUESTED_URL_SESSION_KEY);
          ctx.redirect(originalUrl.orElse("/"));
        } catch (Exception e) {
          if (e instanceof RequiresHttpAction) {
            webContext.sendResponse((RequiresHttpAction) e);
          } else {
            ctx.error(new TechnicalException("Failed to get user profile", e));
          }
        }
      });
    } else {
      Registry registry = Registries.just(Clients.class, clients);
      ctx.next(registry);
    }
  }

  private <C extends Credentials, U extends UserProfile> UserProfile getProfile(WebContext webContext, Client<C, U> client) throws RequiresHttpAction {
    C credentials = client.getCredentials(webContext);
    return client.getUserProfile(credentials, webContext);
  }

}
