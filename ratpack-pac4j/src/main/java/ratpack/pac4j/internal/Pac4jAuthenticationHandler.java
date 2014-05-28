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

import org.pac4j.core.client.Clients;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;

import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.pac4j.Authorizer;
import ratpack.session.store.SessionStorage;

import java.util.concurrent.Callable;

import static ratpack.pac4j.internal.SessionConstants.SAVED_URI;

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
  public void handle(final Context context) throws Exception {
    UserProfile userProfile = getUserProfile(context);
    if (authorizer.isAuthenticationRequired(context) && userProfile == null) {
      initiateAuthentication(context);
    } else {
      if (userProfile != null) {
        registerUserProfile(context, userProfile);
        authorizer.handleAuthorization(context, userProfile);
      } else {
        context.next();
      }
    }
  }

  private void initiateAuthentication(final Context context) {
    context.getRequest().get(SessionStorage.class).put(SAVED_URI, context.getRequest().getUri());
    final Clients clients = context.get(Clients.class);
    final RatpackWebContext webContext = new RatpackWebContext(context);
    context.blocking(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        clients.findClient(name).redirect(webContext, true, false);
        return null;
      }
    }).onError(new Action<Throwable>() {
      @Override
      public void execute(Throwable ex) throws Exception {
        if (ex instanceof RequiresHttpAction) {
          webContext.sendResponse((RequiresHttpAction) ex);
        } else {
          throw new TechnicalException("Failed to redirect", ex);
        }
      }
    }).then(new Action<Void>() {
      @Override
      public void execute(Void ignored) throws Exception {
        webContext.sendResponse();
      }
    });
  }
}
