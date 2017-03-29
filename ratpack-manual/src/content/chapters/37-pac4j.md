# Security (pac4j)

The `ratpack-pac4j` extension provides authentication and authorization support via integration with [pac4j](http://www.pac4j.org).

The pac4j library is a security engine which abstracts over different authentication protocols such as OAuth, CAS, OpenID (Connect), SAML, Google App Engine and HTTP (form and basic auth) as well as custom authentication mechanisms (e.g. database backed).
It also supports various authorization mechanisms: roles / permissions checks, CSRF token, security headers, etc.

The [`RatpackPac4j`](api/ratpack/pac4j/RatpackPac4j.html) class provides the entirety of the integration.
This class provides static methods that provide handler implementations along with other finer grained constructs for use within your handler implementations.
The API reference for this class provides usage examples of each of the methods.

The `ratpack-pac4j` library requires the `ratpack-session` library, and use of the associated [`SessionModule`](api/ratpack/session/SessionModule.html).

## Usage

Each authentication mechanism in `pac4j` is defined as a “client”.
For example, pac4j provides the [FacebookClient](http://www.pac4j.org/apidocs/pac4j/@pac4j-version@/org/pac4j/oauth/client/FacebookClient.html) type that implements the Facebook authentication protocol.
Please see [pac4j's documentation on clients](http://www.pac4j.org/docs/clients.html) for more information.

The [`RatpackPac4j.authenticator(Client<?, ?>... clients)`](api/ratpack/pac4j/RatpackPac4j.html#authenticator-org.pac4j.core.client.Client...-) method provides a handler that defines the clients for an application.
It must be placed early in the handler chain as it makes the configured client instance(s) available to the downstream handlers that require auth operations. 

There are two ways to initiate auth:

- [`RatpackPac4j.requireAuth(Class<Client>, Authorizer...)`](api/ratpack/pac4j/RatpackPac4j.html#requireAuth-java.lang.Class-org.pac4j.core.authorization.authorizer.Authorizer...-)
    - a handler implementation that acts as a “filter” (both for authentication and authorizations)

- [`RatpackPac4j.login(Context, Class<Client>)`](api/ratpack/pac4j/RatpackPac4j.html#login-ratpack.handling.Context-java.lang.Class-)
    - method that initiates login if required (to be used within a handler implementation)

> Note that pac4j [provides many `Authorizer` implementations out of the box](http://www.pac4j.org/docs/authorizers.html).

These methods take a client type as an argument.
It is required that a client _instance_ of the given type was specified via the, upstream, [`RatpackPac4j.authenticator(Client<?, ?>... clients)`](api/ratpack/pac4j/RatpackPac4j.html#authenticator-org.pac4j.core.client.Client...-) handler.

The [`RatpackPac4j.userProfile()`](api/ratpack/pac4j/RatpackPac4j.html#userProfile-ratpack.handling.Context-) method can be used to obtain the user profile if the user is logged in, without requiring authentication.

## Session Usage

As previously mentioned, using `ratpack-pac4j` requires session support via `ratpack-session`.
When authenticated, the user's profile is stored in the session.
Therefore, terminating the session will effectively log the user out.

## Demo application

Please see the [ratpack-pac4j-demo](https://github.com/pac4j/ratpack-pac4j-demo) application for a complete application that demonstrates how to use pac4j with Ratpack. 
