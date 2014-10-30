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

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;

import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.launch.LaunchConfig;
import ratpack.pac4j.Authorizer;
import ratpack.pac4j.Pac4jCallbackHandlerBuilder;

/**
 * Base class for pac4j integration modules.
 *
 * @param <C> The {@link org.pac4j.core.credentials.Credentials} type
 * @param <U> The {@link org.pac4j.core.profile.UserProfile} type
 */
public abstract class AbstractPac4jModule<C extends Credentials, U extends UserProfile> extends AbstractModule implements HandlerDecoratingModule {
  private static final String DEFAULT_CALLBACK_PATH = "pac4j-callback";

  private String callbackPath;

  /**
   * Sets the path to use for callbacks from the identity provider.
   *
   * @param callbackPath The callback path
   * @return This module, for call chaining
   */
  public AbstractPac4jModule<C, U> callbackPath(String callbackPath) {
    this.callbackPath = callbackPath;
    return this;
  }

  @Override
  protected void configure() {
  }

  /**
   * Returns the path to use for callbacks from the identity provider.
   *
   * @param injector The injector created from all the application modules
   * @return The callback path
   */
  private String getCallbackPath(Injector injector) {
    LaunchConfig launchConfig = injector.getInstance(LaunchConfig.class);
    return callbackPath == null ? launchConfig.getOther("pac4j.callbackPath", DEFAULT_CALLBACK_PATH) : callbackPath;
  }

  /**
   * Returns the client to use for authentication.
   *
   * @param injector The injector created from all the application modules
   * @return The client
   */
  protected abstract Client<C, U> getClient(Injector injector);

  /**
   * Returns the authorizer to use for authorization.
   *
   * @param injector The injector created from all the application modules
   * @return The authorizer
   */
  protected abstract Authorizer getAuthorizer(Injector injector);

  @Override
  public Handler decorate(Injector injector, Handler handler) {
    final String callbackPath = getCallbackPath(injector);
    final Client<C, U> client = getClient(injector);
    final Authorizer authorizer = getAuthorizer(injector);
    final Pac4jClientsHandler clientsHandler = new Pac4jClientsHandler(callbackPath, client);
    final Pac4jCallbackHandler callbackHandler = new Pac4jCallbackHandlerBuilder().build();
    final Pac4jAuthenticationHandler authenticationHandler = new Pac4jAuthenticationHandler(client.getName(), authorizer);
    return Handlers.chain(clientsHandler, Handlers.path(callbackPath, callbackHandler), authenticationHandler, handler);
  }
}
