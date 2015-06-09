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
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.exec.Fulfiller;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.pac4j.internal.Pac4jCallbackHandler;
import ratpack.pac4j.internal.Pac4jSessionKeys;
import ratpack.pac4j.internal.RatpackWebContext;
import ratpack.path.PathBinding;
import ratpack.registry.Registries;
import ratpack.session.Session;

import java.util.Optional;

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
 *         .all(RatpackPac4j.callback(new GitHubClient("key", "secret"))) // callback handler must be upstream from auth handlers
 *         .prefix("private", p -> p
 *           .all(RatpackPac4j.auth(GitHubClient.class)) // authenticate all requests flowing through here
 *           .all(ctx -> {
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
  public static final String DEFAULT_CALLBACK_PATH = "authenticator";

  private RatpackPac4j() {
  }

  /**
   * Returns the callback handler, which handles authentication requests.
   * <p>
   * This handler <b>MUST</b> be placed <b>BEFORE</b> the {@link #auth auth handler} in the handler pipeline.
   * It should be added to the handler chain via the {@link Chain#all(Handler)}.
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
    return ctx -> RatpackPac4j.login(ctx, clientType).then(userProfile ->
        ctx.next(Registries.just(userProfile))
    );
  }

  /**
   * Logs the user in by redirecting to the authentication callback, or fulfills the returned promise.
   * <p>
   * This method can be used to programmatically initiate a log in, if required.
   * If the user is already logged in, the user profile will be provided via the returned promise.
   * If the user is not already logged in, the promise will not be fulfilled and the user will be redirected
   * to the authentication callback. As such, like {@link #auth(Class)}, this can only be used downstream of
   * the {@link #callback(Client[])} handler.
   *
   * <pre class="java">{@code
   * import org.pac4j.http.client.BasicAuthClient;
   * import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
   * import org.pac4j.http.profile.UsernameProfileCreator;
   * import ratpack.guice.Guice;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.Optional;
   *
   * import static junit.framework.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.callback(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
   *             .get("auth", ctx -> RatpackPac4j.login(ctx, BasicAuthClient.class).then(p -> ctx.redirect("/")))
   *             .get(ctx ->
   *                 RatpackPac4j.userProfile(ctx)
   *                   .route(Optional::isPresent, p -> ctx.render("Hello " + p.get().getId()))
   *                   .then(p -> ctx.render("not authenticated"))
   *             )
   *         )
   *     ).test(httpClient -> {
   *       // user is not authenticated
   *       assertEquals("not authenticated", httpClient.getText());
   *
   *       // authenticate…
   *       ReceivedResponse response = httpClient.requestSpec(r -> r.basicAuth("user", "user")).get("auth");
   *
   *       // authenticated (redirected to /)
   *       assertEquals("Hello user", response.getBody().getText());
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param ctx the handling context
   * @param clientType the client type to authenticate with
   * @return a promise for the user profile, fulfilled if logged in
   */
  public static Promise<UserProfile> login(Context ctx, Class<? extends Client<?, ?>> clientType) {
    return userProfile(ctx)
      .route(p -> !p.isPresent(), p -> initiateAuthentication(ctx, clientType))
      .map(Optional::get);
  }

  /**
   * Obtains the logged in user's profile, if the user is logged in.
   * <p>
   * The promised optional will be empty if the user is not authenticated.
   * <p>
   * This method should be used if the user <i>may</i> have been authenticated.
   * That is, when the the need for the profile is not downstream of an {@link #auth(Class)} handler,
   * as the auth handler puts the profile into the context registry for easy retrieval.
   * <p>
   * This method returns a promise as it will attempt to load the profile from the session if it
   * isn't already in the context registry.
   *
   * <pre class="java">{@code
   * import io.netty.handler.codec.http.HttpHeaderNames;
   * import org.pac4j.core.profile.UserProfile;
   * import org.pac4j.http.client.BasicAuthClient;
   * import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
   * import org.pac4j.http.profile.UsernameProfileCreator;
   * import ratpack.guice.Guice;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.Optional;
   *
   * import static junit.framework.Assert.assertEquals;
   * import static org.junit.Assert.assertTrue;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.callback(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
   *             .prefix("auth", a -> a
   *                 .all(RatpackPac4j.auth(BasicAuthClient.class))
   *                 .get(ctx -> {
   *                   ctx.render("Hello " + ctx.get(UserProfile.class).getId());
   *                 })
   *             )
   *             .get("no-auth", ctx -> {
   *               RatpackPac4j.userProfile(ctx)
   *                 .route(Optional::isPresent, p -> ctx.render("Hello " + p.get().getId()))
   *                 .then(p -> ctx.render("not authenticated"));
   *             })
   *         )
   *     ).test(httpClient -> {
   *       // User is not authenticated
   *       assertEquals("not authenticated", httpClient.getText("no-auth"));
   *
   *       // Authenticate…
   *       ReceivedResponse response = httpClient.requestSpec(r -> r.redirects(0)).get("auth");
   *       assertEquals(302, response.getStatusCode());
   *       String redirectTo = response.getHeaders().get(HttpHeaderNames.LOCATION);
   *       assertEquals(401, httpClient.get(redirectTo).getStatusCode());
   *       response = httpClient.requestSpec(r -> r
   *           .basicAuth("user", "user")
   *           .redirects(0)
   *       ).post(redirectTo);
   *       assertEquals(302, response.getStatusCode());
   *       redirectTo = response.getHeaders().get(HttpHeaderNames.LOCATION);
   *       assertTrue(redirectTo.endsWith("/auth"));
   *       assertEquals("Hello user", httpClient.getText(redirectTo));
   *
   *       // User is now authenticated
   *       assertEquals("Hello user", httpClient.getText("no-auth"));
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param ctx the handling context
   * @return a promise for the user profile
   * @see #userProfile(Context, Class)
   */
  public static Promise<Optional<UserProfile>> userProfile(Context ctx) {
    return userProfile(ctx, UserProfile.class);
  }

  /**
   * Obtains the logged in user's profile, of the given type, if the user is logged in.
   * <p>
   * The promised optional will be empty if the user is not authenticated.
   * If there exists a {@link UserProfile} for the current user but it is not compatible with the requested type,
   * the returned promise will be a failure with a {@link ClassCastException}.
   * <p>
   * This method should be used if the user <i>may</i> have been authenticated.
   * That is, when the the need for the profile is not downstream of an {@link #auth(Class)} handler,
   * as the auth handler puts the profile into the context registry for easy retrieval.
   * <p>
   * This method returns a promise as it will attempt to load the profile from the session if it
   * isn't already in the context registry.
   *
   * @param ctx the handling context
   * @param type the type of the user profile
   * @param <T> the type of the user profile
   * @return a promise for the user profile
   * @see #userProfile(Context)
   */
  public static <T extends UserProfile> Promise<Optional<T>> userProfile(Context ctx, Class<T> type) {
    return ctx.promise(f ->
        toProfile(type, f, ctx.maybeGet(UserProfile.class), () ->
            ctx.get(Session.class).getData()
              .map(d -> d.get(Pac4jSessionKeys.USER_PROFILE))
              .then(p -> toProfile(type, f, p, () -> f.success(Optional.empty())))
        )
    );
  }

  /**
   * Logs out the current user, removing their profile from the session.
   * <p>
   * The returned operation simply removes the profile from the session, regardless of whether it's actually there or not.
   *
   * <pre class="java">{@code
   * import org.pac4j.http.client.BasicAuthClient;
   * import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
   * import org.pac4j.http.profile.UsernameProfileCreator;
   * import ratpack.guice.Guice;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.Optional;
   *
   * import static junit.framework.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.callback(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
   *             .get("auth", ctx -> RatpackPac4j.login(ctx, BasicAuthClient.class).then(p -> ctx.redirect("/")))
   *             .get(ctx ->
   *                 RatpackPac4j.userProfile(ctx)
   *                   .route(Optional::isPresent, p -> ctx.render("Hello " + p.get().getId()))
   *                   .then(p -> ctx.render("not authenticated"))
   *             )
   *             .get("logout", ctx ->
   *                 RatpackPac4j.logout(ctx).then(() -> ctx.redirect("/"))
   *             )
   *         )
   *     ).test(httpClient -> {
   *       // user is not authenticated
   *       assertEquals("not authenticated", httpClient.getText());
   *
   *       // authenticate…
   *       ReceivedResponse response = httpClient.requestSpec(r -> r.basicAuth("user", "user")).get("auth");
   *
   *       // authenticated (redirected to /)
   *       assertEquals("Hello user", response.getBody().getText());
   *
   *       // logout (redirected to /)
   *       assertEquals("not authenticated", httpClient.getText("logout"));
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param ctx the handling context
   * @return the logout operation
   */
  public static Operation logout(Context ctx) {
    return ctx.get(Session.class).getData().operation(data -> data.remove(Pac4jSessionKeys.USER_PROFILE));
  }

  private static <T extends UserProfile> void toProfile(Class<T> type, Fulfiller<Optional<T>> fulfiller, Optional<UserProfile> userProfileOptional, Block onEmpty) throws Exception {
    if (userProfileOptional.isPresent()) {
      final UserProfile userProfile = userProfileOptional.get();
      if (type.isInstance(userProfile)) {
        fulfiller.success(Optional.of(type.cast(userProfile)));
      } else {
        fulfiller.error(new ClassCastException("UserProfile is of type " + userProfile.getClass() + ", and is not compatible with " + type));
      }
    } else {
      onEmpty.execute();
    }
  }

  private static void initiateAuthentication(Context ctx, Class<? extends Client<?, ?>> clientType) {
    Request request = ctx.getRequest();
    Clients clients = ctx.get(Clients.class);
    Client<?, ?> client = clients.findClient(clientType);

    ctx.get(Session.class).getData().then(session -> {
      RatpackWebContext webContext = new RatpackWebContext(ctx, session);
      session.set(Pac4jSessionKeys.REQUESTED_URL, request.getUri());

      try {
        client.redirect(webContext, true, request.isAjaxRequest());
      } catch (Exception e) {
        if (e instanceof RequiresHttpAction) {
          webContext.sendResponse((RequiresHttpAction) e);
          return;
        } else {
          ctx.error(new TechnicalException("Failed to redirect", e));
        }
      }

      webContext.sendResponse();
    });
  }
}
