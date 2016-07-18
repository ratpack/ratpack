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
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
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
    PathBinding pathBinding = ctx.getPathBinding();
    String pastBinding = pathBinding.getPastBinding();

    if (pastBinding.equals(path)) {
      RatpackWebContext.from(ctx, true).flatMap(webContext -> {
        SessionData sessionData = webContext.getSession();
        return createClients(ctx, pathBinding).map(clients ->
          clients.findClient(webContext)
        ).map(
          Types::<Client<Credentials, UserProfile>>cast
        ).flatMap(client ->
          getProfile(webContext, client)
        ).map(profile -> {
          if (profile != null) {
            sessionData.set(Pac4jSessionKeys.USER_PROFILE, profile);
          }
          Optional<String> originalUrl = sessionData.get(Pac4jSessionKeys.REQUESTED_URL);
          sessionData.remove(Pac4jSessionKeys.REQUESTED_URL);
          return originalUrl;
        }).onError(t -> {
          if (t instanceof RequiresHttpAction) {
            webContext.sendResponse((RequiresHttpAction) t);
          } else {
            ctx.error(new TechnicalException("Failed to get user profile", t));
          }
        });
      }).then(originalUrlOption -> {
        ctx.redirect(originalUrlOption.orElse("/"));
      });
    } else {
      createClients(ctx, pathBinding).then(clients -> {
        Registry registry = Registry.singleLazy(Clients.class, () -> uncheck(() -> clients));
        ctx.next(registry);
      });
    }
  }

  private Promise<Clients> createClients(Context ctx, PathBinding pathBinding) throws Exception {
    String boundTo = pathBinding.getBoundTo();
    PublicAddress publicAddress = ctx.get(PublicAddress.class);
    String absoluteCallbackUrl = publicAddress.get(b -> b.maybeEncodedPath(boundTo).maybeEncodedPath(path)).toASCIIString();

    Iterable<? extends Client<?, ?>> result = clientsProvider.get(ctx);

    @SuppressWarnings("rawtypes")
    List<Client> clients;
    if (result instanceof List) {
      clients = Types.cast(result);
    } else {
      clients = ImmutableList.copyOf(result);
    }

    return Promise.value(new Clients(absoluteCallbackUrl, clients));
  }

  private <C extends Credentials, U extends UserProfile> Promise<U> getProfile(WebContext webContext, Client<C, U> client) throws RequiresHttpAction {
    return Blocking.get(() -> {
      C credentials = client.getCredentials(webContext);
      return client.getUserProfile(credentials, webContext);
    });
  }

}
