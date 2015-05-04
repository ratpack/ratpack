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
 * <p>
 * To use this module, you simply need to register it.
 *
 * Example usage:
 * <pre class="java">{@code
 * import com.google.inject.AbstractModule;
 * import org.pac4j.oauth.client.GitHubClient;
 * import org.pac4j.oauth.profile.github.GitHubProfile;
 * import ratpack.guice.Guice;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.pac4j.AbstractAuthorizer;
 * import ratpack.pac4j.Pac4jModule;
 * import ratpack.session.SessionModule;
 * import ratpack.session.store.MapSessionsModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.assertEquals;
 * import static org.junit.Assert.assertTrue;
 *
 * public class Example {
 *
 *   public static class MyAuthorizer extends AbstractAuthorizer {
 *     {@literal @}Override
 *     public boolean isAuthenticationRequired(Context context) {
 *       return !context.getRequest().getPath().startsWith("public/");
 *     }
 *   }
 *
 *   public static class MyHandler implements Handler {
 *     {@literal @}Override
 *     public void handle(Context ctx) throws Exception {
 *       ctx.render("Authenticated as " + ctx.getRequest().maybeGet(GitHubProfile.class).map(GitHubProfile::getDisplayName).orElse("noone"));
 *     }
 *   }
 *
 *   public static class ServiceModule extends AbstractModule {
 *     protected void configure() {
 *       install(new Pac4jModule<>(new GitHubClient("key", "secret"), new MyAuthorizer()));
 *       install(new MapSessionsModule(10, 5));
 *       install(new SessionModule());
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *         .registry(Guice.registry(b -> b.module(ServiceModule.class)))
 *         .handler(r -> new MyHandler())
 *     ).test(httpClient -> {
 *       assertEquals("Authenticated as noone", httpClient.getText("public/test"));
 *       ReceivedResponse response = httpClient.get("private/test");
 *       String location = response.getHeaders().get("Location");
 *       assertEquals(301, response.getStatusCode());
 *       assertTrue(location != null && location.startsWith("https://github.com/login/oauth"));
 *     });
 *   }
 * }
 * }</pre>
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
