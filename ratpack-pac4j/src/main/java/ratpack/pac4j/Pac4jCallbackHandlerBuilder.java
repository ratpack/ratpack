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

package ratpack.pac4j;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.pac4j.internal.Pac4jCallbackHandler;
import ratpack.session.store.SessionStorage;
import ratpack.util.Types;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static ratpack.pac4j.internal.SessionConstants.SAVED_URI;
import static ratpack.pac4j.internal.SessionConstants.USER_PROFILE;

public class Pac4jCallbackHandlerBuilder {

  /*
    TODO

    1. Class level javadoc showing why you might use this and how you would insert the generated handler into the app
    2. Method level javadoc for each of the methods explaining what the extension point allows, and what the default behaviour is
    3. Class level javadoc explaining the high level flow of the handler, pointing at the methods that allow those to be overridden

    MAYBE

    1. Reconsider the name
    2. Should this be parameterised for the Credentials and UserProfile types?

   */
  private static final String DEFAULT_REDIRECT_URI = "/";

  public Handler build() {
    return new Pac4jCallbackHandler(lookupClient, onSuccess, onError);
  }

  @SuppressWarnings("unused")
  public Pac4jCallbackHandlerBuilder lookupClient(BiFunction<? super Context, ? super WebContext, ? extends Client<Credentials, UserProfile>> lookupClient) {
    this.lookupClient = lookupClient;
    return this;
  }

  private BiFunction<? super Context, ? super WebContext, ? extends Client<Credentials, UserProfile>> lookupClient = (context, webContext) -> {
    Request request = context.getRequest();
    Clients clients = request.get(Clients.class);
    return Types.cast(clients.findClient(webContext));
  };

  @SuppressWarnings("unused")
  public Pac4jCallbackHandlerBuilder onSuccess(BiConsumer<? super Context, ? super UserProfile> onSuccess) {
    this.onSuccess = onSuccess;
    return this;
  }

  private BiConsumer<? super Context, ? super UserProfile> onSuccess = (context, profile) -> {
    Request request = context.getRequest();
    SessionStorage sessionStorage = request.get(SessionStorage.class);
    if (profile != null) {
      sessionStorage.set(USER_PROFILE, profile).then((success) -> {
        //TODO Log?
      });
    }
    sessionStorage.get(SAVED_URI, String.class).then((originalUri) -> {
        sessionStorage.remove(SAVED_URI).then((numberRemoved) -> {
          context.redirect(originalUri.orElse(DEFAULT_REDIRECT_URI));
        });
      }
    );

  };

  @SuppressWarnings("unused")
  public Pac4jCallbackHandlerBuilder onError(BiConsumer<? super Context, ? super Throwable> onError) {
    this.onError = onError;
    return this;
  }

  private BiConsumer<? super Context, ? super Throwable> onError = (ctx, ex) -> {
    throw new TechnicalException("Failed to get user profile", ex);
  };

}
