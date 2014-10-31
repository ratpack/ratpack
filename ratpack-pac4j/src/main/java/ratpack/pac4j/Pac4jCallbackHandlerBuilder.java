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
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Context;
import ratpack.pac4j.internal.Pac4jCallbackHandler;
import ratpack.pac4j.internal.RatpackWebContext;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Builder for Pac4J callback handler.
 */
public class Pac4jCallbackHandlerBuilder {

  private BiFunction<Context, RatpackWebContext, Client<Credentials, UserProfile>> findClientFunction;
  private BiConsumer<Context, UserProfile> handleProfileFunction;
  private BiConsumer<Context, Throwable> handleErrorFunction;

  public Pac4jCallbackHandler build() {
    return new Pac4jCallbackHandler(findClientFunction, handleProfileFunction, handleErrorFunction);
  }

  /**
   * Add function for looking up client given the context.
   *
   * <p>e.g.</p>
   * <pre>
   * <code>
   * builder.findClient(((context, ratpackContext) -&gt; context.getRequest().get(Clients.class).findClient(context.getPathTokens().get("clientName"))
   * </code>
   * </pre>
   *
   * @param findClientFunction the lookup function
   * @return the builder
   */
  @SuppressWarnings("unused")
  public Pac4jCallbackHandlerBuilder findClient(BiFunction<Context, RatpackWebContext, Client<Credentials, UserProfile>> findClientFunction) {
    this.findClientFunction = findClientFunction;
    return this;
  }

  /**
   * Add handler for dealing with the newly-looked-up profile
   *
   * <p>e.g.</p>
   * <pre>
   * <code>
   * builder.handleProfile(((context, profile) -&gt; context.next(Registries.just(profile))));
   * </code>
   * </pre>
   *
   * @param handleProfileFunction the handler function
   * @return the builder
   */
  @SuppressWarnings("unused")
  public Pac4jCallbackHandlerBuilder handleProfile(BiConsumer<Context, UserProfile> handleProfileFunction) {
    this.handleProfileFunction = handleProfileFunction;
    return this;
  }

  /**
   * Add handler for dealing with errors
   *
   * <p>e.g.</p>
   * <pre>
   * <code>
   * this.handleError((context, ex) -&gt; {
   *     LOGGER.error("Error logging in", ex);
   *     context.redirect("/some/url");
   * });
   * </code>
   * </pre>
   *
   * @param handleErrorFunction the handler function
   * @return the builder
   */
  @SuppressWarnings("unused")
  public Pac4jCallbackHandlerBuilder handleError(BiConsumer<Context, Throwable> handleErrorFunction) {
    this.handleErrorFunction = handleErrorFunction;
    return this;
  }

}
