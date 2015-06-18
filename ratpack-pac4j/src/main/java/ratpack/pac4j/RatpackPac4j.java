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
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.auth.UserIdentifier;
import ratpack.exec.Fulfiller;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.pac4j.internal.Pac4jAuthenticator;
import ratpack.pac4j.internal.Pac4jSessionKeys;
import ratpack.pac4j.internal.RatpackWebContext;
import ratpack.path.PathBinding;
import ratpack.registry.Registries;
import ratpack.session.Session;
import ratpack.session.SessionData;

import java.util.Optional;

/**
 * Provides integration with the <a href="http://www.pac4j.org">Pac4j library</a> for authentication and authorization.
 * <p>
 * Pac4j support many different authentication providers, such as external sources like GitHub, Twitter, Facebook etc., as well
 * as proprietary local authentication sources.
 * <p>
 * The {@link #authenticator(Client[])} method provides a handler that implements the authentication process,
 * and is required in all apps wanting to use authentication.
 * <p>
 * The {@link #requireAuth(Class)} method provides a handler that acts like a filter, ensuring that the user is authenticated for all requests.
 * This can be used for requiring authentication for all requests starting with a particular request path for example.
 * <p>
 * The {@link #userProfile(Context)}, {@link #login(Context, Class)} and {@link #logout(Context)} methods provide programmatic authentication mechanisms.
 */
public class RatpackPac4j {

  /**
   * The default path to the authenticator, {@value}, used by {@link #authenticator(Client[])}.
   */
  public static final String DEFAULT_AUTHENTICATOR_PATH = "authenticator";

  private RatpackPac4j() {
  }

  /**
   * Calls {@link #authenticator(String, Client[])} with {@link #DEFAULT_AUTHENTICATOR_PATH}.
   *
   * @param clients the supported auth clients
   * @return a handler
   */
  public static Handler authenticator(Client<?, ?>... clients) {
    return authenticator(DEFAULT_AUTHENTICATOR_PATH, clients);
  }

  /**
   * The authenticator handler implements authentication.
   * <p>
   * This handler <b>MUST</b> be <b>BEFORE</b> any code in the handler pipeline that tries to identify the user, such as a {@link #requireAuth} handler in the pipeline.
   * It should be added to the handler chain via the {@link Chain#all(Handler)}.
   * That is, it should not be added with {@link Chain#get(Handler)} or any method that filters based on request method.
   * It is common for this handler to be one of the first handlers in the pipeline.
   * <p>
   * This handler performs two different functions, based on whether the given path matches the {@link PathBinding#getPastBinding()} component of the current path binding.
   * If the path matches, the handler will attempt authentication, which may involve redirecting to an external auth provider, which may then redirect back to this handler.
   * If authentication is successful, the {@link UserProfile} of the authenticated user will be placed into the session.
   * The user will then be redirected back to the URL that initiated the authentication.
   * <p>
   * If the path does not match, the handler will push an instance of {@link Clients} into the context registry and pass control downstream.
   * The {@link Clients} instance will be retrieved downstream by any {@link #requireAuth(Class)} handler (or use of {@link #login(Context, Class)}.
   *
   * @param path the path to bind the authenticator to (relative to the current request path binding)
   * @param clients the supported authentication clients
   * @return a handler
   */
  public static Handler authenticator(String path, Client<?, ?>... clients) {
    return new Pac4jAuthenticator(path, ImmutableList.copyOf(clients));
  }

  /**
   * An authentication “filter”, that initiates authentication if necessary.
   * <p>
   * This handler can be used to ensure that a user profile is available for all downstream handlers.
   * If there is no user profile present in the session (i.e. user not logged in), authentication will be initiated based on the given client type (i.e. redirect to the {@link #authenticator(Client[])} handler).
   * If there is a {@link UserProfile} present in the session, this handler will push the user profile into the context registry before delegating downstream.
   * If there is a {@link UserProfile} present in the context registry, this handler will simply delegate downstream.
   * <p>
   * This handler requires a {@link Clients} instance available in the context registry.
   * As such, this handler should be downstream of the {@link #authenticator(Client[])} handler.
   *
   * <pre class="java">{@code
   * import org.pac4j.core.profile.UserProfile;
   * import org.pac4j.http.client.BasicAuthClient;
   * import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
   * import org.pac4j.http.profile.UsernameProfileCreator;
   * import ratpack.guice.Guice;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static junit.framework.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.authenticator(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
   *             .prefix("require-auth", a -> a
   *                 .all(RatpackPac4j.requireAuth(BasicAuthClient.class))
   *                 .get(ctx -> ctx.render("Hello " + ctx.get(UserProfile.class).getId()))
   *             )
   *             .get(ctx -> ctx.render("no auth required"))
   *         )
   *     ).test(httpClient -> {
   *       assertEquals("no auth required", httpClient.getText());
   *       assertEquals(401, httpClient.get("require-auth").getStatusCode());
   *       assertEquals("Hello user", httpClient.requestSpec(r -> r.basicAuth("user", "user")).getText("require-auth"));
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param clientType the client type to use to authenticate with if required
   * @return a handler
   */
  public static Handler requireAuth(Class<? extends Client<?, ?>> clientType) {
    return ctx -> RatpackPac4j.login(ctx, clientType).then(userProfile ->
        ctx.next(Registries.just(userProfile))
    );
  }

  /**
   * Logs the user in by redirecting to the authenticator, or provides the user profile if already logged in.
   * <p>
   * This method can be used to programmatically initiate a log in, if required.
   * If the user is already logged in, the user profile will be provided via the returned promise.
   * If the user is not already logged in, the promise will not be fulfilled and the user will be redirected to the authenticator.
   * As such, like {@link #requireAuth(Class)}, this can only be used downstream of the {@link #authenticator(Client[])} handler.
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
   *             .all(RatpackPac4j.authenticator(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
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
   * That is, when the the need for the profile is not downstream of an {@link #requireAuth(Class)} handler,
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
   *             .all(RatpackPac4j.authenticator(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
   *             .prefix("auth", a -> a
   *                 .all(RatpackPac4j.requireAuth(BasicAuthClient.class))
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
   * That is, when the the need for the profile is not downstream of an {@link #requireAuth(Class)} handler,
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
              .then(p -> {
                ctx.getRequest().add(UserIdentifier.class, () -> p.isPresent() ? p.get().getId() : null);
                toProfile(type, f, p, () -> f.success(Optional.empty()));
              })
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
   *             .all(RatpackPac4j.authenticator(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
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

  /**
   * Creates a Pac4j {@link WebContext} implementation based on Ratpack's context.
   *
   * @param ctx the Ratpack context
   * @return a Pac4j web context
   */
  public static Promise<WebContext> webContext(Context ctx) {
    return ctx.get(Session.class).getData().map(session -> webContext(ctx, session));
  }

  private static RatpackWebContext webContext(Context ctx, SessionData sessionData) {
    return new RatpackWebContext(ctx, sessionData);
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
      RatpackWebContext webContext = webContext(ctx, session);
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
