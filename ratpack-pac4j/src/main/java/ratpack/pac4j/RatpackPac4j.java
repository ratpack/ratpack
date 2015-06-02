/*
 * Copyright 2015 the original author or authors.
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

import com.google.common.collect.ImmutableList;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.profile.UserProfile;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.pac4j.internal.Pac4jAuthenticationHandler;
import ratpack.pac4j.internal.Pac4jCallbackHandler;
import ratpack.path.PathBinding;

/**
 * <pre class="java">{@code
 * import org.pac4j.oauth.client.GitHubClient;
 * import org.pac4j.oauth.profile.github.GitHubProfile;
 * import ratpack.guice.Guice;
 * import ratpack.handling.Context;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.pac4j.RatpackPac4j;
 * import ratpack.session.SessionModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.assertEquals;
 * import static org.junit.Assert.assertTrue;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(
 *         Guice.registry(b -> b
 *           .module(SessionModule.class) // session support is required
 *         )
 *       )
 *       .handlers(c -> c
 *         .handler(RatpackPac4j.callback(new GitHubClient("key", "secret"))) // callback handler must be upstream from auth handlers
 *         .prefix("private", p -> p
 *           .handler(RatpackPac4j.auth(GitHubClient.class)) // authenticate all requests flowing through here
 *           .handler(ctx -> {
 *             String displayName = ctx.maybeGet(GitHubProfile.class) // auth handler puts profile in context registry
 *               .map(GitHubProfile::getDisplayName)
 *               .orElse("noone");
 *
 *             ctx.render("Authenticated as " + displayName);
 *           })
 *         )
 *       )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get("private/test");
 *       String location = response.getHeaders().get("Location");
 *       assertEquals(301, response.getStatusCode());
 *       assertTrue(location != null && location.startsWith("https://github.com/login/oauth"));
 *     });
 *   }
 * }
 * }</pre>
 */
public class RatpackPac4j {

  /**
   * The default auth callback path, {@value}, used by {@link #callback(Client[])}.
   */
  public static final String DEFAULT_CALLBACK_PATH = "auth-callback";

  private RatpackPac4j() {
  }

  /**
   * Returns the callback handler, which handles authentication requests.
   * <p>
   * This handler <b>MUST</b> be placed <b>BEFORE</b> the {@link #auth auth handler} in the handler pipeline.
   * It should be added to the handler chain via the {@link Chain#handler(Handler)} method or similar.
   * That is, it should not be added with {@link Chain#get(Handler)} or any method that filters based on request method.
   * It is common for this handler to be one of the first handlers in the pipeline.
   * <p>
   * This handler performs two different functions, based on whether the given path matches the {@link PathBinding#getPastBinding()} component of the current path binding.
   * If the path matches, the handler will attempt authentication, which may involve redirecting to an external auth provider, which may then redirect back to this handler.
   * If authentication is successful, the {@link UserProfile} of the authenticated user will be placed into the session.
   * The user will then be redirected back to the URL that initiated the authentication.
   *
   * If the path does not match, the handler will push an instance of {@link Clients} into the context registry and pass control downstream.
   * The {@link Clients} instance will be retrieved downstream by any {@link #auth(Class)} handlers and used to discover the callback URL for authentication.
   *
   * @param path the auth callback path (not typically seen by users)
   * @param clients the supported authentication clients
   * @return a handler
   */
  public static Handler callback(String path, Client<?, ?>... clients) {
    return new Pac4jCallbackHandler(path, ImmutableList.copyOf(clients));
  }

  /**
   * Calls {@link #callback(String, Client[])} with {@link #DEFAULT_CALLBACK_PATH}.
   *
   * @param clients the supported auth clients
   * @return a handler
   */
  public static Handler callback(Client<?, ?>... clients) {
    return callback(DEFAULT_CALLBACK_PATH, clients);
  }

  /**
   * An authentication “filter”, that initiates authentication if necessary.
   * <p>
   * This handler requires a {@link Clients} instance available in the context registry.
   * This can be provided by the {@link #callback(Client[])} handler.
   * As such, this handler should be downstream of the callback handler.
   * <p>
   * If there is a {@link UserProfile} present in the context registry, this handler will simply delegate downstream.
   * <p>
   * If there is a {@link UserProfile} present in the session, this handler will push the user profile into the context registry
   * before delegating downstream.
   * <p>
   * If there is no user profile present in the session, authentication will be initiated based on the given client type.
   * This involves a redirect to the {@link #callback(Client[])} handler.
   *
   * @param clientType the client type to use to authenticate with if required
   * @return a handler
   */
  public static Handler auth(Class<? extends Client<?, ?>> clientType) {
    return new Pac4jAuthenticationHandler(clientType);
  }

}
