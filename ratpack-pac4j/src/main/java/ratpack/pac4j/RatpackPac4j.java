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
import ratpack.handling.Handler;
import ratpack.pac4j.internal.Pac4jAuthenticationHandler;
import ratpack.pac4j.internal.Pac4jCallbackHandler;

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

  public static final String DEFAULT_CALLBACK_PATH = "auth-callback";

  private RatpackPac4j() {
  }

  public static Handler callback(String path, Client<?, ?>... clients) {
    return new Pac4jCallbackHandler(path, ImmutableList.copyOf(clients));
  }

  public static Handler callback(Client<?, ?>... clients) {
    return callback(DEFAULT_CALLBACK_PATH, clients);
  }

  public static Handler auth(Class<? extends Client<?, ?>> clientType) {
    return new Pac4jAuthenticationHandler(clientType);
  }

}
