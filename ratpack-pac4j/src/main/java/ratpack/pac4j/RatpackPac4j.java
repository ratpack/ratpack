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
import org.pac4j.core.authorization.Authorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import ratpack.exec.Blocking;
import ratpack.exec.Downstream;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.UserId;
import ratpack.http.Request;
import ratpack.pac4j.internal.Pac4jAuthenticator;
import ratpack.pac4j.internal.Pac4jSessionKeys;
import ratpack.pac4j.internal.RatpackWebContext;
import ratpack.path.PathBinding;
import ratpack.registry.Registry;
import ratpack.session.Session;
import ratpack.util.Types;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Iterables.all;
import static java.util.Arrays.asList;

/**
 * Provides integration with the <a href="http://www.pac4j.org">Pac4j library</a> for authentication and authorization.
 * <p>
 * Pac4j support many different authentication providers, such as external sources like GitHub, Twitter, Facebook etc., as well
 * as proprietary local authentication sources.
 * <p>
 * The {@link #authenticator(Client[])} method provides a handler that implements the authentication process,
 * and is required in all apps wanting to use authentication.
 * <p>
 * The {@link #requireAuth(Class, Authorizer...)} method provides a handler that acts like a filter, ensuring that the user is authenticated for all requests.
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
   * Creates a handler that implements authentication when the request path matches, and makes a Pac4j {@link Clients} available to downstream handlers otherwise.
   * <p>
   * This methods performs the same function as {@link #authenticator(String, ClientsProvider)},
   * but is more convenient to use when the {@link Client} instances do not depend on the request environment.
   *
   * @param path the path to bind the authenticator to (relative to the current request path binding)
   * @param clients the supported authentication clients
   * @return a handler
   */
  public static Handler authenticator(String path, Client<?, ?>... clients) {
    ImmutableList<Client<?, ?>> clientList = ImmutableList.copyOf(clients);
    return authenticator(path, ctx -> clientList);
  }

  /**
   * Provides the set of Pac4j {@link Client clients}.
   *
   * @see #authenticator(String, ClientsProvider)
   * @since 1.1
   */
  public interface ClientsProvider {
    Iterable<? extends Client<?, ?>> get(Context ctx);
  }

  /**
   * Creates a handler that implements authentication when the request path matches, and makes a Pac4j {@link Clients} available to downstream handlers otherwise.
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
   * The {@link Clients} instance will be retrieved downstream by any {@link #requireAuth(Class, Authorizer...)} handler (or use of {@link #login(Context, Class)}.
   *
   * @param path the path to bind the authenticator to (relative to the current request path binding)
   * @param clientsProvider the provider of authentication clients
   * @return a handler
   */
  public static Handler authenticator(String path, ClientsProvider clientsProvider) {
    return new Pac4jAuthenticator(path, clientsProvider);
  }

  /**
   * An authentication and authorization “filter”.
   * <p>
   * This handler can be used to ensure that a user profile is available for all downstream handlers.
   * If there is no user profile present in the session (i.e. user not logged in), authentication will be initiated based on the given client type (i.e. redirect to the {@link #authenticator(Client[])} handler).
   * If there is a {@link UserProfile} present in the session, this handler will push the user profile into the context registry before delegating downstream.
   * If there is a {@link UserProfile} present in the context registry, this handler will simply delegate downstream.
   * <p>
   * If there is a {@link UserProfile}, <b>each</b> of the given authorizers will be tested in turn and all must return true.
   * If so, control will flow to the next handler.
   * Otherwise, a {@code 403} {@link Context#clientError(int) client error} will be issued.
   * <p>
   * This handler requires a {@link Clients} instance available in the context registry.
   * As such, this handler should be downstream of the {@link #authenticator(Client[])} handler.
   *
   * <pre class="java">{@code
   * import org.pac4j.core.profile.UserProfile;
   * import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
   * import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
   * import ratpack.guice.Guice;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.authenticator(new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator())))
   *             .get("logout", ctx -> RatpackPac4j.logout(ctx).then(() -> ctx.render("logged out")))
   *             .prefix("require-authn", a -> a
   *                 .all(RatpackPac4j.requireAuth(IndirectBasicAuthClient.class))
   *                 .get(ctx -> ctx.render("Hello " + ctx.get(UserProfile.class).getId()))
   *             )
   *            .prefix("require-authz", a -> a
   *              .all(RatpackPac4j.requireAuth(IndirectBasicAuthClient.class, (ctx, profile) -> { return "special-user".equals(profile.getId()); }))
   *              .get(ctx -> ctx.render("Hello " + ctx.get(UserProfile.class).getId()))
   *            )
   *            .get(ctx -> ctx.render("no auth required"))
   *         )
   *     ).test(httpClient -> {
   *       httpClient.requestSpec(r -> r.redirects(1));
   *       assertEquals("no auth required", httpClient.getText());
   *
   *       assertEquals(401, httpClient.get("require-authn").getStatusCode());
   *       assertEquals("Hello user", httpClient.requestSpec(r -> r.basicAuth("user", "user")).getText("require-authn"));
   *
   *       assertEquals(403, httpClient.get("require-authz").getStatusCode());
   *
   *       assertEquals("logged out", httpClient.getText("logout"));
   *       httpClient.resetRequest();
   *
   *       assertEquals(401, httpClient.get("require-authz").getStatusCode());
   *       assertEquals("Hello special-user", httpClient.requestSpec(r -> r.basicAuth("special-user", "special-user")).getText("require-authz"));
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param clientType the client type to use to authenticate with if required
   * @param authorizers the authorizers to check authorizations
   * @return a handler
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <C extends Credentials, U extends UserProfile> Handler requireAuth(Class<? extends Client<C, U>> clientType, Authorizer<? super U>... authorizers) {
    List<Authorizer<? super U>> authorizerList = asList(authorizers);
    return ctx -> RatpackPac4j.login(ctx, clientType).then(userProfile -> {
      if (authorizerList.isEmpty()) {
        ctx.next(Registry.single(userProfile));
      } else {
        RatpackWebContext.from(ctx, false).then(webContext -> {
          if (all(authorizerList, a -> a == null || a.isAuthorized(webContext, userProfile))) {
            ctx.next(Registry.single(userProfile));
          } else {
            ctx.clientError(403);
          }
        });
      }
    });
  }

  /**
   * Logs the user in by redirecting to the authenticator, or provides the user profile if already logged in.
   * <p>
   * This method can be used to programmatically initiate a log in, if required.
   * If the user is already logged in, the user profile will be provided via the returned promise.
   * If the user is not already logged in, the promise will not be fulfilled and the user will be redirected to the authenticator.
   * As such, like {@link #requireAuth(Class, Authorizer...)}, this can only be used downstream of the {@link #authenticator(Client[])} handler.
   *
   * <pre class="java">{@code
   * import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
   * import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
   * import ratpack.guice.Guice;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.Optional;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.authenticator(new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator())))
   *             .get("auth", ctx -> RatpackPac4j.login(ctx, IndirectBasicAuthClient.class).then(p -> ctx.redirect("/")))
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
  public static <C extends Credentials, U extends UserProfile> Promise<U> login(Context ctx, Class<? extends Client<C, U>> clientType) {
    if (isDirect(clientType)) {
      return userProfile(ctx)
        .flatMap(p -> {
          if (p.isPresent()) {
            Optional<U> cast = Types.cast(p);
            return Promise.value(cast);
          } else {
            return performDirectAuthentication(ctx, clientType);
          }
        })
        .route(p -> !p.isPresent(), p -> ctx.clientError(401))
        .map(Optional::get);
    } else {
      return userProfile(ctx)
        .route(p -> !p.isPresent(), p -> initiateAuthentication(ctx, clientType))
        .map(Optional::get)
        .map(Types::<U>cast);
    }
  }

  /**
   * Obtains the logged in user's profile, if the user is logged in.
   * <p>
   * The promised optional will be empty if the user is not authenticated.
   * <p>
   * This method should be used if the user <i>may</i> have been authenticated.
   * That is, when the the need for the profile is not downstream of an {@link #requireAuth(Class, Authorizer...)} handler,
   * as the auth handler puts the profile into the context registry for easy retrieval.
   * <p>
   * This method returns a promise as it will attempt to load the profile from the session if it
   * isn't already in the context registry.
   *
   * <pre class="java">{@code
   * import io.netty.handler.codec.http.HttpHeaderNames;
   * import org.pac4j.core.profile.UserProfile;
   * import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
   * import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
   * import ratpack.guice.Guice;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.Optional;
   *
   * import static org.junit.Assert.assertEquals;
   * import static org.junit.Assert.assertTrue;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.authenticator(new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator())))
   *             .prefix("auth", a -> a
   *                 .all(RatpackPac4j.requireAuth(IndirectBasicAuthClient.class))
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
   * That is, when the the need for the profile is not downstream of an {@link #requireAuth(Class, Authorizer...)} handler,
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
    return Promise.async(f ->
      toProfile(type, f, ctx.maybeGet(UserProfile.class), () ->
        ctx.get(Session.class)
          .get(Pac4jSessionKeys.USER_PROFILE)
          .then(p -> {
            if (p.isPresent()) {
              ctx.getRequest().add(UserId.class, UserId.of(p.get().getId()));
            }
            toProfile(type, f, p, () -> f.success(Optional.<T>empty()));
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
   * import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
   * import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
   * import ratpack.guice.Guice;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.pac4j.RatpackPac4j;
   * import ratpack.session.SessionModule;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.Optional;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registry(Guice.registry(b -> b.module(SessionModule.class)))
   *         .handlers(c -> c
   *             .all(RatpackPac4j.authenticator(new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator())))
   *             .get("auth", ctx -> RatpackPac4j.login(ctx, IndirectBasicAuthClient.class).then(p -> ctx.redirect("/")))
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
    return ctx.get(Session.class)
      .remove(Pac4jSessionKeys.USER_PROFILE);
  }

  /**
   * Adapts a Ratpack {@link Context} to a Pac4j {@link WebContext}.
   * <p>
   * The returned WebContext does not have access to the request body.
   * {@link WebContext#getRequestParameters()} and associated methods will not include any
   * form parameters if the request was a form.
   *
   * @param ctx a Ratpack context
   * @return a Pac4j web context
   * @since 1.4
   */
  public static Promise<WebContext> webContext(Context ctx) {
    return Types.cast(RatpackWebContext.from(ctx, false));
  }

  private static <T extends UserProfile> void toProfile(Class<T> type, Downstream<? super Optional<T>> downstream, Optional<UserProfile> userProfileOptional, Block onEmpty) throws Exception {
    if (userProfileOptional.isPresent()) {
      final UserProfile userProfile = userProfileOptional.get();
      if (type.isInstance(userProfile)) {
        downstream.success(Optional.of(type.cast(userProfile)));
      } else {
        downstream.error(new ClassCastException("UserProfile is of type " + userProfile.getClass() + ", and is not compatible with " + type));
      }
    } else {
      onEmpty.execute();
    }
  }

  private static void initiateAuthentication(Context ctx, Class<? extends Client<?, ?>> clientType) {
    Request request = ctx.getRequest();
    Clients clients = ctx.get(Clients.class);
    Client<?, ?> client = clients.findClient(clientType);

    RatpackWebContext.from(ctx, false).then(webContext -> {
      webContext.getSession().set(Pac4jSessionKeys.REQUESTED_URL, request.getUri());
      try {
        client.redirect(webContext, true);
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

  private static <C extends Credentials, U extends UserProfile> Promise<Optional<U>> performDirectAuthentication(Context ctx, Class<? extends Client<C, U>> clientType) {
    return RatpackWebContext.from(ctx, false).flatMap(webContext ->
      Blocking.get(() -> {
        Clients clients = ctx.get(Clients.class);
        Client<C, U> client = clients.findClient(clientType);
        return userProfileFromCredentials(client, webContext);
      })
    );
  }

  private static <C extends Credentials, U extends UserProfile> Optional<U> userProfileFromCredentials(Client<C, U> client, RatpackWebContext webContext) throws RequiresHttpAction {
    C credentials = client.getCredentials(webContext);
    U userProfile = client.getUserProfile(credentials, webContext);
    return Optional.ofNullable(userProfile);
  }

  private static boolean isDirect(Class<? extends Client<?, ?>> clientType) {
    return DirectClient.class.isAssignableFrom(clientType);
  }
}
