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
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.session.store.SessionStorage;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static ratpack.pac4j.internal.SessionConstants.SAVED_URI;
import static ratpack.pac4j.internal.SessionConstants.USER_PROFILE;

/**
 * Handles callback requests from identity providers.
 */
public class Pac4jCallbackHandler implements Handler {
  private static final String DEFAULT_REDIRECT_URI = "/";

  private final BiFunction<Context, RatpackWebContext, Client<Credentials, UserProfile>> findClientFunction;
  private final BiConsumer<Context, UserProfile> handleProfileFunction;
  private final BiConsumer<Context, Throwable> handleErrorFunction;

  @SuppressWarnings("unused")
  public Pac4jCallbackHandler() {
    this(null, null, null);
  }

  public Pac4jCallbackHandler(BiFunction<Context, RatpackWebContext, Client<Credentials, UserProfile>> findClientFunction, BiConsumer<Context, UserProfile> handleProfileFunction, BiConsumer<Context, Throwable> handleErrorFunction) {
    this.findClientFunction = findClientFunction != null ? findClientFunction : Pac4jCallbackHandler::defaultFindClient;
    this.handleProfileFunction = handleProfileFunction != null ? handleProfileFunction : Pac4jCallbackHandler::defaultHandleProfile;
    this.handleErrorFunction = handleErrorFunction != null ? handleErrorFunction : Pac4jCallbackHandler::defaultHandleError;
  }

  @Override
  public void handle(final Context context) {
    final RatpackWebContext webContext = new RatpackWebContext(context);
    context.blocking(() -> {
      @SuppressWarnings("unchecked")
      Client<Credentials, UserProfile> client = findClientFunction.apply(context, webContext);
      Credentials credentials = client.getCredentials(webContext);
      return client.getUserProfile(credentials, webContext);
    }).onError(ex -> {
      if (ex instanceof RequiresHttpAction) {
        webContext.sendResponse((RequiresHttpAction) ex);
      } else {
        handleErrorFunction.accept(context, ex);
      }
    }).then(profile -> handleProfileFunction.accept(context, profile));
  }

  private static Client<Credentials, UserProfile> defaultFindClient(Context context, WebContext webContext) {
    Request request = context.getRequest();
    Clients clients = request.get(Clients.class);
    @SuppressWarnings("unchecked")
    Client<Credentials, UserProfile> client = clients.findClient(webContext);
    return client;
  }

  private static void defaultHandleProfile(Context context, UserProfile profile) {
    Request request = context.getRequest();
    SessionStorage sessionStorage = request.get(SessionStorage.class);
    saveUserProfileInSession(sessionStorage, profile);
    context.redirect(getSavedUri(sessionStorage));
  }

  private static void defaultHandleError(Context context, Throwable ex) {
    throw new TechnicalException("Failed to get user profile", ex);
  }

  private static void saveUserProfileInSession(SessionStorage sessionStorage, UserProfile profile) {
    if (profile != null) {
      sessionStorage.put(USER_PROFILE, profile);
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
