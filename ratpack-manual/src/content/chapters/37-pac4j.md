# Security using pac4j

The `ratpack-pac4j` extension provides authentication support based on [pac4j](https://github.com/pac4j/pac4j).

`pac4j` is a generic authentication engine which is implemented by many frameworks and supports multiple protocols: OAuth, CAS, OpenID (Connect), SAML, Google App Engine and HTTP (form and basic auth).

The [`RatpackPac4j`](api/ratpack/pac4j/RatpackPac4j.html) class is the main component to use to manage the security of your web application.


## Prerequisite

All authentication mechanisms work by redirecting the user to an identity provider and finishing the login process when he is redirected back to the application with some dedicated credentials.
To make these authentication processes work and to save the originally requested url, the session is required. That's why the [`SessionModule`](api/ratpack/session/SessionModule.html) is necessary.


## Authentication mechanisms definition

Each authentication mechanism in `pac4j` is defined as a [Client](https://github.com/pac4j/pac4j/blob/master/pac4j-core/src/main/java/org/pac4j/core/client/Client.java).
Thus, configuring the ways to login into your application requires to create the right [clients](https://github.com/pac4j/pac4j/wiki/Clients): a `FacebookClient` to log in with Facebook, a `TwitterClient` to log in with Twitter, a `Google2Client` to log in with Google (using OAuth v2.0 protocol), and so on.

Create your `pac4j` clients and define them **BEFORE** any other authentication operation in your web chain using the [`RatpackPac4j.authenticator(Client<?, ?>... clients)`](api/ratpack/pac4j/RatpackPac4j.html#authenticator-org.pac4j.core.client.Client...-) method. Then secure an url, get the authenticated user profile or log out. 

```language-java
import com.google.appengine.repackaged.com.google.common.collect.Maps;
import org.pac4j.cas.client.CasClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.BasicAuthClient;
import org.pac4j.http.client.FormClient;
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.http.profile.UsernameProfileCreator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import ratpack.func.Action;
import ratpack.groovy.template.TextTemplateModule;
import ratpack.guice.Guice;
import ratpack.handling.Chain;
import ratpack.pac4j.RatpackPac4j;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.session.SessionModule;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonMap;
import static ratpack.groovy.Groovy.groovyTemplate;

public class Example {

    public static void main(final String[] args) throws Exception {
        RatpackServer.start(server -> server
                .serverConfig(ServerConfig
                        .baseDir(new File("src/main"))
                        .port(8080)
                )
                .registry(Guice.registry(b -> b
                        .module(TextTemplateModule.class)
                        .module(SessionModule.class)
                ))
                .handlers(chain -> {
                    final FacebookClient facebookClient = new FacebookClient("fkey", "fsecret");
                    final TwitterClient twitterClient = new TwitterClient("tkey", "tsecret");
                    final FormClient formClient = new FormClient("/theForm.html", new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator());
                    final BasicAuthClient basicAuthClient = new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator());
                    final CasClient casClient = new CasClient();
                    casClient.setCasLoginUrl("http://mycasserver/login");
                    chain
                        .all(RatpackPac4j.authenticator(formClient, formClient, facebookClient, twitterClient, basicAuthClient, casClient))
                        .prefix("facebook", requireAuth(FacebookClient.class))
                        .prefix("twitter", requireAuth(TwitterClient.class))
                        .prefix("form", requireAuth(FormClient.class))
                        .prefix("basicauth", requireAuth(BasicAuthClient.class))
                        .prefix("cas", requireAuth(CasClient.class))
                        .path("theForm.html", ctx ->
                            ctx.render(groovyTemplate(
                                singletonMap("callbackUrl", formClient.getCallbackUrl()),
                                "theForm.html"
                            ))
                        )
                        .path("logout.html", ctx ->
                                RatpackPac4j.logout(ctx).then(() -> ctx.redirect("index.html"))
                        )
                        .path("index.html", ctx -> {
                            RatpackPac4j.userProfile(ctx)
                                .left(RatpackPac4j.webContext(ctx))
                                .then(pair -> {
                                    final WebContext webContext = pair.left;
                                    final Optional<UserProfile> profile = pair.right;
                                    final Map<String, Object> model = Maps.newHashMap();
                                    profile.ifPresent(p -> model.put("profile", p));
                                    final Clients clients = ctx.get(Clients.class);
                                    final FacebookClient fbclient = clients.findClient(FacebookClient.class);
                                    final String fbUrl = fbclient.getRedirectionUrl(webContext);
                                    model.put("facebookLoginUrl", fbUrl);
                                    ctx.render(groovyTemplate(model, "index.html"));
                                });
                        });
                })
        );
    }
    private static Action<Chain> requireAuth(Class<? extends Client<?, ?>> clientClass) {
        return chain -> chain
            .all(RatpackPac4j.requireAuth(clientClass))
            .path("index.html", ctx ->
                    ctx.render(groovyTemplate(
                        singletonMap("profile", ctx.get(UserProfile.class)),
                        "protected.html"
                    ))
            );
    }
}
```

The `authenticator` method creates the `/authenticator` url endpoint to receive callbacks and finish the login processes (it can be set to an another `path` using [`RatpackPac4j.authenticator(String path, Client<?, ?>... clients)`](api/ratpack/pac4j/RatpackPac4j.html#authenticator-java.lang.String-org.pac4j.core.client.Client...-).


## Url protection

If you want to protect an url and request the user to be authenticated, you need to use the [`Ratpack.requireAuth(Class<? extends Client<?, ?>> clientType)`](api/ratpack/pac4j/RatpackPac4j.html#requireAuth-java.lang.Class-) method and provide the client required to perform the authentication.
After a successful authentication, the authenticated user profile is available in the *context*.

You can also use the [`RatpackPac4j.login(Context ctx, Class<? extends Client<?, ?>> clientType)`](api/ratpack/pac4j/RatpackPac4j.html#login-ratpack.handling.Context-java.lang.Class-) method which does not save the user profile in *context* but returns a promise of it (`Promise<UserProfile>`).


## User profile retrieval

To get the user profile (or not) without initiating an authentication, you can just use the [`RatpackPac4j.userProfile(Context ctx)`](api/ratpack/pac4j/RatpackPac4j.html#userProfile-ratpack.handling.Context-) method.


## Logout

The authenticated user profile is stored in session. To logout the current authenticated user, the [`RatpakcPac4j.logout(Context ctx)`](api/ratpack/pac4j/RatpackPac4j.html#logout-ratpack.handling.Context-) method must be used.


## Demo

All capabilities available through this extension are demonstrated in the [ratpack-pac4j-demo](https://github.com/pac4j/ratpack-pac4j-demo).
