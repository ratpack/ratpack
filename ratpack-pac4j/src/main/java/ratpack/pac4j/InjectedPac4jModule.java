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

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Types;
import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import ratpack.pac4j.internal.AbstractPac4jModule;

import java.lang.reflect.Type;

/**
 * An extension module that provides support for authentication via pac4j.
 * <p>
 * If you don't need/want to perform dependency injection on either the {@link org.pac4j.core.client.Client} or {@link ratpack.pac4j.Authorizer}, use {@link ratpack.pac4j.Pac4jModule} instead.
 * </p>
 * <p>
 * To use this module, you need to register it as well as a custom module that binds a {@link org.pac4j.core.client.Client} and an {@link ratpack.pac4j.Authorizer}.
 * </p>
 *
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import com.google.inject.*;
 * import org.pac4j.core.client.Client;
 * import org.pac4j.openid.client.GoogleOpenIdClient;
 * import org.pac4j.openid.credentials.OpenIdCredentials;
 * import org.pac4j.openid.profile.google.GoogleOpenIdProfile;
 * import ratpack.func.Action;
 * import ratpack.guice.*;
 * import ratpack.guice.Guice;
 * import ratpack.handling.*;
 * import ratpack.launch.*;
 * import ratpack.pac4j.*;
 * import ratpack.session.SessionModule;
 * import ratpack.session.store.MapSessionsModule;
 *
 * class AuthenticateAllAuthorizer extends AbstractAuthorizer {
 *   public boolean isAuthenticationRequired(Context context) {
 *     return true;
 *   }
 * }
 *
 * class MyHandler implements Handler {
 *   public void handle(final Context context) {
 *     context.render("Authenticated as " + context.getRequest().get(GoogleOpenIdProfile.class).getDisplayName());
 *   }
 * }
 *
 * class Bindings implements Action&lt;BindingsSpec&gt; {
 *   public void execute(BindingsSpec bindings) {
 *     bindings.add(
 *       new SessionModule(),
 *       new MapSessionsModule(10, 5),
 *       new InjectedPac4jModule&lt;&gt;(OpenIdCredentials.class, GoogleOpenIdProfile.class),
 *       new AbstractModule() {
 *         protected void configure() {
 *           bind(new TypeLiteral&lt;Client&lt;OpenIdCredentials, GoogleOpenIdProfile&gt;&gt;() {}).to(GoogleOpenIdClient.class);
 *           bind(Authorizer.class).to(AuthenticateAllAuthorizer.class);
 *         }
 *       }
 *     );
 *   }
 * }
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
 *   .build(new HandlerFactory() {
 *     public Handler create(LaunchConfig launchConfig) throws Exception {
 *       return Guice.chain(launchConfig, new Bindings(), new ChainAction() {
 *         protected void execute() {
 *           handler(new MyHandler());
 *         }
 *       });
 *     }
 *   });
 * </pre>
 *
 * Example usage: (Groovy DSL)
 * <pre class="groovy-ratpack-dsl">
 * import com.google.inject.*
 * import org.pac4j.core.client.Client
 * import org.pac4j.openid.client.GoogleOpenIdClient
 * import org.pac4j.openid.credentials.OpenIdCredentials
 * import org.pac4j.openid.profile.google.GoogleOpenIdProfile
 * import ratpack.handling.Context
 * import ratpack.pac4j.*
 * import ratpack.session.SessionModule
 * import ratpack.session.store.MapSessionsModule
 *
 * import static ratpack.groovy.Groovy.ratpack
 *
 * class AuthenticateAllAuthorizer extends AbstractAuthorizer {
 *   boolean isAuthenticationRequired(Context context) {
 *     true
 *   }
 * }
 *
 * class SecurityModule extends AbstractModule {
 *   protected void configure() {
 *     bind(new TypeLiteral&lt;Client&lt;OpenIdCredentials, GoogleOpenIdProfile&gt;&gt;() {}).to(GoogleOpenIdClient)
 *     bind(Authorizer).to(AuthenticateAllAuthorizer)
 *   }
 * }
 *
 * ratpack {
 *   bindings {
 *     add new SessionModule(),
 *         new MapSessionsModule(10, 5),
 *         new InjectedPac4jModule&lt;&gt;(OpenIdCredentials, GoogleOpenIdProfile),
 *         new SecurityModule()
 *   }
 *   handlers {
 *     get {
 *       def userProfile = request.get(GoogleOpenIdProfile)
 *       render "Authenticated as ${userProfile.displayName}"
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <C> The {@link org.pac4j.core.credentials.Credentials} type
 * @param <U> The {@link org.pac4j.core.profile.UserProfile} type
 */
public final class InjectedPac4jModule<C extends Credentials, U extends UserProfile> extends AbstractPac4jModule<C, U> {
  private final Type credentialsType;
  private final Type userProfileType;

  /**
   * Constructs a new instance.
   *
   * @param credentialsType The credentials type to use for authentication
   * @param userProfileType The user profile type to use for authentication
   */
  public InjectedPac4jModule(Class<C> credentialsType, Class<U> userProfileType) {
    this.credentialsType = credentialsType;
    this.userProfileType = userProfileType;
  }

  @Override
  protected Client<C, U> getClient(Injector injector) {
    return injector.getInstance(getClientKey());
  }

  @Override
  protected Authorizer getAuthorizer(Injector injector) {
    return injector.getInstance(Authorizer.class);
  }

  @SuppressWarnings("unchecked")
  private Key<Client<C, U>> getClientKey() {
    return (Key<Client<C, U>>) Key.get(Types.newParameterizedType(Client.class, credentialsType, userProfileType));
  }
}
