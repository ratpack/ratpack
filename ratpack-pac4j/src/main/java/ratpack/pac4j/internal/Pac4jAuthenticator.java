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
import ratpack.pac4j.RatpackPac4j;
import ratpack.path.PathBinding;
import ratpack.registry.Registry;
import ratpack.server.PublicAddress;
import ratpack.session.SessionData;
import ratpack.util.Types;

import java.util.List;
import java.util.Optional;

public class Pac4jAuthenticator implements Handler {

  private final String path;
  private final ImmutableList<Client<?, ?>> clients;

  public Pac4jAuthenticator(String path, List<Client<?, ?>> clients) {
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

      RatpackPac4j.webContext(ctx).then(webContext -> {
        SessionData sessionData = webContext.getSession();
        try {
          Client<?, ?> client = clients.findClient(webContext);
          UserProfile profile = getProfile(webContext, client);
          if (profile != null) {
            sessionData.set(Pac4jSessionKeys.USER_PROFILE, profile);
          }
          Optional<String> originalUrl = sessionData.get(Pac4jSessionKeys.REQUESTED_URL);
          sessionData.remove(Pac4jSessionKeys.REQUESTED_URL);
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
      Registry registry = Registry.single(Clients.class, clients);
      ctx.next(registry);
    }
  }

  private <C extends Credentials, U extends UserProfile> UserProfile getProfile(WebContext webContext, Client<C, U> client) throws RequiresHttpAction {
    C credentials = client.getCredentials(webContext);
    return client.getUserProfile(credentials, webContext);
  }

}
