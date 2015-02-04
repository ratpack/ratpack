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

import com.google.inject.Injector;
import com.google.inject.Provides;
import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.Handlers;
import ratpack.pac4j.Authorizer;
import ratpack.pac4j.Pac4jCallbackHandlerBuilder;
import ratpack.registry.Registry;

/**
 * Base class for pac4j integration modules.
 *
 * @param <C> The {@link org.pac4j.core.credentials.Credentials} type
 * @param <U> The {@link org.pac4j.core.profile.UserProfile} type
 */
public abstract class AbstractPac4jModule<C extends Credentials, U extends UserProfile> extends ConfigurableModule<AbstractPac4jModule.Config> {

  public static final String DEFAULT_CALLBACK_PATH = "pac4j-callback";

  /**
   * The configuration object for {@link AbstractPac4jModule}.
   */
  public static class Config {

    private String callbackPath = DEFAULT_CALLBACK_PATH;

    /**
     * Returns the path to use for callbacks from the identity provider.
     *
     * @return the path to use for callbacks
     */
    public String getCallbackPath() {
      return callbackPath;
    }

    /**
     * Sets the path to use for callbacks from the identity provider.
     *
     * @param callbackPath The callback path
     * @return this
     */
    public Config callbackPath(String callbackPath) {
      this.callbackPath = callbackPath;
      return this;
    }
  }

  @Override
  protected void configure() { }

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

  @Provides
  protected Pac4JHandlerDecorator pac4JHandlerDecorator(Config config, Injector injector) {
    return new Pac4JHandlerDecorator(config, getClient(injector), getAuthorizer(injector));
  }

  private static class Pac4JHandlerDecorator implements HandlerDecorator {

    private final Config config;
    private final Client<?, ?> client;
    private final Authorizer authorizer;

    public Pac4JHandlerDecorator(Config config,   Client<?, ?> client, Authorizer authorizer) {
      this.config = config;
      this.client = client;
      this.authorizer = authorizer;
    }

    @Override
    public Handler decorate(Registry serverRegistry, Handler rest) {
      final String callbackPath = config.getCallbackPath();
      final Authorizer authorizer = this.authorizer;
      final Pac4jClientsHandler clientsHandler = new Pac4jClientsHandler(callbackPath, client);
      final Handler callbackHandler = new Pac4jCallbackHandlerBuilder().build();
      final Pac4jAuthenticationHandler authenticationHandler = new Pac4jAuthenticationHandler(client.getName(), authorizer);
      return Handlers.chain(clientsHandler, Handlers.path(callbackPath, callbackHandler), authenticationHandler, rest);
    }
  }

}
