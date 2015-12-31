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

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static ratpack.util.Exceptions.uncheck;

public class Pac4jAuthenticator implements Handler {

  private final String path;
  private final RatpackPac4j.ClientsProvider clientsProvider;

  public Pac4jAuthenticator(String path, RatpackPac4j.ClientsProvider clientsProvider) {
    this.path = path;
    this.clientsProvider = clientsProvider;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    PathBinding pathBinding = ctx.get(PathBinding.TYPE);
    String pastBinding = pathBinding.getPastBinding();

    if (pastBinding.equals(path)) {
      RatpackPac4j.webContext(ctx).map(Types::<RatpackWebContext>cast).then(webContext -> {
        SessionData sessionData = webContext.getSession();
        try {
          Clients clients = createClients(ctx, pathBinding);
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
      Registry registry = Registry.singleLazy(Clients.class, () -> uncheck(() -> createClients(ctx, pathBinding)));
      ctx.next(registry);
    }
  }

  public Clients createClients(Context ctx, PathBinding pathBinding) throws Exception {
    String callback = createCallbackUrl(ctx.get(PublicAddress.class), pathBinding, path);

    return new Clients(callback, getClientsFromProvider(ctx, clientsProvider));
  }

  private String createCallbackUrl(PublicAddress publicAddress, PathBinding pathBinding, String path) {
    String boundTo = pathBinding.getBoundTo();
    URI address = publicAddress.get();

    if(!boundTo.isEmpty()) {
      if (!(address.toString().endsWith("/") || boundTo.startsWith("/"))) {
        boundTo = "/" + boundTo;
      }
    }

    return address + boundTo + "/" + path;
  }

  @SuppressWarnings("rawtypes")
  private List<Client> getClientsFromProvider(Context ctx, RatpackPac4j.ClientsProvider clientsProvider) {
    Iterable<? extends Client<?, ?>> result = clientsProvider.get(ctx);

    List<Client> clients;
    if (result instanceof List) {
      clients = Types.cast(result);
    } else {
      clients = ImmutableList.copyOf(result);
    }

    return clients;
  }

  private <C extends Credentials, U extends UserProfile> UserProfile getProfile(WebContext webContext, Client<C, U> client) throws RequiresHttpAction {
    C credentials = client.getCredentials(webContext);
    return client.getUserProfile(credentials, webContext);
  }

}
