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
import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import ratpack.pac4j.internal.AbstractPac4jModule;

/**
 * An extension module that provides support for authentication via pac4j.
 * <p>
 * If you need/want to perform dependency injection on either the {@link org.pac4j.core.client.Client} or {@link ratpack.pac4j.Authorizer}, use {@link ratpack.pac4j.InjectedPac4jModule} instead.
 * </p>
 * <p>
 * To use this module, you simply need to register it.
 * </p>
 *
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import org.pac4j.openid.client.GoogleOpenIdClient;
 * import org.pac4j.openid.profile.google.GoogleOpenIdProfile;
 * import ratpack.func.Action;
 * import ratpack.guice.*;
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
 *       new Pac4jModule&lt;&gt;(new GoogleOpenIdClient(), new AuthenticateAllAuthorizer())
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
 *
 * launchConfig.execController.close()
 * </pre>
 *
 * Example usage: (Groovy DSL)
 * <pre class="groovy-ratpack-dsl">
 * import org.pac4j.openid.client.GoogleOpenIdClient
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
 *     true // authenticate every request
 *   }
 * }
 *
 * ratpack {
 *   bindings {
 *     add new SessionModule(),
 *         new MapSessionsModule(10, 5),
 *         new Pac4jModule&lt;&gt;(new GoogleOpenIdClient(), new AuthenticateAllAuthorizer())
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
public final class Pac4jModule<C extends Credentials, U extends UserProfile> extends AbstractPac4jModule<C, U> {
  private final Client<C, U> client;
  private final Authorizer authorizer;

  /**
   * Constructs a new instance.
   *
   * @param client The pac4j client to use for authentication
   * @param authorizer The strategy to use for authorization
   */
  public Pac4jModule(Client<C, U> client, Authorizer authorizer) {
    this.client = client;
    this.authorizer = authorizer;
  }

  @Override
  protected Client<C, U> getClient(Injector injector) {
    return client;
  }

  @Override
  protected Authorizer getAuthorizer(Injector injector) {
    return authorizer;
  }
}
