# Security using pac4j

The `ratpack-pac4j` extension provides authentication support based on [pac4j](https://github.com/pac4j/pac4j).

`pac4j` is a generic authentication engine which is implemented by many frameworks and supports multiple protocols: OAuth, CAS, OpenID (Connect), SAML, Google App Engine and HTTP (form and basic auth).

The `RatpackPac4j` class is the main component to use to manage the security of your web application.


## Prerequisite

All authentication mechanisms work by redirecting the user to an identity provider and finishing the login process when he is redirected back to the application with some dedicated credentials.
To make these authentication processes work and to save the originally requested url, the session is required. That's why the `SessionModule` is necessary:

```language-java
.registry(Guice.registry(b -> b
                  .module(SessionModule.class)
```


## Authentication mechanisms definition

Each authentication mechanism in `pac4j` is defined as a [Client](https://github.com/pac4j/pac4j/blob/master/pac4j-core/src/main/java/org/pac4j/core/client/Client.java).
Thus, configuring the ways to login into your application requires to create the right [clients](https://github.com/pac4j/pac4j/wiki/Clients): a `FacebookClient` to login with Facebook, a `TwitterClient` to login with Twitter, a `Google2Client` to login with Google (using OAuth v2.0 protocol), and so on.

Create your clients:

```language-java
final Saml2Client saml2Client = new Saml2Client();
saml2Client.setKeystorePath("resource:samlKeystore.jks");
saml2Client.setKeystorePassword("pac4j-demo-passwd");
saml2Client.setPrivateKeyPassword("pac4j-demo-passwd");
saml2Client.setIdpMetadataPath("resource:testshib-providers.xml");

final FacebookClient facebookClient = new FacebookClient("145278422258960", "be21409ba8f39b5dae2a7de525484da8")
final TwitterClient twitterClient = new TwitterClient("CoxUiYwQOSFDReZYdjigBA", "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");

final FormClient formClient = new FormClient("/theForm.html", new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator());
final BasicAuthClient basicAuthClient = new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator());

final CasClient casClient = new CasClient();
casClient.setCasLoginUrl("http://localhost:8888/cas/login");
```

And define them **BEFORE** any other authentication operation in your web chain using the `RatpackPac4j.authenticator(Client<?, ?>... clients)` method:

```language-java
chain
   .all(RatpackPac4j.authenticator(formClient, formClient, saml2Client, facebookClient, twitterClient, basicAuthClient, casClient))
```

The above command creates the `/authenticator` url endpoint to receive callbacks and finish the login processes (it can be set to an another `path` using `RatpackPac4j.authenticator(String path, Client<?, ?>... clients)`.


## Url protection

If you want to protect an url and request the user to be authenticated, you need to use the `Ratpack.requireAuth(Class<? extends Client<?, ?>> clientType)` method and provide the client required to perform the authentication.
After a successful authentication, the authenticated user profile is available in the *context*:

```language-java
.prefix("facebook", chain -> chain
                                .all(RatpackPac4j.requireAuth(FacebookClient.class))
                                .get(ctx -> ctx.render("User profile: " + ctx.get(FacebookProfile.class)))
       )
```

You can also use the `RatpackPac4j.login(Context ctx, Class<? extends Client<?, ?>> clientType)` method which doest not save the user profile in *context* but returns a promise of it (`Promise<UserProfile>`). 


## User profile retrieval

To get the user profile (or not) without initiating an authentication, you can just use the `RatpackPac4j.userProfile(Context ctx)` method:

```language-java
.get("profile", ctx -> {
                  RatpackPac4j.userProfile(ctx)
                    .route(Optional::isPresent, p -> ctx.render("User profile: " + p.get()))
                    .then(p -> ctx.render("not authenticated"));
                })
```

## Logout

The authenticated user profile is stored in session. To logout the current authenticated user, the `RatpakcPac4j.logout(Context ctx)` method must be used:

```language-java
.get("logout", ctx ->
                     RatpackPac4j.logout(ctx).then(() -> ctx.redirect("/"))
    )
```

## Demo

All capabilities available through this extension are demonstrated in the [ratpack-pac4j-demo](https://github.com/pac4j/ratpack-pac4j-demo).
