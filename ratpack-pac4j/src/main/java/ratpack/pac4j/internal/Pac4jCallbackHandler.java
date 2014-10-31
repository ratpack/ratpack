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
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Pac4jCallbackHandler implements Handler {

  private final BiFunction<? super Context, ? super WebContext, ? extends Client<Credentials, UserProfile>> lookupClient;
  private final BiConsumer<? super Context, ? super UserProfile> onSuccess;
  private final BiConsumer<? super Context, ? super Throwable> onError;

  public Pac4jCallbackHandler(
    BiFunction<? super Context, ? super WebContext, ? extends Client<Credentials, UserProfile>> lookupClient,
    BiConsumer<? super Context, ? super UserProfile> onSuccess,
    BiConsumer<? super Context, ? super Throwable> onError
  ) {
    this.lookupClient = lookupClient;
    this.onSuccess = onSuccess;
    this.onError = onError;
  }

  @Override
  public void handle(Context context) {
    RatpackWebContext webContext = new RatpackWebContext(context);
    context.blocking(() -> {
      Client<Credentials, UserProfile> client = lookupClient.apply(context, webContext);
      Credentials credentials = client.getCredentials(webContext);
      return client.getUserProfile(credentials, webContext);
    }).onError(e -> {
      if (e instanceof RequiresHttpAction) {
        webContext.sendResponse((RequiresHttpAction) e);
      } else {
        onError.accept(context, e);
      }
    }).then(profile -> onSuccess.accept(context, profile));
  }

}
